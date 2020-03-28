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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;

import com.hazelcast.config.Config;
import com.hazelcast.core.ManagedContext;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HazelcastInstanceFactory}.
 *
 * @author Stephane Nicoll
 */
class HazelcastInstanceFactoryTests {

	private static final ClassPathResource CONFIG_LOCATION = new ClassPathResource(
			"org/springframework/boot/autoconfigure/hazelcast/hazelcast-specific.xml");

	@Test
	void createWithoutCustomizers() throws IOException {
		Config config = new HazelcastInstanceFactory(CONFIG_LOCATION).getConfig();
		assertThat(config.getMapConfigs()).containsOnlyKeys("foobar");
		assertThat(config.getManagedContext()).isNull();
	}

	@Test
	void createWithCustomizers() throws IOException {
		ManagedContext managedContext = mock(ManagedContext.class);
		HazelcastConfigCustomizer customizer = (config) -> {
			config.setManagedContext(managedContext);
		};
		Config config = new HazelcastInstanceFactory(CONFIG_LOCATION, customizer).getConfig();
		assertThat(config.getMapConfigs()).containsOnlyKeys("foobar");
		assertThat(config.getManagedContext()).isSameAs(managedContext);
	}

}
