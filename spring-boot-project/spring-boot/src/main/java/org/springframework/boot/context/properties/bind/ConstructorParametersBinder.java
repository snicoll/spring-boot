/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.boot.context.properties.bind;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.ResolvableType;

/**
 * {@link BeanBinder} for constructor based binding.
 *
 * @author Madhura Bhave
 */
class ConstructorParametersBinder implements BeanBinder {

	@Override
	@SuppressWarnings("unchecked")
	public <T> T bind(ConfigurationPropertyName name, Bindable<T> target,
			Binder.Context context, BeanPropertyBinder propertyBinder) {
		try {
			Bean<T> bean = Bean.get(target);
			List<Object> bound = bind(propertyBinder, bean, context.getConverter());
			return (T) BeanUtils.instantiateClass(bean.getConstructor(), bound.toArray());
		}
		catch (InvalidConstructorCountException ex) {
			return null;
		}
	}

	private <T> List<Object> bind(BeanPropertyBinder propertyBinder, Bean<T> bean,
			BindConverter converter) {
		List<Object> boundParams = new ArrayList<>();
		for (ConstructorParameter parameter : bean.getParameters().values()) {
			Object bound = bind(propertyBinder, parameter);
			if (bound == null) {
				bound = getDefaultValue(converter, parameter, bound);
			}
			boundParams.add(bound);
		}
		return boundParams;
	}

	private Object getDefaultValue(BindConverter converter,
			ConstructorParameter parameter, Object bound) {
		if (parameter.getDefaultValue() != null) {
			bound = converter.convert(parameter.getDefaultValue(), parameter.getType(),
					parameter.getAnnotations());
		}
		else if (parameter.getType().resolve() != null
				&& parameter.getType().resolve().isPrimitive()) {
			bound = 0;
		}
		return bound;
	}

	private <T> Object bind(BeanPropertyBinder propertyBinder,
			ConstructorParameter parameter) {
		String propertyName = parameter.getName();
		ResolvableType type = parameter.getType();
		return propertyBinder.bindProperty(propertyName, Bindable.of(type));
	}

	private static class Bean<T> {

		private final Map<String, ConstructorParameter> parameters = new LinkedHashMap<>();

		private final Constructor<?> constructor;

		Bean(Constructor<?> constructor) {
			this.constructor = constructor;
			putParameters();
		}

		private void putParameters() {
			Parameter[] parameters = this.constructor.getParameters();
			for (Parameter parameter : parameters) {
				String name = parameter.getName();
				DefaultValue[] annotationsByType = parameter
						.getAnnotationsByType(DefaultValue.class);
				String defaultValue = (annotationsByType.length > 0)
						? annotationsByType[0].value() : null;
				this.parameters.computeIfAbsent(name,
						(s) -> new ConstructorParameter(name,
								ResolvableType.forClass(parameter.getType()),
								parameter.getDeclaredAnnotations(), defaultValue));
			}
		}

		@SuppressWarnings("unchecked")
		public static <T> Bean<T> get(Bindable<T> bindable) {
			Class<?> type = bindable.getType().resolve(Object.class);
			Constructor<?>[] constructors = type.getDeclaredConstructors();
			if (constructors.length != 1 || constructors[0].getParameterCount() == 0) {
				throw new InvalidConstructorCountException();
			}
			Bean<?> bean = new Bean<>(constructors[0]);
			return (Bean<T>) bean;
		}

		public Map<String, ConstructorParameter> getParameters() {
			return this.parameters;
		}

		public Constructor<?> getConstructor() {
			return this.constructor;
		}

	}

	private static class InvalidConstructorCountException extends RuntimeException {

	}

	/**
	 * A constructor parameter being bound.
	 */
	private static class ConstructorParameter {

		private final String name;

		private final ResolvableType type;

		private final Annotation[] annotations;

		private final String defaultValue;

		ConstructorParameter(String name, ResolvableType type, Annotation[] annotations,
				String defaultValue) {
			this.name = BeanPropertyName.toDashedForm(name);
			this.type = type;
			this.annotations = annotations;
			this.defaultValue = defaultValue;
		}

		public String getName() {
			return this.name;
		}

		public ResolvableType getType() {
			return this.type;
		}

		public Annotation[] getAnnotations() {
			return this.annotations;
		}

		public String getDefaultValue() {
			return this.defaultValue;
		}

	}

}
