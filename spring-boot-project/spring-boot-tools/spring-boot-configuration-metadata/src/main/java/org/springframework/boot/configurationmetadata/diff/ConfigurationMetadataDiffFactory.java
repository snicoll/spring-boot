/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.configurationmetadata.diff;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;

/**
 * Create a {@link ConfigurationMetadataDiff} based on two
 * {@link ConfigurationMetadataRepository repositories}.
 *
 * @author Stephane Nicoll
 * @since 2.6.0
 */
public class ConfigurationMetadataDiffFactory {

	/**
	 * Create a diff of the specified {@link ConfigurationMetadataRepository
	 * repositories}.
	 * @param left the repository corresponding to the left side of the diff
	 * @param right the repository corresponding to the right side of the diff
	 * @return the diff between the specified repositories
	 */
	public ConfigurationMetadataDiff diff(ConfigurationMetadataRepository left, ConfigurationMetadataRepository right) {
		return new SimpleConfigurationMetadataDiff(diffGroups(left, right), diffProperties(left, right));
	}

	private Map<String, ConfigurationMetadataDiffEntry<ConfigurationMetadataGroup>> diffGroups(
			ConfigurationMetadataRepository left, ConfigurationMetadataRepository right) {
		Map<String, ConfigurationMetadataDiffEntry<ConfigurationMetadataGroup>> diff = new HashMap<>();
		Map<String, ConfigurationMetadataGroup> leftGroups = left.getAllGroups();
		Map<String, ConfigurationMetadataGroup> rightGroups = right.getAllGroups();
		for (ConfigurationMetadataGroup leftGroup : leftGroups.values()) {
			String id = leftGroup.getId();
			diff.put(id, new ConfigurationMetadataDiffEntry<>(id, leftGroup, rightGroups.get(id)));
		}
		for (ConfigurationMetadataGroup rightGroup : rightGroups.values()) {
			String id = rightGroup.getId();
			if (!diff.containsKey(id)) {
				diff.put(id, new ConfigurationMetadataDiffEntry<>(id, null, rightGroup));
			}
		}
		return diff;
	}

	private Map<String, ConfigurationMetadataDiffEntry<ConfigurationMetadataProperty>> diffProperties(
			ConfigurationMetadataRepository left, ConfigurationMetadataRepository right) {
		Map<String, ConfigurationMetadataDiffEntry<ConfigurationMetadataProperty>> diff = new HashMap<>();
		Map<String, ConfigurationMetadataProperty> leftProperties = left.getAllProperties();
		Map<String, ConfigurationMetadataProperty> rightProperties = right.getAllProperties();
		for (ConfigurationMetadataProperty leftProperty : leftProperties.values()) {
			String id = leftProperty.getId();
			diff.put(id, new ConfigurationMetadataDiffEntry<>(id, leftProperty, rightProperties.get(id)));
		}
		for (ConfigurationMetadataProperty rightProperty : rightProperties.values()) {
			String id = rightProperty.getId();
			if (!diff.containsKey(id)) {
				diff.put(id, new ConfigurationMetadataDiffEntry<>(id, null, rightProperty));
			}
		}
		return diff;
	}

}
