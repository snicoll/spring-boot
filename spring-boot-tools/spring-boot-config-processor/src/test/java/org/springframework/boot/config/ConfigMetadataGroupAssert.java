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
 * Assert helper for {@link ConfigMetadataGroup}.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ConfigMetadataGroupAssert
		extends ConfigMetadataRepositoryAssert<ConfigMetadataGroup, ConfigMetadataGroupAssert> {

	public ConfigMetadataGroupAssert(ConfigMetadataGroup actual) {
		super(actual);
	}

	/**
	 * Assert the name of the group.
	 */
	public ConfigMetadataGroupAssert name(String name) {
		assertEquals("Wrong name for '" + actual + "'", name, actual.getName());
		return this;
	}

	/**
	 * Assert the id of the group.
	 */
	public ConfigMetadataGroupAssert id(String id) {
		assertEquals("Wrong id for '" + actual + "'", id, actual.getId());
		return this;
	}

	/**
	 * Assert that no type is defined on this group.
	 */
	public ConfigMetadataGroupAssert noType() {
		return types(new String[0]);
	}

	/**
	 * Assert that the specified types are defined on this group
	 * and only those.
	 * @see #noType()
	 */
	public ConfigMetadataGroupAssert types(String... types) {
		for (String type : types) {
			assertTrue("Type '" + type + "' is not registered for '" + actual + "'",
					actual.getTypes().contains(type));
		}
		assertEquals("Wrong number of types", types.length, actual.getTypes().size());
		return this;
	}

	/**
	 * Assert that the specified types are defined on this group
	 * and only those.
	 * @see #noType()
	 */
	public ConfigMetadataGroupAssert types(Class<?>... types) {
		String[]  typeStrings = new String[types.length];
		for (int i = 0; i < types.length; i++) {
			typeStrings[i] = types[i].getName();
		}
		return types(typeStrings);
	}

	/**
	 * Assert the number of local item(s), that is the first level items of this group.
	 */
	public ConfigMetadataGroupAssert localItems(int localItems) {
		assertEquals("Wrong number of local items for '" + actual + "'", localItems, actual.getItems().size());
		return this;
	}

	/**
	 * Assert the number of local group(s), that is the first level groups of this group.
	 */
	public ConfigMetadataGroupAssert localGroups(int localGroups) {
		assertEquals("Wrong number of local groups for '" + actual + "'", localGroups, actual.getGroups().size());
		return this;
	}

}
