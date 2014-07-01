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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link ConfigMetadataRepository} implementation that keeps its content
 * in memory.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class SimpleConfigMetadataRepository extends ConfigMetadataRepositoryAdapter {

	// Groups with no identifier at all
	private final List<ConfigMetadataGroup> noIdGroups = new ArrayList<ConfigMetadataGroup>();

	/**
	 * Register a root group. An id is expected to be set.
	 */
	public void registerRootGroup(ConfigMetadataGroup group) {
		if (group.getId() == null || group.getId().trim().equals("")) {
			this.noIdGroups.add(group);
		}
		else {
			ConfigMetadataGroup existingGroup = this.groups.get(group.getId());
			if (existingGroup == null) {
				this.groups.put(group.getId(), group);
			}
			else {
				existingGroup.merge(group);
			}
		}
		clearCache();
	}

	/**
	 * Include the content of the specified {@link ConfigMetadataRepository}. This basically
	 * merge the content of the specified {@link ConfigMetadataRepository} in this repository.
	 */
	public void include(ConfigMetadataRepository repository) {
		ConfigMetadataGroup noId = ConfigMetadataGroup.root(null);
		for (ConfigMetadataItem item : repository.getItems().values()) {
			noId.registerItem(item);
		}
		registerRootGroup(noId);

		for (ConfigMetadataGroup group : repository.getGroups().values()) {
			registerRootGroup(group);
		}
	}

	@Override
	public Map<String, ConfigMetadataItem> getItems() {
		Map<String, ConfigMetadataItem> items = new HashMap<String, ConfigMetadataItem>();
		for (ConfigMetadataGroup noIdGroup : noIdGroups) {
			items.putAll(noIdGroup.getAllItems());
		}
		return Collections.unmodifiableMap(items);
	}

	@Override
	protected Map<String, ConfigMetadataItem> getCachedItems() {
		Map<String, ConfigMetadataItem> items = super.getCachedItems();
		items.putAll(getItems());
		return items;
	}

}
