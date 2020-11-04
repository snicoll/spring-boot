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

import java.util.stream.Stream;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;

/**
 * Provide a diff for the {@link ConfigurationMetadataGroup groups} and the
 * {@link ConfigurationMetadataProperty properties} between two
 * {@link ConfigurationMetadataRepository repositories}.
 *
 * @author Stephane Nicoll
 * @since 2.6.0
 */
public interface ConfigurationMetadataDiff {

	/**
	 * Return a {@link Stream} of group diff.
	 * @return a stream of group diff
	 */
	Stream<ConfigurationMetadataDiffEntry<ConfigurationMetadataGroup>> groups();

	/**
	 * Return a {@link Stream} of property diff.
	 * @return a stream of property diff
	 */
	Stream<ConfigurationMetadataDiffEntry<ConfigurationMetadataProperty>> properties();

	/**
	 * Return the diff for the group identified with the given {@code groupId} or
	 * {@code null} if no such group exists.
	 * @param groupId the id of the group
	 * @return the group diff or {@code null}
	 */
	ConfigurationMetadataDiffEntry<ConfigurationMetadataGroup> getGroupDiff(String groupId);

	/**
	 * Return the diff for the property identified with the given {@code propertyId} or
	 * {@code null} if no such property exists.
	 * @param propertyId the id of the property
	 * @return the property diff or {@code null}
	 */
	ConfigurationMetadataDiffEntry<ConfigurationMetadataProperty> getPropertyDiff(String propertyId);

}
