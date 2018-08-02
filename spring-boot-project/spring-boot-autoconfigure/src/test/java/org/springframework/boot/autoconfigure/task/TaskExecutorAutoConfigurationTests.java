/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.task;

import java.util.function.Consumer;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link TaskExecutorAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class TaskExecutorAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(TaskExecutorAutoConfiguration.class));

	@Test
	public void taskExecutorShouldApplyCustomSettings() {
		this.contextRunner
				.withPropertyValues("spring.task.pool.core-size=2",
						"spring.task.pool.max-size=4",
						"spring.task.pool.allow-core-thread-timeout=true",
						"spring.task.pool.keep-alive=5s", "spring.task.queue-capacity=10",
						"spring.task.thread-name-prefix=mytest-")
				.run(assertTaskExecutor((taskExecutor) -> {
					DirectFieldAccessor dfa = new DirectFieldAccessor(taskExecutor);
					assertThat(taskExecutor.getCorePoolSize()).isEqualTo(2);
					assertThat(taskExecutor.getMaxPoolSize()).isEqualTo(4);
					assertThat(dfa.getPropertyValue("allowCoreThreadTimeOut"))
							.isEqualTo(true);
					assertThat(taskExecutor.getKeepAliveSeconds()).isEqualTo(5);
					assertThat(dfa.getPropertyValue("queueCapacity")).isEqualTo(10);
					assertThat(taskExecutor.getThreadNamePrefix()).isEqualTo("mytest-");
				}));
	}

	@Test
	public void taskExecutorWhenHasCustomBuilderShouldUseCustomBuilder() {
		this.contextRunner.withUserConfiguration(CustomTaskExecutorBuilderConfig.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(TaskExecutorBuilder.class);
					assertThat(context.getBean(TaskExecutorBuilder.class))
							.isSameAs(context.getBean(
									CustomTaskExecutorBuilderConfig.class).taskExecutorBuilder);
				});
	}

	@Test
	public void taskExecutorShouldUseTaskDecorator() {
		this.contextRunner.withUserConfiguration(TaskDecoratorConfig.class)
				.run((context) -> {
					assertThat(context).hasSingleBean(TaskExecutorBuilder.class);
					ThreadPoolTaskExecutor executor = context
							.getBean(TaskExecutorBuilder.class).build();
					assertThat(ReflectionTestUtils.getField(executor, "taskDecorator"))
							.isSameAs(context.getBean(TaskDecorator.class));
				});
	}

	@Test
	public void taskExecutorShouldApplyCustomizer() {
		this.contextRunner.withUserConfiguration(TaskExecutorCustomizerConfig.class)
				.run((context) -> {
					TaskExecutorCustomizer customizer = context
							.getBean(TaskExecutorCustomizer.class);
					verify(customizer, never()).customize(any());
					ThreadPoolTaskExecutor executor = context
							.getBean(TaskExecutorBuilder.class).build();
					verify(customizer).customize(executor);
				});
	}

	private ContextConsumer<AssertableApplicationContext> assertTaskExecutor(
			Consumer<ThreadPoolTaskExecutor> taskExecutor) {
		return (context) -> {
			assertThat(context).hasSingleBean(TaskExecutorBuilder.class);
			TaskExecutorBuilder builder = context.getBean(TaskExecutorBuilder.class);
			taskExecutor.accept(builder.build());
		};
	}

	@Configuration
	static class CustomTaskExecutorBuilderConfig {

		private final TaskExecutorBuilder taskExecutorBuilder = new TaskExecutorBuilder();

		@Bean
		public TaskExecutorBuilder customTaskExecutorBuilder() {
			return this.taskExecutorBuilder;
		}

	}

	@Configuration
	static class TaskExecutorCustomizerConfig {

		@Bean
		public TaskExecutorCustomizer mockTaskExecutorCustomizer() {
			return mock(TaskExecutorCustomizer.class);
		}

	}

	@Configuration
	static class TaskDecoratorConfig {

		@Bean
		public TaskDecorator mockTaskDecorator() {
			return mock(TaskDecorator.class);
		}

	}

}
