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

import java.util.HashMap;
import java.util.Map;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

/**
 * Define an entity that holds some configuration properties.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class EntityModel {

	private final TypeElement typeElement;

	private final DeclaredType declaredType;

	private String prefix;

	private final Map<String, PropertyModel> properties = new HashMap<String, PropertyModel>();

	private final Map<String, EntityModel> nestedEntities = new HashMap<String, EntityModel>();

	/**
	 * Create a new instance.
	 * @param typeElement the class definition
	 */
	public EntityModel(TypeElement typeElement) {
		this.typeElement = typeElement;
		this.declaredType = (DeclaredType) typeElement.asType();
	}

	/**
	 * Return the class definition
	 */
	public TypeElement getTypeElement() {
		return typeElement;
	}

	/**
	 * Return the type (i.e. similar to a Class in the runtime model).
	 */
	public DeclaredType getDeclaredType() {
		return declaredType;
	}

	/**
	 * Return the prefix to be used for the elements defined in this entity
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * Set the prefix to use.
	 */
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * Return the properties defined on this entity.
	 * @see PropertyModel
	 */
	public Map<String, PropertyModel> getProperties() {
		return properties;
	}

	/**
	 * Return the nested entities defined on this entity.
	 */
	public Map<String, EntityModel> getNestedEntities() {
		return nestedEntities;
	}

	/**
	 * Specify if this model is empty, that is does not hold any property or
	 * nested model.
	 */
	public boolean isEmpty() {
		return this.properties.isEmpty() && this.nestedEntities.isEmpty();
	}

	/**
	 * Add a new property.
	 */
	void addProperty(PropertyModel property) {
		this.properties.put(property.getName(), property);
	}

	/**
	 * Add a nested entity.
	 */
	void addNestedEntity(EntityModel entityModel) {
		this.nestedEntities.put(entityModel.getPrefix(), entityModel);
	}
}
