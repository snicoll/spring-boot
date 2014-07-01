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

package org.springframework.boot.config;

import static org.junit.Assert.*;

/**
 * Assert helper for {@link ConfigMetadataItem}.
 * @author Stephane Nicoll
 */
public class ConfigMetadataItemAssert {

	private final ConfigMetadataItem actual;

	public ConfigMetadataItemAssert(ConfigMetadataItem actual) {
		this.actual = actual;
	}

	/**
	 * Assert then name of the item.
	 */
	public ConfigMetadataItemAssert name(String name) {
		assertEquals("Wrong name for '" + actual + "'", name, actual.getName());
		return this;
	}

	/**
	 * Assert the id of the item.
	 */
	public ConfigMetadataItemAssert id(String id) {
		assertEquals("Wrong id for '" + actual + "'", id, actual.getId());
		return this;
	}

	/**
	 * Assert that this item is not linked to any group type.
	 */
	public ConfigMetadataItemAssert noGroupType() {
		return groupTypes(new String[0]);
	}

	/**
	 * Assert that the specified group types are defined on this item
	 * and only those.
	 * @see #noGroupType()
	 */
	public ConfigMetadataItemAssert groupTypes(String... groupTypes) {
		for (String groupType : groupTypes) {
			assertTrue("Group type '" + groupType + "' is not registered for '" + actual + "'",
					actual.getGroupTypes().contains(groupType));
		}
		assertEquals("Wrong number of group types", groupTypes.length, actual.getGroupTypes().size());
		return this;
	}

	/**
	 * Assert that the specified group types are defined on this item
	 * and only those.
	 * @see #noGroupType()
	 */
	public ConfigMetadataItemAssert groupTypes(Class<?>... groupTypes) {
		String[] groupTypesString = new String[groupTypes.length];
		for (int i = 0; i < groupTypes.length; i++) {
			groupTypesString[i] = groupTypes[i].getName();
		}
		return groupTypes(groupTypesString);
	}

	/**
	 * Assert that no value type is defined on this item.
	 */
	public ConfigMetadataItemAssert noValueType() {
		return valueType((String) null);
	}

	/**
	 * Assert the type of the value for the item.
	 */
	public ConfigMetadataItemAssert valueType(String valueType) {
		assertEquals("Wrong value type for '" + actual + "'", valueType, actual.getValueType());
		return this;
	}

	/**
	 * Assert the type of the value for the item.
	 */
	public ConfigMetadataItemAssert valueType(Class<?> valueType) {
		return valueType(valueType.getName());
	}

	/**
	 * Assert that no description is defined on this item.
	 */
	public ConfigMetadataItemAssert noDescription() {
		return description(null);
	}

	/**
	 * Assert the description for the item.
	 * @see #noDescription()
	 */
	public ConfigMetadataItemAssert description(String description) {
		assertEquals("Wrong description for '" + actual + "'", description, actual.getDescription());
		return this;
	}
}
