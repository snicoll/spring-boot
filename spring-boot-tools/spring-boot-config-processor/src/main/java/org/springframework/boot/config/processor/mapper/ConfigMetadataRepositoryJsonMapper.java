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

package org.springframework.boot.config.processor.mapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import org.springframework.boot.config.ConfigMetadataGroup;
import org.springframework.boot.config.ConfigMetadataItem;
import org.springframework.boot.config.ConfigMetadataRepository;
import org.springframework.boot.config.SimpleConfigMetadataRepository;

/**
 * A {@link ConfigMetadataRepositoryMapper} that writes the repository as
 * a json document.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ConfigMetadataRepositoryJsonMapper implements ConfigMetadataRepositoryMapper {

	static final Charset UTF_8 = Charset.forName("UTF-8");

	public static final int BUFFER_SIZE = 4096;

	public static final String GROUPS = "groups";

	public static final String ITEMS = "items";

	public static final String TYPE_ATTRIBUTE = "type";

	public static final String DEFAULT_VALUE_ATTRIBUTE = "defaultValue";

	public static final String TYPES_ARRAY = "types";

	public static final String GROUP_TYPE_ATTRIBUTE = "groupType";

	public static final String GROUP_TYPES_ARRAY = "groupTypes";

	public static final String DESCRIPTION_ATTRIBUTE = "description";

	@Override
	public void writeRepository(ConfigMetadataRepository repo, OutputStream out) throws IOException {
		JSONObject json = new JSONObject();
		JSONObject groupsJSon = new JSONObject();
		for (ConfigMetadataGroup group : repo.getGroups().values()) {
			groupsJSon.put(group.getName(), writeGroup(group));
		}
		json.put(GROUPS, groupsJSon);

		Collection<ConfigMetadataItem> rootItems = repo.getItems().values();
		if (!rootItems.isEmpty()) {
			JSONObject itemsJson = new JSONObject();
			for (ConfigMetadataItem item : rootItems) {
				itemsJson.put(item.getName(), writeItem(null, item));
			}
			json.put(ITEMS, itemsJson);
		}

		write(json, out);
	}

	@Override
	public ConfigMetadataRepository readRepository(InputStream in) throws IOException {
		String content = read(in);
		JSONObject json = new JSONObject(content);

		SimpleConfigMetadataRepository repository = new SimpleConfigMetadataRepository();
		if (json.has(GROUPS)) {
			JSONObject rootGroups = json.getJSONObject(GROUPS);
			for (Object o : rootGroups.keySet()) {
				String groupName = (String) o;
				JSONObject rootGroup = rootGroups.getJSONObject(groupName);
				ConfigMetadataGroup group = ConfigMetadataGroup.root(groupName);
				readGroup(rootGroup, group);
				repository.registerRootGroup(group);
			}
		}
		if (json.has(ITEMS)) {
			ConfigMetadataGroup group = ConfigMetadataGroup.root("");
			JSONObject items = json.getJSONObject(ITEMS);
			for (Object o : items.keySet()) {
				String itemName = (String) o;
				JSONObject item = items.getJSONObject(itemName);
				group.registerItem(readItem(group, item, itemName));
			}
			repository.registerRootGroup(group);
		}

		return repository;
	}


	private JSONObject writeGroup(ConfigMetadataGroup group) {
		JSONObject json = new JSONObject();

		// types
		List<String> groupTypes = group.getTypes();
		if (!groupTypes.isEmpty()) {
			if (groupTypes.size() == 1) {
				json.put(TYPE_ATTRIBUTE, groupTypes.get(0));
			}
			else {
				JSONArray types = new JSONArray();
				for (String groupType : groupTypes) {
					types.put(groupType);
				}
				json.put(TYPES_ARRAY, types);
			}
		}

		// items
		JSONObject itemsJson = new JSONObject();
		for (ConfigMetadataItem item : group.getItems().values()) {
			itemsJson.put(item.getName(), writeItem(group, item));
		}
		if (itemsJson.length() > 0) {
			json.put(ITEMS, itemsJson);
		}

		// nested groups
		JSONObject groupsJson = new JSONObject();
		for (ConfigMetadataGroup childGroup : group.getGroups().values()) {
			groupsJson.put(childGroup.getName(), writeGroup(childGroup));
		}
		if (groupsJson.length() > 0) {
			json.put(GROUPS, groupsJson);
		}
		return json;
	}

	private JSONObject writeItem(ConfigMetadataGroup group, ConfigMetadataItem item) {
		JSONObject json = new JSONObject();

		// group types
		List<String> groupTypes = item.getGroupTypes();
		if (!groupTypes.isEmpty()) {
			if (groupTypes.size() == 1) {
				json.put(GROUP_TYPE_ATTRIBUTE, resolveGroupTypeIndex(group, groupTypes.get(0)));
			}
			else {
				JSONArray types = new JSONArray();
				for (String groupType : groupTypes) {
					int index = resolveGroupTypeIndex(group, groupType);
					types.put(index);
				}
				json.put(GROUP_TYPES_ARRAY, types);
			}
		}
		if (item.getValueType() != null) {
			json.put(TYPE_ATTRIBUTE, item.getValueType());
		}
		if (item.getDefaultValue() != null) {
			json.put(DEFAULT_VALUE_ATTRIBUTE, item.getDefaultValue());
		}
		if (item.getDescription() != null) {
			json.put(DESCRIPTION_ATTRIBUTE, item.getDescription());
		}
		return json;
	}

	private void readGroup(JSONObject json, ConfigMetadataGroup group) {
		if (json.has(TYPES_ARRAY)) {
			JSONArray types = json.getJSONArray(TYPES_ARRAY);
			for (int i = 0; i < types.length(); i++) {
				String type = types.getString(i);
				group.addType(type);
			}
		}
		else if (json.has(TYPE_ATTRIBUTE)) {
			group.addType(json.getString(TYPE_ATTRIBUTE));
		}
		// items
		if (json.has(ITEMS)) {
			JSONObject items = json.getJSONObject(ITEMS);
			for (Object o : items.keySet()) {
				String itemName = (String) o;
				JSONObject item = items.getJSONObject(itemName);
				group.registerItem(readItem(group, item, itemName));
			}
		}

		// groups
		if (json.has(GROUPS)) {
			JSONObject groups = json.getJSONObject(GROUPS);
			for (Object o : groups.keySet()) {
				String groupName = (String) o;
				JSONObject childGroup = groups.getJSONObject(groupName);
				ConfigMetadataGroup child = group.registerGroup(groupName);
				readGroup(childGroup, child);
			}
		}
	}

	private ConfigMetadataItem readItem(ConfigMetadataGroup owner, JSONObject json, String name) {
		ConfigMetadataItem item = new ConfigMetadataItem(name);

		// Group types resolution
		if (json.has(GROUP_TYPES_ARRAY)) {
			JSONArray types = json.getJSONArray(GROUP_TYPES_ARRAY);
			for (int i = 0; i < types.length(); i++) {
				doAddGroupType(owner, item, types.getInt(i));
			}
		}
		else if (json.has(GROUP_TYPE_ATTRIBUTE)) {
			int index = json.getInt(GROUP_TYPE_ATTRIBUTE);
			doAddGroupType(owner, item, index);
		}
		if (json.has(TYPE_ATTRIBUTE)) {
			item.setValueType(json.getString(TYPE_ATTRIBUTE));
		}
		if (json.has(DEFAULT_VALUE_ATTRIBUTE)) {
			item.setDefaultValue(json.getString(DEFAULT_VALUE_ATTRIBUTE));
		}
		if (json.has(DESCRIPTION_ATTRIBUTE)) {
			item.setDescription(json.getString(DESCRIPTION_ATTRIBUTE));
		}
		return item;
	}

	private void write(JSONObject json, OutputStream out) throws IOException {
		String content = json.toString(2);
		out.write(content.getBytes(UTF_8));
	}

	private String read(InputStream in) throws IOException {
		StringBuilder out = new StringBuilder();
		InputStreamReader reader = new InputStreamReader(in, UTF_8);
		char[] buffer = new char[BUFFER_SIZE];
		int bytesRead = -1;
		while ((bytesRead = reader.read(buffer)) != -1) {
			out.append(buffer, 0, bytesRead);
		}
		return out.toString();
	}

	private void doAddGroupType(ConfigMetadataGroup group, ConfigMetadataItem item, int index) {
		String groupType = resolveGroupType(group, index);
		if (groupType != null) {
			item.addGroupType(groupType);
		}
	}

	/**
	 * Return the group type index of the specified {@code groupType} or {@code -1} if
	 * it cannot be found.
	 */
	private int resolveGroupTypeIndex(ConfigMetadataGroup group, String groupType) {
		if (group == null) {
			return -1;
		}
		return group.getTypes().indexOf(groupType);
	}

	/**
	 * Return the group at the specified {@code index} or {@code null} if it cannot
	 * be located.
	 */
	private String resolveGroupType(ConfigMetadataGroup group, int index) {
		List<String> types = group.getTypes();
		if (index < 0 || index >= types.size()) {
			return null;
		}
		return types.get(index);
	}

}
