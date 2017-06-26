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
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.context.ConfigurableApplicationContext;
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

	private ConfigurableApplicationContext applicationContext;

	@After
	public void closeContext() {
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	public void getBeanWithNoContext() {
		this.thrown.expectMessage(
				"make sure the test is calling one of the 'load' methods");
		this.context.getBean("foo");
	}

	@Test
	public void configurationIsProcessedInOrder() {
		this.applicationContext = this.context.config(ConfigA.class, AutoConfigA.class)
				.load();
		assertThat(this.context.getBean("a")).isEqualTo("autoconfig-a");
	}

	@Test
	public void useConfigurationIsProcessedFirst() {
		this.applicationContext = this.context.autoConfig(AutoConfigA.class)
				.config(ConfigA.class).load();
		assertThat(this.context.getBean("a")).isEqualTo("autoconfig-a");
	}

	@Test
	public void autoConfigureFirstIsAppliedProperly() {
		this.applicationContext = this.context.autoConfig(ConfigA.class)
				.autoConfigFirst(AutoConfigA.class).load();
		assertThat(this.context.getBean("a")).isEqualTo("a");
	}

	@Test
	public void configurationIsAdditive() {
		this.applicationContext = this.context.config(AutoConfigA.class)
				.config(AutoConfigB.class).load();
		assertThat(this.context.containsBean("a")).isTrue();
		assertThat(this.context.containsBean("b")).isTrue();
	}

	@Test
	public void autoConfigurationIsAdditive() {
		this.applicationContext = this.context.autoConfig(AutoConfigA.class)
				.autoConfig(AutoConfigB.class).load();
		assertThat(this.context.containsBean("a")).isTrue();
		assertThat(this.context.containsBean("b")).isTrue();
	}

	@Test
	public void envIsAdditive() {
		this.applicationContext = this.context.env("test.foo=1").env("test.bar=2")
				.load();
		ConfigurableEnvironment environment = this.context.getBean(
				ConfigurableEnvironment.class);
		assertThat(environment.getProperty("test.foo", Integer.class)).isEqualTo(1);
		assertThat(environment.getProperty("test.bar", Integer.class)).isEqualTo(2);
	}

	@Test
	public void envOverridesExistingKey() {
		this.applicationContext = this.context.env("test.foo=1").env("test.foo=2").load();
		assertThat(this.context.getBean(ConfigurableEnvironment.class)
				.getProperty("test.foo", Integer.class)).isEqualTo(2);
	}

	@Test
	public void closeContextDisposeIt() {
		this.context.config(ConfigA.class).load();
		assertThat(this.context.containsBean("a")).isTrue();
		this.context.close();
		this.thrown.expectMessage("The ApplicationContext has been closed already");
		this.context.containsBean("a");
	}

	@Test
	public void contextIsClosedAutomatically() throws Throwable {
		TestStatement statement = new TestStatement(this.context);
		this.context.apply(statement, createTestDescription()).evaluate();
		assertThat(statement.applicationContext.isActive()).isFalse();
	}

	@Test
	public void loadIsMandatoryIfRuleIsConfigured() throws Throwable {
		this.context.env("spring.foo=bar"); // Configured outside the test
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Did you forget to actually load the context?");

		this.context.apply(new Statement() {
			@Override
			public void evaluate() throws Throwable {
				SpringContextTests.this.context.env("spring.bar=foo");
			}
		}, createTestDescription()).evaluate();
	}

	private Description createTestDescription() {
		return Description.createTestDescription(
				SpringContextTests.class, "test");
	}

	static class TestStatement extends Statement {

		private final SpringContext rule;

		private ConfigurableApplicationContext applicationContext;

		TestStatement(SpringContext rule) {
			this.rule = rule;
		}

		@Override
		public void evaluate() throws Throwable {
			this.applicationContext = this.rule.config(ConfigA.class)
					.env("test.foo=bar").load();
			assertThat(this.applicationContext.getBean("a")).isEqualTo("a");
		}

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
