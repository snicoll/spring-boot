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

/**
 * Define a particular property
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class PropertyModel {

	private final String name;

	private final String type;

	private final String description;

	private final boolean nested;

	PropertyModel(String name, String type, String description, boolean nested) {
		this.name = name;
		this.type = type;
		this.description = description;
		this.nested = nested;
	}

	/**
	 * Return the name of the property.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the type of the property.
	 */
	public String getType() {
		return type;
	}

	/**
	 * Return the description of the property as defined by
	 * the javadoc.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Specify if the property defines a nested group.
	 */
	public boolean isNested() {
		return nested;
	}
}
