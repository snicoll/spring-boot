/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.rule;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringContext}.
 *
 * @author Stephane Nicoll
 */
public class SpringContextTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final SpringContext context = new SpringContext();

	@After
	public void closeContext() {
		this.context.closeAll();
	}

	@Test
	public void getBeanWithNoContext() {
		this.thrown.expectMessage(
				"make sure the test is calling one of the 'load' methods");
		this.context.getBean("foo");
	}

	@Test
	public void configurationIsProcessedInOrder() {
		this.context.config(ConfigA.class, AutoConfigA.class).load();
		assertThat(this.context.getBean("a")).isEqualTo("autoconfig-a");
	}

	@Test
	public void useConfigurationIsProcessedFirst() {
		this.context.autoConfig(AutoConfigA.class)
				.config(ConfigA.class).load();
		assertThat(this.context.getBean("a")).isEqualTo("autoconfig-a");
	}

	@Test
	public void autoConfigureFirstIsAppliedProperly() {
		this.context.autoConfig(ConfigA.class)
				.autoConfigFirst(AutoConfigA.class).load();
		assertThat(this.context.getBean("a")).isEqualTo("a");
	}

	@Test
	public void configurationIsAdditive() {
		this.context.config(AutoConfigA.class).config(AutoConfigB.class).load();
		assertThat(this.context.containsBean("a")).isTrue();
		assertThat(this.context.containsBean("b")).isTrue();
	}

	@Test
	public void autoConfigurationIsAdditive() {
		this.context.autoConfig(AutoConfigA.class).autoConfig(AutoConfigB.class).load();
		assertThat(this.context.containsBean("a")).isTrue();
		assertThat(this.context.containsBean("b")).isTrue();
	}

	@Test
	public void envIsAdditive() {
		this.context.env("test.foo=1").env("test.bar=2").load();
		ConfigurableEnvironment environment = this.context.getBean(
				ConfigurableEnvironment.class);
		assertThat(environment.getProperty("test.foo", Integer.class)).isEqualTo(1);
		assertThat(environment.getProperty("test.bar", Integer.class)).isEqualTo(2);
	}

	@Test
	public void envOverridesExistingKey() {
		this.context.env("test.foo=1").env("test.foo=2").load("test.foo=3");
		assertThat(this.context.getBean(ConfigurableEnvironment.class)
				.getProperty("test.foo", Integer.class)).isEqualTo(3);
	}

	@Test
	public void closeContextDisposeIt() {
		this.context.load(ConfigA.class);
		assertThat(this.context.containsBean("a")).isTrue();
		this.context.close();
		this.thrown.expectMessage(
				"make sure the test is calling one of the 'load' methods");
		this.context.containsBean("a");
	}


	@Configuration
	static class ConfigA {

		@Bean
		public String a() {
			return "a";
		}

	}

	@Configuration
	static class ConfigB {

		@Bean
		public Integer b() {
			return 1;
		}

	}

	@Configuration
	static class AutoConfigA {

		@Bean
		public String a() {
			return "autoconfig-a";
		}

	}

	@Configuration
	static class AutoConfigB {

		@Bean
		public Integer b() {
			return 42;
		}

	}

}
