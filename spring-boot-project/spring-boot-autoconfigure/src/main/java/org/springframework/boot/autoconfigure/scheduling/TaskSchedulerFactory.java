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

import java.util.List;
import java.util.function.Supplier;

import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Factory to create {@link ThreadPoolTaskScheduler}.
 *
 * @author Stephane Nicoll
 */
class TaskSchedulerFactory implements Supplier<ThreadPoolTaskScheduler> {

	private final TaskSchedulingProperties properties;

	private final List<TaskSchedulerCustomizer> taskSchedulerCustomizers;

	TaskSchedulerFactory(TaskSchedulingProperties properties,
			List<TaskSchedulerCustomizer> taskSchedulerCustomizers) {
		this.properties = properties;
		this.taskSchedulerCustomizers = taskSchedulerCustomizers;
	}

	@Override
	public ThreadPoolTaskScheduler get() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(this.properties.getPool().getSize());
		taskScheduler.setThreadNamePrefix(this.properties.getThreadNamePrefix());

		if (this.taskSchedulerCustomizers != null) {
			for (TaskSchedulerCustomizer customizer : this.taskSchedulerCustomizers) {
				customizer.customize(taskScheduler);
			}
		}
		return taskScheduler;
	}

}
