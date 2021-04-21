/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics.task;

import java.util.Collection;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.metrics.test.MetricsRun;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TaskExecutionMetricsAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class TaskExecutionMetricsAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().with(MetricsRun.simple())
			.withConfiguration(AutoConfigurations.of(TaskExecutionMetricsAutoConfiguration.class));

	@Test
	void taskExecutorUsingAutoConfigurationIsInstrumented() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
				.run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					Collection<FunctionCounter> meters = registry.get("executor.completed").functionCounters();
					assertThat(meters).singleElement()
							.satisfies((meter) -> assertThat(meter.getId().getTag("name")).isEqualTo("application"));
				});
	}

	@Test
	void taskExecutorsWithCustomNamesAreInstrumented() {
		this.contextRunner.withBean("firstTaskExecutor", ThreadPoolTaskExecutor.class, ThreadPoolTaskExecutor::new)
				.withBean("customName", ThreadPoolTaskExecutor.class, ThreadPoolTaskExecutor::new).run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					Collection<FunctionCounter> meters = registry.get("executor.completed").functionCounters();
					assertThat(meters).map((meter) -> meter.getId().getTag("name")).containsExactlyInAnyOrder("first",
							"customName");
				});
	}

	@Test
	void threadPoolTaskExecutorWithNoTaskExecutorIsIgnored() {
		ThreadPoolTaskExecutor unavailableTaskExecutor = mock(ThreadPoolTaskExecutor.class);
		given(unavailableTaskExecutor.getThreadPoolExecutor()).willThrow(new IllegalStateException("Test"));
		this.contextRunner.withBean("firstTaskExecutor", ThreadPoolTaskExecutor.class, ThreadPoolTaskExecutor::new)
				.withBean("customName", ThreadPoolTaskExecutor.class, () -> unavailableTaskExecutor).run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					Collection<FunctionCounter> meters = registry.get("executor.completed").functionCounters();
					assertThat(meters).singleElement()
							.satisfies((meter) -> assertThat(meter.getId().getTag("name")).isEqualTo("first"));
				});
	}

	@Test
	void taskExecutorInstrumentationCanBeDisabled() {
		this.contextRunner.withPropertyValues("management.metrics.enable.executor=false")
				.withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class)).run((context) -> {
					MeterRegistry registry = context.getBean(MeterRegistry.class);
					assertThat(registry.find("executor.completed").tags("name", "application").functionCounter())
							.isNull();
				});
	}

}
