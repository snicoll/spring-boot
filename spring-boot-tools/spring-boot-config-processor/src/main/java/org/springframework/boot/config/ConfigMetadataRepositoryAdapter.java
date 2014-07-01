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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link ConfigMetadataRepository} adapter implementation.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ConfigMetadataRepositoryAdapter implements ConfigMetadataRepository {

	protected final Map<String, ConfigMetadataGroup> groups;

	protected final Map<String, ConfigMetadataItem> items;

	private Map<String, ConfigMetadataGroup> cachedGroups;

	private Map<String, ConfigMetadataItem> cachedItems;

	public ConfigMetadataRepositoryAdapter(Map<String, ConfigMetadataGroup> groups,
			Map<String, ConfigMetadataItem> items) {
		this.groups = groups;
		this.items = items;
	}

	public ConfigMetadataRepositoryAdapter() {
		this(new HashMap<String, ConfigMetadataGroup>(), new HashMap<String, ConfigMetadataItem>());
	}

	@Override
	public Map<String, ConfigMetadataGroup> getAllGroups() {
		return new HashMap<String, ConfigMetadataGroup>(getCachedGroups());
	}

	public Map<String, ConfigMetadataItem> getAllItems() {
		return new HashMap<String, ConfigMetadataItem>(getCachedItems());
	}

	@Override
	public Map<String, ConfigMetadataGroup> getGroups() {
		return Collections.unmodifiableMap(this.groups);
	}

	@Override
	public Map<String, ConfigMetadataItem> getItems() {
		return Collections.unmodifiableMap(this.items);
	}

	public void clearCache() {
		cachedGroups = null;
		cachedItems = null;
	}


	protected Map<String, ConfigMetadataGroup> getCachedGroups() {
		if (cachedGroups == null) {
			cachedGroups = new HashMap<String, ConfigMetadataGroup>();
			for (ConfigMetadataGroup group : getGroups().values()) {
				cachedGroups.put(group.getId(), group);
			}
			for (ConfigMetadataGroup group : getGroups().values()) {
				cachedGroups.putAll(group.getAllGroups());
			}
		}
		return cachedGroups;
	}

	protected Map<String, ConfigMetadataItem> getCachedItems() {
		if (cachedItems == null) {
			cachedItems = new HashMap<String, ConfigMetadataItem>();
			for (ConfigMetadataItem item : getItems().values()) {
				cachedItems.put(item.getId(), item);
			}
			for (ConfigMetadataGroup group : getGroups().values()) {
				cachedItems.putAll(group.getAllItems());
			}
		}
		return cachedItems;
	}

}
