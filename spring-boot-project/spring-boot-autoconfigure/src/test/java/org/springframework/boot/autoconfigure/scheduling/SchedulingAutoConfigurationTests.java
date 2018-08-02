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

package org.springframework.boot.autoconfigure.scheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutorAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SchedulingAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class SchedulingAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestConfiguration.class)
			.withConfiguration(AutoConfigurations.of(TaskExecutorAutoConfiguration.class,
					SchedulingAutoConfiguration.class));

	@Test
	public void noSchedulingDoesNotExposeTaskExecutor() {
		this.contextRunner.run(
				(context) -> assertThat(context).doesNotHaveBean(TaskExecutor.class));
	}

	@Test
	public void enableAsyncWithNoTakExecutorAutoConfiguresOne() {
		this.contextRunner
				.withPropertyValues("spring.task.thread-name-prefix=executor-test-")
				.withUserConfiguration(AsyncConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(TaskExecutor.class);
					TestBean bean = context.getBean(TestBean.class);
					String text = bean.echo("test").get();
					assertThat(text).contains("executor-test-").contains("test");
				});
	}

	@Test
	public void enableAsyncWithExistingTakExecutorBacksOff() {
		this.contextRunner.withUserConfiguration(AsyncConfiguration.class,
				TaskExecutorConfiguration.class).run((context) -> {
					assertThat(context).hasSingleBean(TaskExecutor.class);
					assertThat(context.getBean(TaskExecutor.class))
							.isInstanceOf(TestExecutor.class);
					TestBean bean = context.getBean(TestBean.class);
					String text = bean.echo("test").get();
					assertThat(text).isEqualTo("test-1 test");
				});
	}

	@Test
	public void enableAsyncWithConfigurerBacksOff() {
		this.contextRunner.withUserConfiguration(AsyncConfiguration.class,
				AsyncConfigurerConfiguration.class).run((context) -> {
					assertThat(context).doesNotHaveBean(TaskExecutor.class);
					TestBean bean = context.getBean(TestBean.class);
					String text = bean.echo("test").get();
					assertThat(text).isEqualTo("test-1 test");
				});
	}

	@Configuration
	@EnableAsync
	static class AsyncConfiguration {

	}

	@Configuration
	static class TaskExecutorConfiguration {

		@Bean
		public TaskExecutor taskExecutor() {
			return new TestExecutor();
		}

	}

	@Configuration
	static class AsyncConfigurerConfiguration implements AsyncConfigurer {

		@Override
		public Executor getAsyncExecutor() {
			return new TestExecutor();
		}

	}

	@Configuration
	static class TestConfiguration {

		@Bean
		public TestBean testBean() {
			return new TestBean();
		}

	}

	static class TestBean {

		@Async
		public Future<String> echo(String text) {
			return new AsyncResult<>(Thread.currentThread().getName() + " " + text);
		}

	}

	private static class TestExecutor extends ThreadPoolTaskExecutor {

		TestExecutor() {
			setCorePoolSize(1);
			setMaxPoolSize(1);
			setThreadNamePrefix("test-");
			afterPropertiesSet();
		}

	}

}
