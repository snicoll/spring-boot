/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.context.assertj;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AssertableApplicationContextParameterResolver}.
 *
 * @author Stephane Nicoll
 */
@SpringBootTest
class AssertableApplicationContextParameterResolverIntegrationTests {

	@Autowired
	private ApplicationContext sourceApplicationContext;

	@Test
	void parameterIsResolved(AssertableApplicationContext context) {
		assertThat(context).hasBean("first")
			.hasBean("second")
			.getBeanNames(String.class)
			.containsOnly("first", "second");
		assertThat(context.getSourceApplicationContext()).isEqualTo(this.sourceApplicationContext);
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		static String first() {
			return "one";
		}

		@Bean
		static String second() {
			return "two";
		}

	}

}
