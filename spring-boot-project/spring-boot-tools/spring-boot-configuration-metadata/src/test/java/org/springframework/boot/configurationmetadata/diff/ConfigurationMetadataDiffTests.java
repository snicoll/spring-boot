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

import org.assertj.core.api.AssertProvider;
import org.junit.jupiter.api.Test;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryTestBuilder;
import org.springframework.boot.configurationmetadata.diff.test.ConfigurationMetadataPropertyDiffAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationMetadataDiff}.
 *
 * @author Stephane Nicoll
 */
class ConfigurationMetadataDiffTests {

	@Test
	void diffWithIdenticalProperties() {
		ConfigurationMetadataRepository repository = new ConfigurationMetadataRepositoryTestBuilder()
				.withProperty("acme.one").withDeprecatedProperty("acme.two").build();
		ConfigurationMetadataDiff diff = diff(repository, repository);
		assertThat(diff.properties().map(ConfigurationMetadataDiffEntry::getId)).containsOnly("acme.one", "acme.two");
		ConfigurationMetadataProperty acmeOne = repository.getAllProperties().get("acme.one");
		assertThat(property(diff.getPropertyDiff("acme.one"))).isSameAs(acmeOne, acmeOne);
		ConfigurationMetadataProperty acmeTwo = repository.getAllProperties().get("acme.two");
		assertThat(property(diff.getPropertyDiff("acme.two"))).isSameAs(acmeTwo, acmeTwo);
	}

	@Test
	void diffWithSeparateProperties() {
		ConfigurationMetadataRepository leftRepository = new ConfigurationMetadataRepositoryTestBuilder()
				.withProperty("acme.one").build();
		ConfigurationMetadataRepository rightRepository = new ConfigurationMetadataRepositoryTestBuilder()
				.withProperty("acme.two").build();
		ConfigurationMetadataDiff diff = diff(leftRepository, rightRepository);
		assertThat(diff.properties().map(ConfigurationMetadataDiffEntry::getId)).containsOnly("acme.one", "acme.two");
		assertThat(property(diff.getPropertyDiff("acme.one")))
				.isRemoved(leftRepository.getAllProperties().get("acme.one"));
		assertThat(property(diff.getPropertyDiff("acme.two")))
				.isAdded(rightRepository.getAllProperties().get("acme.two"));
	}

	private static ConfigurationMetadataDiff diff(ConfigurationMetadataRepository left,
			ConfigurationMetadataRepository right) {
		return new ConfigurationMetadataDiffFactory().diff(left, right);
	}

	private static AssertProvider<ConfigurationMetadataPropertyDiffAssert> property(
			ConfigurationMetadataDiffEntry<ConfigurationMetadataProperty> diff) {
		return () -> new ConfigurationMetadataPropertyDiffAssert(diff);
	}

}
