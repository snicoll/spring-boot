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

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;

/**
 * Setter-related utilities
 * @author Stephane Nicoll
 */
public abstract class WriteMethodUtils {

	/**
	 * The default prefix for a setter method.
	 */
	public static final String WRITE_PREFIX = "set";

	/**
	 * Check if the {@link ExecutableElement} is a valid setter (i.e. mostly
	 * that it takes a single parameter and the return type is {@link TypeKind#VOID}).
	 */
	public static boolean isSetter(ExecutableElement method) {
		Assert.notNull(method, "Method must not be null");
		final String name = method.getSimpleName().toString();
		return isSetterMethodName(name) && method.getParameters().size() == 1
				&& (TypeKind.VOID == method.getReturnType().getKind());
	}

	/**
	 * Return the property name from the specified setter name. For
	 * {@code setFoo} this returns {@code foo}.
	 */
	public static String toPropertyName(String methodName) {
		if (methodName.startsWith(WRITE_PREFIX)) {
			return ModelUtils.uncapitalize(methodName.substring(WRITE_PREFIX.length()));
		}
		else {
			return null;
		}
	}

	private static boolean isSetterMethodName(String name) {
		return name.startsWith(WRITE_PREFIX);
	}

}
