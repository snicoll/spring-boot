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

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/**
 * Various model-related utilities
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public abstract class ModelUtils {

	/**
	 * The fully qualified name of the {@code ConfigurationProperties} annotation.
	 */
	public static final String CONFIGURATION_PROPERTIES_FQN
			= "org.springframework.boot.context.properties.ConfigurationProperties";

	private static final Map<TypeKind, Class<?>> primitivesToWrappers;

	static {
		Map<TypeKind, Class<?>> map = new HashMap<TypeKind, Class<?>>();
		map.put(TypeKind.BOOLEAN, Boolean.class);
		map.put(TypeKind.BYTE, Byte.class);
		map.put(TypeKind.CHAR, Character.class);
		map.put(TypeKind.DOUBLE, Double.class);
		map.put(TypeKind.FLOAT, Float.class);
		map.put(TypeKind.INT, Integer.class);
		map.put(TypeKind.LONG, Long.class);
		map.put(TypeKind.SHORT, Short.class);
		primitivesToWrappers = map;
	}

	/**
	 * Return the name of the element.
	 */
	public static String getName(Element element) {
		return element.getSimpleName().toString();
	}

	/**
	 * Return the type exposed by a property based on its getter or setter method.
	 * @param method the method to use to get the type
	 * @param setter {@code true} if the method is the write method, {@code false} if the method
	 * is the read method
	 */
	public static TypeMirror getPropertyType(ExecutableElement method, boolean setter) {
		if (setter) {
			return method.getParameters().get(0).asType();
		}
		else {
			return method.getReturnType();
		}
	}

	/**
	 * Specify if the given {@link Element} is annotated with {@code @ConfigurationProperties}.
	 */
	public static boolean isConfigurationPropertiesAnnotated(Element element) {
		return getConfigurationPropertiesAnnotation(element) != null;
	}

	/**
	 * Return the {@link AnnotationMirror} corresponding to the {@code @ConfigurationProperties}
	 * defined on the specified {@link Element} or {@code null} if that element does not
	 * define the annotation
	 */
	public static AnnotationMirror getConfigurationPropertiesAnnotation(Element element) {
		for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
			if (isAnnotation(ModelUtils.CONFIGURATION_PROPERTIES_FQN, annotation)) {
				return annotation;
			}
		}
		return null;
	}

	/**
	 * Return the attributes of the specified annotation. Note that if the annotation
	 * defines a default for a property, the property is not defined at all in the
	 * result map if it's absent from this specific annotation.
	 * <p>Array are represented as a {@code List<Object>}.
	 */
	public static Map<String, Object> parseAnnotationProperties(AnnotationMirror annotation) {
		Map<String, Object> parameters = new HashMap<String, Object>();
		for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
				: annotation.getElementValues().entrySet()) {
			String name = ModelUtils.getName(entry.getKey());
			Object value = entry.getValue().getValue();
			parameters.put(name, value);
		}
		return parameters;
	}

	/**
	 * Return the wrapper type for the specified {@code TypeKind} or {@code null} if
	 * the specified type is not a primitive type.
	 * @see TypeKind#isPrimitive()
	 */
	public static Class<?> getWrapper(TypeKind typeKind) {
		return primitivesToWrappers.get(typeKind);
	}

	/**
	 * Capitalize the first character of a String.
	 */
	public static String capitalize(String str) {
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}

	/**
	 * Make sure the first character of the specified {@link String} is
	 * lower case.
	 */
	public static String uncapitalize(String str) {
		return Character.toLowerCase(str.charAt(0)) + str.substring(1);
	}

	/**
	 * Specify if the value is non null and non empty.
	 */
	public static boolean hasText(String value) {
		return value != null && !value.trim().isEmpty();
	}

	/**
	 * Create a getter or setter method name from a property.
	 *
	 * @param prefix a prefix (get/is or set)
	 * @param propertyName the name of a property
	 * @return a getter or setter method name
	 */
	public static String toMethodName(String prefix, String propertyName) {
		return prefix + capitalize(propertyName);
	}

	private static boolean isAnnotation(String fqn, AnnotationMirror annotation) {
		return annotation.getAnnotationType().toString().equals(fqn);
	}

}
