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

import java.util.Map;

/**
 * Container of {@link ConfigMetadataItem} in a hierarchical structure. Provides
 * access to the first level items and groups. Each group provides access to
 * the lower level, if any.
 *
 * <p>First level items can be accessed using their names. For instance, if a
 * group named "app.tech" has a sub-group called "network", the group can be
 * retrieved on that instance using {@link #getGroups()} with the "network"
 * key. It is also possible to retrieve it anywhere in the hierarchy using
 * {@link #getAllGroups()} with the "app.tech.network" key. The same principle
 * goes for items.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public interface ConfigMetadataRepository {

	/**
	 * Return all groups contained within this repository. Groups
	 * are indexed by id.
	 * @see ConfigMetadataGroup#getId()
	 */
	Map<String, ConfigMetadataGroup> getAllGroups();

	/**
	 * Return all items contained within this repository. Items
	 * are indexed by id.
	 * @see ConfigMetadataItem#getId()
	 */
	Map<String, ConfigMetadataItem> getAllItems();

	/**
	 * Return the root (i.e. first level) groups contained withing this
	 * repository. Groups are indexed by name.
	 * @see ConfigMetadataGroup#getName()
	 */
	Map<String, ConfigMetadataGroup> getGroups();

	/**
	 * Return the root (i.e. first level) items contained withing this
	 * repository. Items re indexed by name.
	 * @see ConfigMetadataItem#getName()
	 */
	Map<String, ConfigMetadataItem> getItems();

}
