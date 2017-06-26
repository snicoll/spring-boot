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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ObjectUtils;

/**
 * JUnit {@code @Rule} to manage a Spring {@link ApplicationContext}.
 *
 * @author Stephane Nicoll
 */
public class SpringContext implements TestRule {

	private boolean initialized;

	private boolean configured;

	private boolean loaded;

	private List<String> env = new ArrayList<>();

	private LinkedList<Class<?>> autoConfigurations = new LinkedList<>();

	private Set<Class<?>> userConfigurations = new LinkedHashSet<>();

	private final Deque<ConfigurableApplicationContext> contexts = new ArrayDeque<>();

	public SpringContext env(String... environment) {
		triggerConfigurationInvoked();
		if (!ObjectUtils.isEmpty(environment)) {
			this.env.addAll(Arrays.asList(environment));
		}
		return this;
	}

	public SpringContext autoConfig(Class<?>... autoConfigurations) {
		triggerConfigurationInvoked();
		if (!ObjectUtils.isEmpty(autoConfigurations)) {
			this.autoConfigurations.addAll(Arrays.asList(autoConfigurations));
		}
		return this;
	}

	public SpringContext autoConfigFirst(Class<?> autoConfiguration) {
		triggerConfigurationInvoked();
		this.autoConfigurations.addFirst(autoConfiguration);
		return this;
	}

	public SpringContext config(Class<?>... configs) {
		triggerConfigurationInvoked();
		if (!ObjectUtils.isEmpty(configs)) {
			this.userConfigurations.addAll(Arrays.asList(configs));
		}
		return this;
	}

	public ConfigurableApplicationContext load() {
		this.loaded = true;
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		if (!ObjectUtils.isEmpty(this.env)) {
			TestPropertyValues.of(this.env.toArray(new String[this.env.size()]))
					.applyTo(ctx);
		}
		if (!ObjectUtils.isEmpty(this.userConfigurations)) {
			ctx.register(this.userConfigurations.toArray(
					new Class<?>[this.userConfigurations.size()]));
		}
		if (!ObjectUtils.isEmpty(this.autoConfigurations)) {
			ctx.register(this.autoConfigurations.toArray(
					new Class<?>[this.autoConfigurations.size()]));
		}
		ctx.refresh();
		this.contexts.push(ctx);
		return ctx;
	}

	public <T> T getBean(Class<T> type) {
		return getApplicationContext().getBean(type);
	}

	public Object getBean(String beanName) {
		return getApplicationContext().getBean(beanName);
	}

	public <T> T getBean(String beanName, Class<T> type) {
		return getApplicationContext().getBean(beanName, type);
	}

	public <T> Map<String, T> getBeansOfType(Class<T> target) {
		return getApplicationContext().getBeansOfType(target);
	}

	public boolean containsBean(String beanName) {
		return getApplicationContext().containsBean(beanName);
	}

	public void close() {
		ConfigurableApplicationContext context = this.contexts.poll();
		if (context != null) {
			context.close();
		}
	}

	private ConfigurableApplicationContext getApplicationContext() {
		if (!this.contexts.isEmpty()) {
			return this.contexts.peek();

		}
		if (this.loaded) {
			throw new IllegalStateException("The ApplicationContext has been closed already");
		}
		else {
			throw new IllegalStateException("The ApplicationContext has not be started, "
					+ "make sure the test is calling one of the 'load' methods");
		}
	}

	private void triggerConfigurationInvoked() {
		if (this.initialized) {
			this.configured = true;
		}
	}

	@Override
	public Statement apply(Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try {
					SpringContext.this.initialized = true;
					base.evaluate();
				}
				finally {
					closeAll();
				}

			}
		};
	}

	private void closeAll() {
		if (this.configured && !this.loaded) {
			throw new IllegalStateException("SpringContext was configured in the test "
					+ "but load() was not invoked. Did you forget to actually load the "
					+ "context?");
		}
		for (ConfigurableApplicationContext context : this.contexts) {
			if (context.isActive()) {
				context.close();
			}
		}
	}

}
