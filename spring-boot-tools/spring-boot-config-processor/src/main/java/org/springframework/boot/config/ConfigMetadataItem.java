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

import org.springframework.boot.config.processor.util.Assert;

/**
 * Define a particular configuration property. Such property has
 * an id which is the key used to configure that property.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ConfigMetadataItem {

	private final String name;

	private String id;

	private final List<String> groupTypes;

	private String valueType;

	private String description;

	private String tagLine;

	public ConfigMetadataItem(String name) {
		this.name = name;
		this.groupTypes = new ArrayList<String>();
	}

	/**
	 * Return the name of the property
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the identifier of the property which is used to configure
	 * that item externally (i.e. in configuration files).
	 */
	public String getId() {
		return id;
	}

	void setId(String id) {
		this.id = id;
	}

	/**
	 * Return the group type(s) that are associated with this item. Return
	 * an empty list if this item isn't associated to an explicit type.
	 * <p>The returned list is read-only, use {@link #addGroupType(String)}
	 * to register an additional group type.
	 * @see ConfigMetadataGroup#getTypes()
	 */
	public List<String> getGroupTypes() {
		return Collections.unmodifiableList(groupTypes);
	}

	/**
	 * Associate an additional group type to the specified item. Doing so
	 * means that this item is "compatible" with the specified group type.
	 */
	public void addGroupType(String groupType)  {
		Assert.notNull(groupType, "GroupType must not be null");
		if (!this.groupTypes.contains(groupType)) {
			this.groupTypes.add(groupType);
		}
	}

	/**
	 * Return the type of the value.
	 */
	public String getValueType() {
		return valueType;
	}

	public void setValueType(String valueType) {
		this.valueType = valueType;
	}

	/**
	 * Return the description of property.
	 */
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
		this.tagLine = buildTagLine(description);
	}

	/**
	 * Return a tag line for the property that is suitable for short
	 * summary. Only contains a sentence.
	 */
	public String getTagLine() {
		return tagLine;
	}

	/**
	 * Merge the specified item with this item. Improve this item with missing
	 * information that may be defined by the specified {@link ConfigMetadataItem}.
	 * <p>throws a {@link IllegalArgumentException} if the id of the item parameter
	 * does not match the id of this item.
	 */
	public void merge(ConfigMetadataItem item) {
		if (!this.id.equals(item.getId())) {
			throw new IllegalArgumentException("Could not merge " + item + " in " + this + " as they" +
					"have a different ids " + this.id + " -- " + item.getId());
		}
		for (String groupType : item.getGroupTypes()) {
			addGroupType(groupType);
		}

		if (getValueType() == null && item.getValueType() != null) {
			setValueType(item.getValueType());
		}
		if (getDescription() == null && item.getDescription() != null) {
			setDescription(item.getDescription());
		}
	}

	private String buildTagLine(String description) {
		if (description == null) {
			return null;
		}
		int dotIndex = description.indexOf('.');
		int newlineIndex = description.indexOf('\n');
		final int i;
		if (dotIndex != -1 && newlineIndex != -1) {
			i = dotIndex < newlineIndex ? dotIndex : newlineIndex;
		} else {
			i = dotIndex != -1 ? dotIndex : newlineIndex;
		}
		String result = (i != -1 ? description.substring(0, i) : description);
		return result.trim();
	}

}
