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
 * Base assert helper for {@link ConfigMetadataRepository}.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ConfigMetadataRepositoryAssert<T extends ConfigMetadataRepository, A extends ConfigMetadataRepositoryAssert> {

	protected final T actual;

	public ConfigMetadataRepositoryAssert(T actual) {
		this.actual = actual;
	}

	/**
	 * Return a {@link ConfigMetadataItemAssert} for the item with the specified id.
	 */
	public ConfigMetadataItemAssert item(String id) {
		return ConfigMetadataTestUtils.assertItem(this.actual, id);
	}

	/**
	 * Return a {@link ConfigMetadataItemAssert} for the first level item with the specified name.
	 */
	public ConfigMetadataItemAssert localItem(String name) {
		ConfigMetadataItem item = this.actual.getItems().get(name);
		assertNotNull("No local item with name '" + name + "' for '" + this.actual + "'", item);
		return ConfigMetadataTestUtils.assertItem(item);
	}

	/**
	 * Return a {@link ConfigMetadataGroupAssert} for the group with the specified id.
	 */
	public ConfigMetadataGroupAssert group(String id) {
		return ConfigMetadataTestUtils.assertGroup(this.actual, id);
	}

	/**
	 * Return a {@link ConfigMetadataGroupAssert} for the first level group with the specified name.
	 */
	public ConfigMetadataGroupAssert localGroup(String name) {
		ConfigMetadataGroup group = this.actual.getGroups().get(name);
		assertNotNull("No local group with name '" + name + "' for '" + this.actual + "'", group);
		return ConfigMetadataTestUtils.assertGroup(group);
	}

	/**
	 * Assert that all items exist. Additional items may exist.
	 */
	public A itemsExist(String... itemIds) {
		for (String itemId : itemIds) {
			assertNotNull("Item with id '" + itemId + "' not found in repository",
					actual.getAllItems().get(itemId));
		}
		return self();
	}

	/**
	 * Assert that all first level items exist. Additional items may exist.
	 */
	public A localItemsExist(String... itemNames) {
		for (String itemName : itemNames) {
			assertNotNull("Local item with name '" + itemName + "' not found in repository",
					actual.getItems().get(itemName));
		}
		return self();
	}

	/**
	 * Assert that none of the specified item exists.
	 */
	public A itemsDoNotExist(String... itemIds) {
		for (String itemId : itemIds) {
			assertNull("No item with id '" + itemId + "' was expected", actual.getAllItems().get(itemId));
		}
		return self();
	}

	/**
	 * Assert the size of the repository.
	 */
	public A size(int rootGroups, int allItems) {
		assertEquals("Wrong number of root group", rootGroups, actual.getGroups().size());
		assertEquals("Wrong number of total items", allItems, actual.getAllItems().size());
		return self();
	}

	@SuppressWarnings("unchecked")
	protected A self() {
		return (A) this;
	}
}
