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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represent a group of configuration items. These items are usually defined within a
 * class with a given prefix. Each item in the group share a common prefix that is the
 * id of the group.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ConfigMetadataGroup extends ConfigMetadataRepositoryAdapter {

	private final String name;

	private String id;

	private final List<String> types;

	public ConfigMetadataGroup(String name) {
		this.name = name;
		this.types = new ArrayList<String>();
	}

	/**
	 * Create a root group (i.e. a group having no parent). If the
	 * {@code name} is {@code null}, this refers to a group where each
	 * item is held in the root of the hierarchy.
	 * @param name the name of the group (serves as id)
	 */
	public static ConfigMetadataGroup root(String name) {
		ConfigMetadataGroup group = new ConfigMetadataGroup(name);
		group.setId(name);
		return group;
	}

	/**
	 * Return the name of the group.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the identifier of the group.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the identifier of the group.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Return the classes in which the direct items of this group are defined or
	 * an empty list if such information is not available.
	 */
	public List<String> getTypes() {
		return Collections.unmodifiableList(types);
	}

	/**
	 * Add a class in which some of the direct items of this group are defined.
	 */
	public void addType(String type) {
		if (type != null && !this.types.contains(type)) {
			this.types.add(type);
		}
	}

	/**
	 * Register a new item. The identifier is automatically assigned
	 * as part of the registration process. All group types that this
	 * item defines should be associated to this group.
	 */
	public void registerItem(ConfigMetadataItem item) {
		// sanity check
		for (String groupType : item.getGroupTypes()) {
			if (!this.types.contains(groupType)) {
				throw new IllegalArgumentException("Could not register '"
						+ item + "': group type '" + groupType + "' is unknown.");
			}
		}
		String name = item.getName();
		if (!this.items.containsKey(name)) {
			item.setId(generateId(getId(), name));
			this.items.put(name, item);
			clearCache();
		}
	}

	/**
	 * Register a child item. The identifier is automatically assigned
	 * as part of the registration process.
	 */
	public ConfigMetadataGroup registerGroup(String name) {
		ConfigMetadataGroup group = new ConfigMetadataGroup(name);
		group.setId(generateId(getId(), group.getName()));
		this.groups.put(name, group);
		clearCache();
		return group;
	}

	/**
	 * Merge the specified group with this group. Improve this group with missing
	 * information that may be defined by the specified {@link ConfigMetadataGroup}.
	 * <p>Recursively merge nested groups and items.
	 * <p>throws a {@link IllegalArgumentException} if the id of the group parameter
	 * does not match the id of this group.
	 */
	public void merge(ConfigMetadataGroup group) {
		if (!this.id.equals(group.getId())) {
			throw new IllegalArgumentException("Could not merge " + group + " in " + this + " as they" +
					"have a different ids " + this.id + " -- " + group.getId());
		}
		for (String groupType : group.getTypes()) {
			addType(groupType);
		}

		for (ConfigMetadataItem item : group.getItems().values()) {
			ConfigMetadataItem existingItem = this.items.get(item.getName());
			if (existingItem != null) {
				existingItem.merge(item);
			} else {
				this.items.put(item.getName(), item);
			}
		}
		for (ConfigMetadataGroup childGroup : group.getGroups().values()) {
			ConfigMetadataGroup existingGroup = this.groups.get(childGroup.getName());
			if (existingGroup != null) {
				existingGroup.merge(childGroup);
			}
			else {
				this.groups.put(childGroup.getName(), childGroup);
			}
		}
	}

	private static String generateId(String parentId, String name) {
		return generatePrefix(parentId) + name;
	}

	private static String generatePrefix(String id) {
		if (id == null || id.trim().isEmpty()) {
			return "";
		}
		else {
			return id + ".";
		}
	}

}
