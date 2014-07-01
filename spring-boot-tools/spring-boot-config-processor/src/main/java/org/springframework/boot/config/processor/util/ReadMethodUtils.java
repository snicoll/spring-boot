/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.config.processor.util;

import java.util.ArrayList;
import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

/**
 * Getter-related utilities.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public abstract class ReadMethodUtils {

	/**
	 * The default prefix for a getter method.
	 * @see #READ_BOOLEAN_PREFIX
	 */
	public static final String READ_PREFIX = "get";

	/**
	 * The prefix for a getter method returning a boolean.
	 */
	public static final String READ_BOOLEAN_PREFIX = "is";

	/**
	 * Check if the {@link ExecutableElement} is a valid getter (i.e. mostly
	 * that it has no argument and its return type is {@link TypeKind#VOID}.
	 */
	public static boolean isGetter(ExecutableElement method) {
		Assert.notNull(method, "Method must not be null");

		final String name = ModelUtils.getName(method);
		return isGetterMethodName(name) && method.getParameters().isEmpty()
				&& (TypeKind.VOID != method.getReturnType().getKind());
	}

	/**
	 * Return the property name from the specified getter name. For
	 * {@code getFoo} this returns {@code foo}.
	 */
	public static String toPropertyName(String methodName) {
		if (methodName.startsWith(READ_PREFIX)) {
			return ModelUtils.uncapitalize(methodName.substring(READ_PREFIX.length()));
		}
		else if (methodName.startsWith(READ_BOOLEAN_PREFIX)) {
			return ModelUtils.uncapitalize(methodName.substring(READ_BOOLEAN_PREFIX.length()));
		}
		else {
			return null;
		}
	}

	/**
	 * Return the candidate method names for the specified property.
	 */
	public static List<String> toMethodNames(String propertyName) {
		Assert.notNull(propertyName, "Property name must not be null.");
		final List<String> result = new ArrayList<String>();
		result.add(ModelUtils.toMethodName(READ_PREFIX, propertyName));
		result.add(ModelUtils.toMethodName(READ_BOOLEAN_PREFIX, propertyName));
		return result;
	}

	private static boolean isGetterMethodName(String name) {
		return name.startsWith(READ_PREFIX) || name.startsWith(READ_BOOLEAN_PREFIX);
	}
}

