/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.endpoint.web;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A resolver for the arguments to be passed to an operation on a web endpoint. Supports
 * {@link String} and {@link Enum} arguments.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class WebEndpointOperationArgumentResolver {

	// TODO Use a ConversionService for argument conversion?

	/**
	 * Resolves the arguments from the given {@code argumentSource} for an invocation of
	 * the given {@code operationMethod}.
	 *
	 * @param operationMethod the method
	 * @param argumentSource the source of possible arguments
	 * @return the resolved arguments
	 */
	public Object[] resolveArguments(Method operationMethod,
			Function<String, Object> argumentSource) {
		Parameter[] parameters = operationMethod.getParameters();
		@SuppressWarnings("unchecked")
		List<Object> arguments = Stream.of(parameters).map(parameter -> {
			Object value = argumentSource.apply(parameter.getName());
			if (value != null && Enum.class.isAssignableFrom(parameter.getType())) {
				value = convert((Class<Enum<?>>) parameter.getType(), value.toString());
			}
			return value;
		}).collect(Collectors.toList());
		return arguments.toArray(new Object[arguments.size()]);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T extends Enum> T convert(Class<T> enumType, String input) {
		return (T) Enum.valueOf(enumType, input.toUpperCase());
	}

}
