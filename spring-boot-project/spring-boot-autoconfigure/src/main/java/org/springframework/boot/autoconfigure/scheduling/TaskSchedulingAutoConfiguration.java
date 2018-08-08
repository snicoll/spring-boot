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

import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.TaskManagementConfigUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link TaskScheduler}.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@Configuration
@ConditionalOnBean(name = TaskManagementConfigUtils.SCHEDULED_ANNOTATION_PROCESSOR_BEAN_NAME)
@EnableConfigurationProperties(TaskSchedulingProperties.class)
public class TaskSchedulingAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean({ SchedulingConfigurer.class, TaskScheduler.class })
	public ThreadPoolTaskScheduler taskScheduler(
			Supplier<ThreadPoolTaskScheduler> taskSchedulerFactory) {
		return taskSchedulerFactory.get();
	}

	@Bean
	@ConditionalOnMissingBean
	public Supplier<ThreadPoolTaskScheduler> taskSchedulerFactory(
			TaskSchedulingProperties properties,
			ObjectProvider<TaskSchedulerCustomizer> taskSchedulerCustomizers) {
		return new TaskSchedulerFactory(properties,
				taskSchedulerCustomizers.stream().collect(Collectors.toList()));
	}

}
