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

package org.springframework.boot.configurationmetadata.diff.test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.diff.ConfigurationMetadataDiffEntry;

/**
 * Assertions on {@link ConfigurationMetadataGroup}.
 *
 * @author Stephane Nicoll
 */
public class ConfigurationMetadataGroupDiffAssert
		extends ConfigurationMetadataDiffEntryAssert<ConfigurationMetadataGroupDiffAssert, ConfigurationMetadataGroup> {

	public ConfigurationMetadataGroupDiffAssert(ConfigurationMetadataDiffEntry<ConfigurationMetadataGroup> actual) {
		super(actual, ConfigurationMetadataGroupDiffAssert.class);
	}

}
