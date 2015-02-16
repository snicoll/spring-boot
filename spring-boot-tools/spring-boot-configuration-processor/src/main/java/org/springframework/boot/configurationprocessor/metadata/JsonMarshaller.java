/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.configurationprocessor.metadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Marshaller to write {@link ConfigurationMetadata} as JSON.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 */
public class JsonMarshaller {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final int BUFFER_SIZE = 4098;

	public void write(ConfigurationMetadata metadata, OutputStream outputStream)
			throws IOException {
		JSONObject object = new JSONObject();
		object.put("groups", toGroupArray(metadata.getGroups()));
		object.put("properties", toPropertyArray(metadata.getProperties()));
		outputStream.write(object.toString(2).getBytes(UTF_8));
	}

	private JSONArray toGroupArray(List<GroupMetadata> groups) {
		JSONArray jsonArray = new JSONArray();
		for (GroupMetadata group : groups) {
			jsonArray.put(toJsonObject(group));
		}
		return jsonArray;
	}

	private JSONArray toPropertyArray(List<PropertyMetadata> properties) {
		JSONArray jsonArray = new JSONArray();
		for (PropertyMetadata property : properties) {
			jsonArray.put(toJsonProperty(property));
		}
		return jsonArray;
	}

	private JSONObject toJsonProperty(PropertyMetadata metadata) {
		JSONObject object = toJsonObject(metadata);
		TypeDescriptor typeDescriptor = metadata.getTypeDescriptor();
		if (typeDescriptor != null &&
				(typeDescriptor.getKeyType() != null || typeDescriptor.getValueTypeDescriptor() != null)) {
			object.put("domain", toJsonTypeDescriptor(typeDescriptor, false));
		}
		return object;
	}


	private JSONObject toJsonObject(ItemMetadata item) {
		JSONObject jsonObject = new JSONOrderedObject();
		jsonObject.put("name", item.getName());
		putIfPresent(jsonObject, "type", item.getType());
		putIfPresent(jsonObject, "description", item.getDescription());
		putIfPresent(jsonObject, "sourceType", item.getSourceType());
		putIfPresent(jsonObject, "sourceMethod", item.getSourceMethod());
		Object defaultValue = item.getDefaultValue();
		if (defaultValue != null) {
			putDefaultValue(jsonObject, defaultValue);
		}
		if (item.isDeprecated()) {
			jsonObject.put("deprecated", true);
		}
		return jsonObject;
	}

	private JSONObject toJsonTypeDescriptor(TypeDescriptor typeDescriptor, boolean includeType) {
		JSONObject jsonObject = new JSONObject();
		if (includeType) {
			jsonObject.put("type", typeDescriptor.getType());
		}
		if (typeDescriptor.getKeyType() != null) {
			JSONObject keyType = new JSONObject();
			keyType.put("type", typeDescriptor.getKeyType());
			jsonObject.put("key", keyType);
		}
		if (typeDescriptor.getValueTypeDescriptor() != null) {
			jsonObject.put("value", toJsonTypeDescriptor(typeDescriptor.getValueTypeDescriptor(), true));
		}
		return jsonObject;
	}

	private void putIfPresent(JSONObject jsonObject, String name, Object value) {
		if (value != null) {
			jsonObject.put(name, value);
		}
	}

	private void putDefaultValue(JSONObject jsonObject, Object value) {
		Object defaultValue = value;
		if (value.getClass().isArray()) {
			JSONArray array = new JSONArray();
			int length = Array.getLength(value);
			for (int i = 0; i < length; i++) {
				array.put(Array.get(value, i));
			}
			defaultValue = array;

		}
		jsonObject.put("defaultValue", defaultValue);
	}

	public ConfigurationMetadata read(InputStream inputStream) throws IOException {
		ConfigurationMetadata metadata = new ConfigurationMetadata();
		JSONObject object = new JSONObject(toString(inputStream));
		JSONArray groups = object.optJSONArray("groups");
		if (groups != null) {
			for (int i = 0; i < groups.length(); i++) {
				metadata.add(toGroupMetadata((JSONObject) groups.get(i)));
			}
		}
		JSONArray properties = object.optJSONArray("properties");
		if (properties != null) {
			for (int i = 0; i < properties.length(); i++) {
				metadata.add(toPropertyMetadata((JSONObject) properties.get(i)));
			}
		}
		return metadata;
	}

	private GroupMetadata toGroupMetadata(JSONObject object) {
		String name = object.getString("name");
		String type = object.optString("type", null);
		String description = object.optString("description", null);
		String sourceType = object.optString("sourceType", null);
		String sourceMethod = object.optString("sourceMethod", null);
		Object defaultValue = readDefaultValue(object);
		boolean deprecated = object.optBoolean("deprecated");
		return new GroupMetadata(name, null, sourceType, sourceMethod,
				description, defaultValue, deprecated, type);
	}

	private PropertyMetadata toPropertyMetadata(JSONObject object) {
		String name = object.getString("name");
		String type = object.optString("type", null);
		String description = object.optString("description", null);
		String sourceType = object.optString("sourceType", null);
		String sourceMethod = object.optString("sourceMethod", null);
		Object defaultValue = readDefaultValue(object);
		boolean deprecated = object.optBoolean("deprecated");
		TypeDescriptor typeDescriptor = toTypeDescriptor(object, type);
		return new PropertyMetadata(name, null, typeDescriptor, sourceType, sourceMethod,
				description, defaultValue, deprecated);
	}

	private TypeDescriptor toTypeDescriptor(JSONObject object, String type) {
		JSONObject domain = object.optJSONObject("domain");
		if (domain != null) {
			JSONObject keyJson = domain.optJSONObject("key");
			String keyType = (keyJson != null ? keyJson.optString("type", null) : null);
			JSONObject valueJson = domain.optJSONObject("value");
			TypeDescriptor valueTypeDescriptor =
					(valueJson != null ? toTypeDescriptor(valueJson, valueJson.getString("type")) : null);
			return new TypeDescriptor(type, valueTypeDescriptor, keyType);
		}
		else {
			return new TypeDescriptor(type);

		}
	}

	private Object readDefaultValue(JSONObject object) {
		Object defaultValue = object.opt("defaultValue");
		if (defaultValue instanceof JSONArray) {
			JSONArray array = (JSONArray) defaultValue;
			Object[] content = new Object[array.length()];
			for (int i = 0; i < array.length(); i++) {
				content[i] = array.get(i);
			}
			return content;
		}
		return defaultValue;
	}

	private String toString(InputStream inputStream) throws IOException {
		StringBuilder out = new StringBuilder();
		InputStreamReader reader = new InputStreamReader(inputStream, UTF_8);
		char[] buffer = new char[BUFFER_SIZE];
		int bytesRead;
		while ((bytesRead = reader.read(buffer)) != -1) {
			out.append(buffer, 0, bytesRead);
		}
		return out.toString();
	}

	/**
	 * Extension to {@link JSONObject} that remembers the order of inserts.
	 */
	@SuppressWarnings("rawtypes")
	private static class JSONOrderedObject extends JSONObject {

		private Set<String> keys = new LinkedHashSet<String>();

		@Override
		public JSONObject put(String key, Object value) throws JSONException {
			this.keys.add(key);
			return super.put(key, value);
		}

		@Override
		public Set keySet() {
			return this.keys;
		}

	}

}
