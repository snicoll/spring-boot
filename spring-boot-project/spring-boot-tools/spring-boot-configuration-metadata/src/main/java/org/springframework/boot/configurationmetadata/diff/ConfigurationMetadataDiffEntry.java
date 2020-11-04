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

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;

/**
 * A diff entry for either a {@link ConfigurationMetadataGroup group} or a
 * {@link ConfigurationMetadataProperty property}.
 *
 * @param <T> the type of the entry
 * @author Stephane Nicoll
 * @since 2.6.0
 * @see ConfigurationMetadataGroup
 * @see ConfigurationMetadataProperty
 */
public final class ConfigurationMetadataDiffEntry<T> {

	private final String id;

	private final T left;

	private final T right;

	ConfigurationMetadataDiffEntry(String id, T left, T right) {
		this.id = id;
		this.left = left;
		this.right = right;
	}

	/**
	 * Return the id of the entry.
	 * @return the entry Id
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Return the state of the entry on the left side or {@code null} if the entry does
	 * not exist.
	 * @return the state on the left side or {@code null}
	 */
	public T getLeft() {
		return this.left;
	}

	/**
	 * Return the state of the entry on the right side or {@code null} if the entry does
	 * not exist.
	 * @return the state on the right side or {@code null}
	 */
	public T getRight() {
		return this.right;
	}

}
