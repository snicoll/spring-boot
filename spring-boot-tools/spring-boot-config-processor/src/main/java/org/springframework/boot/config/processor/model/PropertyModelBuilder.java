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

import java.util.Collections;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
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

	private EntityModel owner;

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

	public PropertyModelBuilder withOwner(EntityModel owner) {
		this.owner = owner;
		return this;
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
	public PropertyModel build(ModelHelper modelHelper) {
		TypeMirror type = detectType();
		String cleanType = cleanType(modelHelper, type);
		String description = detectDescription(modelHelper);

		// Check @ConfigurationItem annotation
		Map<String, Object> annotationAttributes = detectConfigurationItemAnnotation();
		boolean nestedValue = getNestedValue(annotationAttributes, isNestedEntity(modelHelper, type));

		return new PropertyModel(name, cleanType, description, nestedValue);
	}

	/**
	 * Return the attributes of the {@code ConfigurationItem} annotation. If
	 * the attribute is not defined, it is not present at all even if that attribute
	 * has a default value.
	 */
	private Map<String, Object> detectConfigurationItemAnnotation() {
		AnnotationMirror fieldAnnotation = detectConfigurationItemAnnotation(this.field);
		if (fieldAnnotation != null) {
			return ModelUtils.parseAnnotationProperties(fieldAnnotation);
		}
		AnnotationMirror getterAnnotation = detectConfigurationItemAnnotation(this.getter);
		if (getterAnnotation != null) {
			return ModelUtils.parseAnnotationProperties(getterAnnotation);
		}
		AnnotationMirror setterAnnotation = detectConfigurationItemAnnotation(this.setter);
		if (setterAnnotation != null) {
			return ModelUtils.parseAnnotationProperties(setterAnnotation);
		}
		return Collections.emptyMap();
	}

	private AnnotationMirror detectConfigurationItemAnnotation(Element element) {
		return (element != null ? ModelUtils.getConfigurationItemAnnotation(element) : null);
	}

	private String detectDescription(ModelHelper modelHelper) {
		String fieldJavadoc = cleanJavadoc(modelHelper, field);
		if (fieldJavadoc != null) {
			return fieldJavadoc;
		}
		String getterJavadoc = cleanJavadoc(modelHelper, getter);
		if (getterJavadoc != null) {
			return getterJavadoc;
		}
		String setterJavadoc = cleanJavadoc(modelHelper, setter);
		if (setterJavadoc != null) {
			return setterJavadoc;
		}
		return null;
	}

	private TypeMirror detectType() {
		if (setter != null) {
			return setter.getParameters().get(0).asType();
		}
		else if (field != null) {
			return field.asType();
		}
		else if (getter != null) {
			return getter.getReturnType();
		}
		throw new IllegalStateException("Could not detect type");
	}

	private String cleanType(ModelHelper modelHelper, TypeMirror typeMirror) {
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

	private String cleanJavadoc(ModelHelper modelHelper, Element element) {
		String javadoc = modelHelper.getJavadoc(element);
		if (javadoc != null) {
			javadoc = javadoc.trim();
		}
		return javadoc;
	}

	private boolean getNestedValue(Map<String, Object> annotationAttributes, boolean defaultValue) {
		Boolean attributeValue = (Boolean) annotationAttributes.get("nested");
		return (attributeValue != null ? attributeValue : defaultValue);
	}

	private boolean isNestedEntity(ModelHelper modelHelper, TypeMirror propertyType) {
		return modelHelper.isConcreteClass(propertyType)
				&& modelHelper.isNestedType(propertyType, owner.getTypeElement());
	}

}
