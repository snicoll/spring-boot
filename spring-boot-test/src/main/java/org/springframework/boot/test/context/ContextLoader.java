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

package org.springframework.boot.test.context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A test helper to manage {@link ApplicationContext}.
 *
 * @author Stephane Nicoll$
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class ContextLoader {

	private List<String> env = new ArrayList<>();

	private LinkedList<Class<?>> autoConfigurations = new LinkedList<>();

	private Set<Class<?>> userConfigurations = new LinkedHashSet<>();

	public ContextLoader env(String... environment) {
		if (!ObjectUtils.isEmpty(environment)) {
			this.env.addAll(Arrays.asList(environment));
		}
		return this;
	}

	public ContextLoader autoConfig(Class<?>... autoConfigurations) {
		if (!ObjectUtils.isEmpty(autoConfigurations)) {
			this.autoConfigurations.addAll(Arrays.asList(autoConfigurations));
		}
		return this;
	}

	public ContextLoader autoConfigFirst(Class<?> autoConfiguration) {
		this.autoConfigurations.addFirst(autoConfiguration);
		return this;
	}

	public ContextLoader config(Class<?>... configs) {
		if (!ObjectUtils.isEmpty(configs)) {
			this.userConfigurations.addAll(Arrays.asList(configs));
		}
		return this;
	}

	public void load(ContextConsumer consumer) {
		try (ConfigurableApplicationContext ctx = load()) {
			try {
				consumer.accept(ctx);
			}
			catch (RuntimeException ex) {
				throw ex;
			}
			catch (Throwable ex) {
				throw new IllegalStateException("An unexpected error occurred: "
						+ ex.getMessage(), ex);
			}
		}
	}

	public void loadAndFail(Consumer<Throwable> consumer) {
		loadAndFail(Throwable.class, consumer);
	}

	public <T extends Throwable> void loadAndFail(Class<T> exceptionType,
			Consumer<T> consumer) {
		try {
			load();
			throw new AssertionError("ApplicationContext should have failed");
		}
		catch (Throwable ex) {
			assertThat(ex).isInstanceOf(exceptionType);
			consumer.accept(exceptionType.cast(ex));
		}
	}

	private ConfigurableApplicationContext load() {
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
		return ctx;
	}

}
