/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.Map;
import java.util.stream.Stream;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

/**
 * A simple {@link ConfigurationMetadataDiff} implementation.
 *
 * @author Stephane Nicoll
 */
class SimpleConfigurationMetadataDiff implements ConfigurationMetadataDiff {

	private final Map<String, ConfigurationMetadataDiffEntry<ConfigurationMetadataGroup>> groups;

	private final Map<String, ConfigurationMetadataDiffEntry<ConfigurationMetadataProperty>> properties;

	SimpleConfigurationMetadataDiff(Map<String, ConfigurationMetadataDiffEntry<ConfigurationMetadataGroup>> groups,
			Map<String, ConfigurationMetadataDiffEntry<ConfigurationMetadataProperty>> properties) {
		this.groups = groups;
		this.properties = properties;
	}

	@Override
	public Stream<ConfigurationMetadataDiffEntry<ConfigurationMetadataGroup>> groups() {
		return this.groups.values().stream();
	}

	@Override
	public Stream<ConfigurationMetadataDiffEntry<ConfigurationMetadataProperty>> properties() {
		return this.properties.values().stream();
	}

	@Override
	public ConfigurationMetadataDiffEntry<ConfigurationMetadataGroup> getGroupDiff(String groupId) {
		return this.groups.get(groupId);
	}

	@Override
	public ConfigurationMetadataDiffEntry<ConfigurationMetadataProperty> getPropertyDiff(String propertyId) {
		return this.properties.get(propertyId);
	}

}
