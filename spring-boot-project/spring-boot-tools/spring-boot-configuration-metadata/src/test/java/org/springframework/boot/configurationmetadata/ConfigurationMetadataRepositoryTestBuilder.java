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

package org.springframework.boot.configurationmetadata;

import java.util.Collections;

import org.springframework.boot.configurationmetadata.Deprecation.Level;

/**
 * @author Stephane Nicoll
 */
public class ConfigurationMetadataRepositoryTestBuilder {

	private final SimpleConfigurationMetadataRepository repository = new SimpleConfigurationMetadataRepository();

	public ConfigurationMetadataRepositoryTestBuilder withGroup(String id) {
		ConfigurationMetadataSource source = new ConfigurationMetadataSource();
		source.setGroupId(id);
		this.repository.add(Collections.singletonList(source));
		return this;
	}

	public ConfigurationMetadataRepositoryTestBuilder withProperty(String id) {
		ConfigurationMetadataProperty property = new ConfigurationMetadataProperty();
		property.setId(id);
		return registerProperty(property);
	}

	public ConfigurationMetadataRepositoryTestBuilder withDeprecatedProperty(String id) {
		return withDeprecatedProperty(id, Level.WARNING);
	}

	public ConfigurationMetadataRepositoryTestBuilder withDeprecatedProperty(String id, Level level) {
		ConfigurationMetadataProperty property = new ConfigurationMetadataProperty();
		property.setId(id);
		Deprecation deprecation = new Deprecation();
		if (level != null) {
			deprecation.setLevel(level);
		}
		property.setDeprecation(deprecation);
		return registerProperty(property);
	}

	public ConfigurationMetadataRepository build() {
		return this.repository;
	}

	private ConfigurationMetadataRepositoryTestBuilder registerProperty(ConfigurationMetadataProperty property) {
		this.repository.add(property, null);
		return this;
	}

}
