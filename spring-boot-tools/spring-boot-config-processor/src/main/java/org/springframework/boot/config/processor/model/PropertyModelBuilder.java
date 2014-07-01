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

package org.springframework.boot.config.processor.model;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.springframework.boot.config.processor.util.ModelHelper;
import org.springframework.boot.config.processor.util.ModelUtils;

/**
 * Build a {@link PropertyModel} based on its source code
 * definition.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class PropertyModelBuilder {

	private String name;

	private ExecutableElement getter;

	private ExecutableElement setter;

	private VariableElement field;

	/**
	 * Create a new builder for the specified property name
	 */
	public static PropertyModelBuilder forName(String name) {
		return new PropertyModelBuilder(name);
	}

	public PropertyModelBuilder(String name) {
		this.name = name;
	}

	/**
	 * Specify the method to read the value of the property (can be null).
	 */
	public PropertyModelBuilder withGetter(ExecutableElement getter) {
		this.getter = getter;
		return this;
	}

	/**
	 * Specify the method to write the value of the property (can be null).
	 */
	public PropertyModelBuilder withSetter(ExecutableElement setter) {
		this.setter = setter;
		return this;
	}

	/**
	 * Specify the field storing the value of the property (can be null).
	 */
	public PropertyModelBuilder withField(VariableElement field) {
		this.field = field;
		return this;
	}

	/**
	 * Build a {@link PropertyModel} based on its source code definition.
	 * <p>Detect the type and description based on available information.
	 */
	public PropertyModel build(ProcessingEnvironment env) {
		String type = detectType(env);
		String description = detectDescription(env);

		return new PropertyModel(name, type, description);
	}

	private String detectType(ProcessingEnvironment env) {
		if (setter != null) {
			return cleanType(env, setter.getParameters().get(0).asType());
		}
		else if (field != null) {
			return cleanType(env, field.asType());
		}
		else if (getter != null) {
			return cleanType(env, getter.getReturnType());
		}
		else {
			return null;
		}
	}

	private String detectDescription(ProcessingEnvironment env) {
		String fieldJavadoc = cleanJavadoc(env, field);
		if (fieldJavadoc != null) {
			return fieldJavadoc;
		}
		String getterJavadoc = cleanJavadoc(env, getter);
		if (getterJavadoc != null) {
			return getterJavadoc;
		}
		String setterJavadoc = cleanJavadoc(env, setter);
		if (setterJavadoc != null) {
			return setterJavadoc;
		}
		return null;
	}

	private String cleanType(ProcessingEnvironment env, TypeMirror typeMirror) {
		ModelHelper modelHelper = new ModelHelper(env);
		Class<?> wrapper = ModelUtils.getWrapper(typeMirror.getKind());
		if (wrapper != null) {
			return wrapper.getName();
		}
		if (modelHelper.isMapType(typeMirror)) {
			TypeMirror mapType = modelHelper.getMapType(typeMirror);
			return (mapType != null ? mapType.toString() : null);
		}
		if (modelHelper.isCollectionType(typeMirror)) {
			TypeMirror collectionType = modelHelper.getCollectionType(typeMirror);
			return (collectionType != null ? collectionType.toString() : null);
		}
		return typeMirror.toString();
	}

	private String cleanJavadoc(ProcessingEnvironment env, Element element) {
		String javadoc = getJavadoc(env, element);
		if (javadoc != null) {
			javadoc = javadoc.trim();
		}
		return javadoc;
	}

	private String getJavadoc(ProcessingEnvironment env, Element element) {
		if (element != null) {
			return env.getElementUtils().getDocComment(element);
		}
		return null;
	}

}
