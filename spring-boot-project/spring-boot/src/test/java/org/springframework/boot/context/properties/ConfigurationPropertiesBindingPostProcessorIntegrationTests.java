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

package org.springframework.boot.context.properties;

import java.util.Collections;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * Integration tests for {@link ConfigurationPropertiesBindingPostProcessor} that
 * demonstrates how the delegate can be changed.
 *
 * @author Stephane Nicoll
 */
public class ConfigurationPropertiesBindingPostProcessorIntegrationTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void bindWithCustomBinder() {
		load(CustomBinderConfiguration.class, "test.bar=biz");
		assertThat(this.context.getBean(TestConfiguration.class).getBar())
				.isEqualTo("replaced");
	}

	@Test
	public void bindWithInvalidFactory() {
		try {
			load(InvalidConfiguration.class);
			fail("Should have failed");
		}
		catch (BeanCreationException ex) {
			ex.getMessage().contains("Bean with name " + ConfigurationPropertiesBinderFactory.class.getName());
			ex.getMessage().contains(String.class.getName());
		}
	}

	private void load(Class<?> configuration, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		ctx.register(configuration);
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(ctx, environment);
		ctx.refresh();
		this.context = ctx;
	}


	@ConfigurationProperties("test")
	public static class TestConfiguration {

		private String bar;

		public void setBar(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return this.bar;
		}

	}

	@Configuration
	@EnableConfigurationProperties(TestConfiguration.class)
	static class CustomBinderConfiguration {

		@Bean(name = "org.springframework.boot.context.properties.ConfigurationPropertiesBinderFactory")
		public TestConfigurationPropertiesBinderFactory customBinderFactory() {
			return new TestConfigurationPropertiesBinderFactory();
		}

	}

	@Configuration
	@EnableConfigurationProperties(TestConfiguration.class)
	static class InvalidConfiguration {

		@Bean(name = "org.springframework.boot.context.properties.ConfigurationPropertiesBinderFactory")
		public String myFactory() {
			return "Wrong factory";
		}

	}

	private static class TestConfigurationPropertiesBinderFactory
			implements ConfigurationPropertiesBinderFactory {

		@Override
		public ConfigurationPropertiesBinder createBinder(
				ConfigurationPropertiesBindingContext context) {
			MutablePropertySources sources = new MutablePropertySources();
			sources.addFirst(new MapPropertySource("test",
					Collections.singletonMap("test.bar", "replaced")));
			return new TestConfigurationPropertiesBinder(sources);
		}

	}

	private static class TestConfigurationPropertiesBinder
			extends DefaultConfigurationPropertiesBinder {

		protected TestConfigurationPropertiesBinder(
				Iterable<PropertySource<?>> propertySources) {
			super(propertySources, null, null);
		}

	}

}
