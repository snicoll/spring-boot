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

import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryTestBuilder;
import org.springframework.boot.configurationmetadata.Deprecation.Level;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DiffPredicates}.
 *
 * @author Stephane Nicoll
 */
class DiffPredicatesTests {

	@Test
	void propertiesAddedMatchAddedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two").build(),
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.three").withProperty("acme.one")
						.withDeprecatedProperty("acme.four").withDeprecatedProperty("acme.two").build());
		assertThat(diff.properties().filter(DiffPredicates.ADDED).map(ConfigurationMetadataDiffEntry::getId))
				.containsOnly("acme.three", "acme.four");
	}

	@Test
	void propertiesAvailableOnBothSideDoesNotMatchAddedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two").build(),
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one").build());
		assertThat(diff.properties().filter(DiffPredicates.ADDED)).isEmpty();
	}

	@Test
	void groupsAddedMatchAddedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withGroup("acme").build(),
				new ConfigurationMetadataRepositoryTestBuilder().withGroup("acme").withGroup("example").build());
		assertThat(diff.groups().filter(DiffPredicates.ADDED).map(ConfigurationMetadataDiffEntry::getId))
				.containsOnly("example");
	}

	@Test
	void propertiesRemovedMatchRemovedPredicate() {
		ConfigurationMetadataDiff diff = diff(new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
				.withProperty("acme.two").build(),
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one").build());
		assertThat(diff.properties().filter(DiffPredicates.REMOVED).map(ConfigurationMetadataDiffEntry::getId))
				.containsOnly("acme.two");
	}

	@Test
	void propertiesDeprecatedWithErrorLevelMatchRemovedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one").withProperty("acme.two")
						.build(),
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two", Level.ERROR).build());
		assertThat(diff.properties().filter(DiffPredicates.REMOVED).map(ConfigurationMetadataDiffEntry::getId))
				.containsOnly("acme.two");
	}

	@Test
	void propertiesDeprecatedAndThenRemovedMatchRemovedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two").build(),
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one").build());
		assertThat(diff.properties().filter(DiffPredicates.REMOVED).map(ConfigurationMetadataDiffEntry::getId))
				.containsOnly("acme.two");
	}

	@Test
	void propertiesDeprecatedWithErrorLevelOnBothSideDoesNotMatchRemovedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two", Level.ERROR).build(),
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two", Level.ERROR).build());
		assertThat(diff.properties().filter(DiffPredicates.REMOVED)).isEmpty();
	}

	@Test
	void propertiesAvailableOnBothSideDoesNotMatchRemovedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two").build(),
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two").build());
		assertThat(diff.properties().filter(DiffPredicates.REMOVED)).isEmpty();
	}

	@Test
	void propertiesAddedDoesNotMachRemovedPredicate() {
		ConfigurationMetadataDiff diff = diff(new ConfigurationMetadataRepositoryTestBuilder().build(),
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two").build());
		assertThat(diff.properties().filter(DiffPredicates.REMOVED)).isEmpty();
	}

	@Test
	void groupsRemovedMatchRemovedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withGroup("acme").withGroup("example").build(),
				new ConfigurationMetadataRepositoryTestBuilder().withGroup("acme").build());
		assertThat(diff.groups().filter(DiffPredicates.REMOVED).map(ConfigurationMetadataDiffEntry::getId))
				.containsOnly("example");
	}

	@Test
	void groupsAvailableOnBothSideDoesNotMatchRemovedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withGroup("acme").withGroup("example").build(),
				new ConfigurationMetadataRepositoryTestBuilder().withGroup("acme").withGroup("example").build());
		assertThat(diff.groups().filter(DiffPredicates.REMOVED)).isEmpty();
	}

	@Test
	void propertiesDeprecatedMatchDeprecatedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one").withProperty("acme.two")
						.build(),
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two").build());
		assertThat(diff.properties().filter(DiffPredicates.DEPRECATED).map(ConfigurationMetadataDiffEntry::getId))
				.containsOnly("acme.two");
	}

	@Test
	void propertiesDeprecatedOnBothSideDoesNotMatchDeprecatedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two").build(),
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two").build());
		assertThat(diff.properties().filter(DiffPredicates.DEPRECATED)).isEmpty();
	}

	@Test
	void propertiesDeprecatedWithErrorLevelDoesNotMatchDeprecatedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one").withProperty("acme.two")
						.build(),
				new ConfigurationMetadataRepositoryTestBuilder().withProperty("acme.one")
						.withDeprecatedProperty("acme.two", Level.ERROR).build());
		assertThat(diff.properties().filter(DiffPredicates.DEPRECATED)).isEmpty();
	}

	@Test
	void groupsDoNotMatchDeprecatedPredicate() {
		ConfigurationMetadataDiff diff = diff(
				new ConfigurationMetadataRepositoryTestBuilder().withGroup("acme").withGroup("example").build(),
				new ConfigurationMetadataRepositoryTestBuilder().withGroup("acme").withGroup("another").build());
		assertThat(diff.groups().filter(DiffPredicates.DEPRECATED)).isEmpty();
	}

	private static ConfigurationMetadataDiff diff(ConfigurationMetadataRepository left,
			ConfigurationMetadataRepository right) {
		return new ConfigurationMetadataDiffFactory().diff(left, right);
	}

}
