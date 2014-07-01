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

import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.springframework.boot.config.processor.util.ModelUtils;

/**
 * Process the model of a class to generate an {@link EntityModel}
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class EntityModelFactory {

	private final ProcessingEnvironment env;

	public EntityModelFactory(ProcessingEnvironment env) {
		this.env = env;
	}

	/**
	 * Create a fully initialized {@link EntityModel} based on the
	 * specified {@link TypeElement}.
	 */
	public EntityModel create(TypeElement element) {
		return create(element, element);
	}

	/**
	 * Create a fully initialized {@link EntityModel} based on the
	 * specified {@link TypeElement} detected by the specified
	 * {@code annotationSource}.
	 * @param typeElement the element to harvest
	 * @param annotationSource the element holding the @ConfigurationProperties annotation
	 */
	public EntityModel create(TypeElement typeElement, Element annotationSource) {
		EntityModel model = new EntityModel(typeElement);

		Map<String, Object> parameters = getConfigurationParameters(annotationSource);
		model.setPrefix(getPrefix(parameters));

		PropertyResolver propertyResolver = new PropertyResolver(env, model);
		propertyResolver.resolve();

		return model;
	}

	private String getPrefix(Map<String, Object> parameters) {
		String prefix = (String) parameters.get("prefix");
		if (ModelUtils.hasText(prefix)) {
			return prefix;
		}
		return (String) parameters.get("value");
	}

	/**
	 * Return the attributes of the {@code ConfigurationProperties} annotation. If
	 * the attribute is not defined, it is not present at all even if that attribute
	 * has a default value.
	 */
	private Map<String, Object> getConfigurationParameters(Element typeElement) {
		AnnotationMirror annotation = ModelUtils.getConfigurationPropertiesAnnotation(typeElement);
		if (annotation != null) {
			return ModelUtils.parseAnnotationProperties(annotation);
		}
		throw new IllegalStateException("No @ConfigurationProperties found on '" + typeElement + "'");
	}

}
