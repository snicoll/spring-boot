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

package org.springframework.boot.task;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Builder that can be used to configure and create a {@link TaskExecutor}. Provides
 * convenience methods to set common {@link ThreadPoolTaskExecutor} settings and register
 * {@link #taskDecorator(TaskDecorator)}).
 * <p>
 * For advanced configuration, use {@link TaskExecutorCustomizer}. In a typical
 * auto-configured Spring Boot application this builder is available as a bean and can be
 * injected whenever a {@link TaskExecutor} is needed.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class TaskExecutorBuilder {

	private final Integer corePoolSize;

	private final Integer maxPoolSize;

	private final Duration keepAlive;

	private final Integer queueCapacity;

	private final Boolean allowCoreThreadTimeOut;

	private final String threadNamePrefix;

	private final TaskDecorator taskDecorator;

	private final Set<TaskExecutorCustomizer> taskExecutorCustomizers;

	public TaskExecutorBuilder(TaskExecutorCustomizer... taskExecutorCustomizers) {
		Assert.notNull(taskExecutorCustomizers,
				"TaskExecutorCustomizers must not be null");
		this.corePoolSize = null;
		this.maxPoolSize = null;
		this.keepAlive = null;
		this.queueCapacity = null;
		this.allowCoreThreadTimeOut = null;
		this.threadNamePrefix = null;
		this.taskDecorator = null;
		this.taskExecutorCustomizers = Collections.unmodifiableSet(
				new LinkedHashSet<>(Arrays.asList(taskExecutorCustomizers)));
	}

	public TaskExecutorBuilder(Integer corePoolSize, Integer maxPoolSize,
			Duration keepAlive, Integer queueCapacity, Boolean allowCoreThreadTimeOut,
			String threadNamePrefix, TaskDecorator taskDecorator,
			Set<TaskExecutorCustomizer> taskExecutorCustomizers) {
		this.corePoolSize = corePoolSize;
		this.maxPoolSize = maxPoolSize;
		this.keepAlive = keepAlive;
		this.queueCapacity = queueCapacity;
		this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
		this.threadNamePrefix = threadNamePrefix;
		this.taskDecorator = taskDecorator;
		this.taskExecutorCustomizers = taskExecutorCustomizers;
	}

	public TaskExecutorBuilder corePoolSize(int corePoolSize) {
		return new TaskExecutorBuilder(corePoolSize, this.maxPoolSize, this.keepAlive,
				this.queueCapacity, this.allowCoreThreadTimeOut, this.threadNamePrefix,
				this.taskDecorator, this.taskExecutorCustomizers);
	}

	public TaskExecutorBuilder maxPoolSize(int maxPoolSize) {
		return new TaskExecutorBuilder(this.corePoolSize, maxPoolSize, this.keepAlive,
				this.queueCapacity, this.allowCoreThreadTimeOut, this.threadNamePrefix,
				this.taskDecorator, this.taskExecutorCustomizers);
	}

	public TaskExecutorBuilder keepAlive(Duration keepAlive) {
		return new TaskExecutorBuilder(this.corePoolSize, this.maxPoolSize, keepAlive,
				this.queueCapacity, this.allowCoreThreadTimeOut, this.threadNamePrefix,
				this.taskDecorator, this.taskExecutorCustomizers);
	}

	public TaskExecutorBuilder queueCapacity(int queueCapacity) {
		return new TaskExecutorBuilder(this.corePoolSize, this.maxPoolSize,
				this.keepAlive, queueCapacity, this.allowCoreThreadTimeOut,
				this.threadNamePrefix, this.taskDecorator, this.taskExecutorCustomizers);
	}

	public TaskExecutorBuilder allowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
		return new TaskExecutorBuilder(this.corePoolSize, this.maxPoolSize,
				this.keepAlive, this.queueCapacity, allowCoreThreadTimeOut,
				this.threadNamePrefix, this.taskDecorator, this.taskExecutorCustomizers);
	}

	public TaskExecutorBuilder threadNamePrefix(String threadNamePrefix) {
		return new TaskExecutorBuilder(this.corePoolSize, this.maxPoolSize,
				this.keepAlive, this.queueCapacity, this.allowCoreThreadTimeOut,
				threadNamePrefix, this.taskDecorator, this.taskExecutorCustomizers);
	}

	public TaskExecutorBuilder taskDecorator(TaskDecorator taskDecorator) {
		return new TaskExecutorBuilder(this.corePoolSize, this.maxPoolSize,
				this.keepAlive, this.queueCapacity, this.allowCoreThreadTimeOut,
				this.threadNamePrefix, taskDecorator, this.taskExecutorCustomizers);
	}

	public TaskExecutorBuilder customizers(
			TaskExecutorCustomizer... taskExecutorCustomizers) {
		Assert.notNull(taskExecutorCustomizers,
				"TaskExecutorCustomizers must not be null");
		return customizers(Arrays.asList(taskExecutorCustomizers));
	}

	public TaskExecutorBuilder customizers(
			Collection<? extends TaskExecutorCustomizer> taskExecutorCustomizers) {
		Assert.notNull(taskExecutorCustomizers,
				"TaskExecutorCustomizers must not be null");
		return new TaskExecutorBuilder(this.corePoolSize, this.maxPoolSize,
				this.keepAlive, this.queueCapacity, this.allowCoreThreadTimeOut,
				this.threadNamePrefix, this.taskDecorator,
				Collections.unmodifiableSet(new LinkedHashSet<TaskExecutorCustomizer>(
						taskExecutorCustomizers)));
	}

	public TaskExecutorBuilder additionalCustomizers(
			TaskExecutorCustomizer... taskExecutorCustomizers) {
		Assert.notNull(taskExecutorCustomizers,
				"TaskExecutorCustomizers must not be null");
		return additionalCustomizers(Arrays.asList(taskExecutorCustomizers));
	}

	public TaskExecutorBuilder additionalCustomizers(
			Collection<? extends TaskExecutorCustomizer> taskExecutorCustomizers) {
		Assert.notNull(taskExecutorCustomizers,
				"TaskExecutorCustomizers must not be null");
		return new TaskExecutorBuilder(this.corePoolSize, this.maxPoolSize,
				this.keepAlive, this.queueCapacity, this.allowCoreThreadTimeOut,
				this.threadNamePrefix, this.taskDecorator,
				append(this.taskExecutorCustomizers, taskExecutorCustomizers));
	}

	public ThreadPoolTaskExecutor build() {
		return build(ThreadPoolTaskExecutor.class);
	}

	public <T extends ThreadPoolTaskExecutor> T build(Class<T> taskExecutorClass) {
		return configure(BeanUtils.instantiateClass(taskExecutorClass));
	}

	public <T extends ThreadPoolTaskExecutor> T configure(T taskExecutor) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(() -> this.corePoolSize).to(taskExecutor::setCorePoolSize);
		map.from(() -> this.maxPoolSize).to(taskExecutor::setMaxPoolSize);
		map.from(() -> this.keepAlive).asInt(Duration::getSeconds)
				.to(taskExecutor::setKeepAliveSeconds);
		map.from(() -> this.queueCapacity).to(taskExecutor::setQueueCapacity);
		map.from(() -> this.allowCoreThreadTimeOut)
				.to(taskExecutor::setAllowCoreThreadTimeOut);
		map.from(() -> this.threadNamePrefix).whenHasText()
				.to(taskExecutor::setThreadNamePrefix);
		map.from(() -> this.taskDecorator).to(taskExecutor::setTaskDecorator);

		if (!CollectionUtils.isEmpty(this.taskExecutorCustomizers)) {
			for (TaskExecutorCustomizer customizer : this.taskExecutorCustomizers) {
				customizer.customize(taskExecutor);
			}
		}
		return taskExecutor;
	}

	private static <T> Set<T> append(Set<T> set, Collection<? extends T> additions) {
		Set<T> result = new LinkedHashSet<>((set != null) ? set : Collections.emptySet());
		result.addAll(additions);
		return Collections.unmodifiableSet(result);
	}

}
