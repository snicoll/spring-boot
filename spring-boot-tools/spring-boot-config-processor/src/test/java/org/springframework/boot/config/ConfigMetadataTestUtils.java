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
 * Base test utilities for config metadata.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public abstract class ConfigMetadataTestUtils {

	public static ConfigMetadataRepositoryAssert<?, ?> assertRepo(ConfigMetadataRepository repository) {
		return new ConfigMetadataRepositoryAssert<ConfigMetadataRepository, ConfigMetadataRepositoryAssert>(repository);
	}

	public static ConfigMetadataItemAssert assertItem(ConfigMetadataItem item) {
		return new ConfigMetadataItemAssert(item);
	}

	public static ConfigMetadataItemAssert assertItem(ConfigMetadataRepository repository, String id) {
		ConfigMetadataItem item = repository.getAllItems().get(id);
		assertNotNull("No item found with id '" + id + "'", item);
		return assertItem(item);
	}

	public static ConfigMetadataGroupAssert assertGroup(ConfigMetadataGroup group) {
		return new ConfigMetadataGroupAssert(group);
	}

	public static ConfigMetadataGroupAssert assertGroup(ConfigMetadataRepository repository, String id) {
		ConfigMetadataGroup group = repository.getAllGroups().get(id);
		assertNotNull("No group found with id '" + id + "'", group);
		return assertGroup(group);
	}

	public static ConfigMetadataGroup createGroup(String id, String... itemNames) {
		ConfigMetadataGroup group = ConfigMetadataGroup.root(id);
		registerItems(group, itemNames);
		return group;
	}

	public static void registerItems(ConfigMetadataGroup group, String... itemNames) {
		for (String itemName : itemNames) {
			group.registerItem(createItem(itemName, null, null));
		}
	}

	public static ConfigMetadataItem createItem(String name, Class<?> type, String description, String... groupTypes) {
		ConfigMetadataItem item = new ConfigMetadataItem(name);
		item.setId(name);
		if (type != null) {
			item.setValueType(type.getName());
		}
		if (description != null) {
			item.setDescription(description);
		}
		for (String groupType : groupTypes) {
			item.addGroupType(groupType);
		}
		return item;
	}

}
