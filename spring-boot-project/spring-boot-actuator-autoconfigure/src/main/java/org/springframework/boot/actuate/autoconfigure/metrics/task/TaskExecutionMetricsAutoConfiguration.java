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

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link ThreadPoolTaskExecutor task executors}.
 *
 * @author Stephane Nicoll
 * @since 2.6.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class,
		TaskExecutionAutoConfiguration.class })
@ConditionalOnClass(ExecutorServiceMetrics.class)
@ConditionalOnBean({ ThreadPoolTaskExecutor.class, MeterRegistry.class })
public class TaskExecutionMetricsAutoConfiguration {

	private static final String TASK_EXECUTOR_SUFFIX = "taskExecutor";

	@Autowired
	public void bindTaskExecutorsToRegistry(Map<String, ThreadPoolTaskExecutor> taskExecutors, MeterRegistry registry) {
		taskExecutors.forEach((beanName, taskExecutor) -> {
			ThreadPoolExecutor executor = safeGetThreadPoolExecutor(taskExecutor);
			if (executor != null) {
				ExecutorServiceMetrics.monitor(registry, executor, getTaskExecutorName(beanName));
			}
		});
	}

	private ThreadPoolExecutor safeGetThreadPoolExecutor(ThreadPoolTaskExecutor taskExecutor) {
		try {
			return taskExecutor.getThreadPoolExecutor();
		}
		catch (IllegalStateException ex) {
			return null;
		}
	}

	/**
	 * Get the name of a {@link ThreadPoolTaskExecutor} based on its {@code beanName}.
	 * @param beanName the name of the {@link ThreadPoolTaskExecutor} bean
	 * @return a name for the given task executor
	 */
	private String getTaskExecutorName(String beanName) {
		if (beanName.length() > TASK_EXECUTOR_SUFFIX.length()
				&& StringUtils.endsWithIgnoreCase(beanName, TASK_EXECUTOR_SUFFIX)) {
			return beanName.substring(0, beanName.length() - TASK_EXECUTOR_SUFFIX.length());
		}
		return beanName;
	}

}
