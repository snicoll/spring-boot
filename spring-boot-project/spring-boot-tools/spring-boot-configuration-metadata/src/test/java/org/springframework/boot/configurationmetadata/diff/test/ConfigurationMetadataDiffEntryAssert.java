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

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.ObjectAssert;

import org.springframework.boot.configurationmetadata.diff.ConfigurationMetadataDiffEntry;

/**
 * Base class for assertions on {@link ConfigurationMetadataDiffEntry}.
 *
 * @author Stephane Nicoll
 */
public abstract class ConfigurationMetadataDiffEntryAssert<SELF extends ConfigurationMetadataDiffEntryAssert<SELF, TYPE>, TYPE>
		extends AbstractObjectAssert<SELF, ConfigurationMetadataDiffEntry<TYPE>> {

	ConfigurationMetadataDiffEntryAssert(ConfigurationMetadataDiffEntry<TYPE> actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isAdded() {
		return isAdded(null);
	}

	public SELF isAdded(TYPE added) {
		left().isNull();
		if (added != null) {
			right().isSameAs(added);
		}
		else {
			right().isNotNull();
		}
		return this.myself;
	}

	public SELF isRemoved() {
		return isRemoved(null);
	}

	public SELF isRemoved(TYPE removed) {
		if (removed != null) {
			left().isSameAs(removed);
		}
		else {
			left().isNotNull();
		}
		right().isNull();
		return this.myself;
	}

	public SELF isSameAs(TYPE left, TYPE right) {
		left().isSameAs(left);
		right().isSameAs(right);
		return this.myself;
	}

	private ObjectAssert<TYPE> left() {
		return new ObjectAssert<>(this.actual.getLeft()).describedAs("Left entry with id '%s'", this.actual.getId());
	}

	private ObjectAssert<TYPE> right() {
		return new ObjectAssert<>(this.actual.getRight()).describedAs("Right entry with id '%s'", this.actual.getId());
	}

}
