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
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics on all available
 * {@link ThreadPoolTaskScheduler task schedulers}.
 *
 * @author Stephane Nicoll
 * @since 2.6.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter({ MetricsAutoConfiguration.class, SimpleMetricsExportAutoConfiguration.class,
		TaskSchedulingAutoConfiguration.class })
@ConditionalOnClass(ExecutorServiceMetrics.class)
@ConditionalOnBean({ ThreadPoolTaskScheduler.class, MeterRegistry.class })
public class TaskSchedulingMetricsAutoConfiguration {

	private static final String TASK_SCHEDULER_SUFFIX = "taskScheduler";

	@Autowired
	public void bindTaskSchedulersToRegistry(Map<String, ThreadPoolTaskScheduler> taskSchedulers,
			MeterRegistry registry) {
		taskSchedulers.forEach((beanName, taskScheduler) -> {
			ThreadPoolExecutor scheduler = safeGetThreadPoolExecutor(taskScheduler);
			if (scheduler != null) {
				ExecutorServiceMetrics.monitor(registry, scheduler, getTaskSchedulerName(beanName));
			}
		});
	}

	private ThreadPoolExecutor safeGetThreadPoolExecutor(ThreadPoolTaskScheduler taskScheduler) {
		try {
			return taskScheduler.getScheduledThreadPoolExecutor();
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
	private String getTaskSchedulerName(String beanName) {
		if (beanName.equals(TASK_SCHEDULER_SUFFIX)) {
			return "application";
		}
		if (beanName.length() > TASK_SCHEDULER_SUFFIX.length()
				&& StringUtils.endsWithIgnoreCase(beanName, TASK_SCHEDULER_SUFFIX)) {
			return beanName.substring(0, beanName.length() - TASK_SCHEDULER_SUFFIX.length());
		}
		return beanName;
	}

}
