/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link ConfigFileCondition}.
 *
 * @author Stephane Nicoll
 */
public class ConfigFileConditionTests {

	private ConfigurableApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultFileAndNoExplicitKey() {
		load(DefaultFileConfiguration.class);
		assertTrue(this.context.containsBean("foo"));
	}

	@Test
	public void noDefaultFileAndNoExplicitKey() {
		load(NoDefaultFileConfiguration.class);
		assertFalse(this.context.containsBean("foo"));
	}

	@Test
	public void noDefaultFileAndExplicitKeyToResource() {
		load(NoDefaultFileConfiguration.class, "spring.foo.test.config=logging.properties");
		assertTrue(this.context.containsBean("foo"));
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(config);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	@Configuration
	@Conditional(ConfigFileDefaultFileCondition.class)
	static class DefaultFileConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}
	}

	@Configuration
	@Conditional(ConfigFileNoDefaultFileCondition.class)
	static class NoDefaultFileConfiguration {

		@Bean
		public String foo() {
			return "foo";
		}
	}

	private static class ConfigFileDefaultFileCondition extends
			ConfigFileCondition {

		public ConfigFileDefaultFileCondition() {
			super("test", "spring.foo.test.", "config", "classpath:/logging.properties");
		}
	}

	private static class ConfigFileNoDefaultFileCondition extends
			ConfigFileCondition {
		public ConfigFileNoDefaultFileCondition() {
			super("test", "spring.foo.test", "config",
					"classpath:/this-file-does-not-exist.xml");
		}

	}
}
