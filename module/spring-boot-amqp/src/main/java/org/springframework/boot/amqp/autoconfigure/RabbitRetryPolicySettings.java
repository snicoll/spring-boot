/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.amqp.autoconfigure;

import java.time.Duration;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.amqp.autoconfigure.RabbitProperties.Retry;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryPolicy.Builder;

/**
 * Defines the settings of a {@link RetryPolicy}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public final class RabbitRetryPolicySettings {

	/**
	 * Default number of retry attempts.
	 */
	public static final long DEFAULT_MAX_ATTEMPTS = RetryPolicy.Builder.DEFAULT_MAX_ATTEMPTS;

	/**
	 * Default initial delay.
	 */
	public static final Duration DEFAULT_DELAY = Duration.ofMillis(RetryPolicy.Builder.DEFAULT_DELAY);

	/**
	 * Default multiplier, uses a fixed delay.
	 */
	public static final double DEFAULT_MULTIPLIER = RetryPolicy.Builder.DEFAULT_MULTIPLIER;

	/**
	 * Default maximum delay (infinite).
	 */
	public static final Duration DEFAULT_MAX_DELAY = Duration.ofMillis(RetryPolicy.Builder.DEFAULT_MAX_DELAY);

	private Long maxAttempts = DEFAULT_MAX_ATTEMPTS;

	private Duration delay = DEFAULT_DELAY;

	private Double multiplier = DEFAULT_MULTIPLIER;

	private Duration maxDelay = DEFAULT_MAX_DELAY;

	private @Nullable Function<Builder, RetryPolicy> factory;

	/**
	 * Create an instance based on the specified {@link Retry} properties.
	 * @param retryProperties the properties to use to initialize the instance
	 * @return a new instance initialized with the state of the given retry properties
	 */
	static RabbitRetryPolicySettings from(Retry retryProperties) {
		RabbitRetryPolicySettings settings = new RabbitRetryPolicySettings();
		PropertyMapper map = PropertyMapper.get();
		map.from(retryProperties::getMaxAttempts).to(settings::setMaxAttempts);
		map.from(retryProperties::getInitialInterval).to(settings::setDelay);
		map.from(retryProperties::getMultiplier).to(settings::setMultiplier);
		map.from(retryProperties::getMaxInterval).to(settings::setMaxDelay);
		return settings;
	}

	public Long getMaxAttempts() {
		return this.maxAttempts;
	}

	public void setMaxAttempts(Long maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	public Duration getDelay() {
		return this.delay;
	}

	public void setDelay(Duration delay) {
		this.delay = delay;
	}

	public Double getMultiplier() {
		return this.multiplier;
	}

	public void setMultiplier(Double multiplier) {
		this.multiplier = multiplier;
	}

	public Duration getMaxDelay() {
		return this.maxDelay;
	}

	public void setMaxDelay(Duration maxDelay) {
		this.maxDelay = maxDelay;
	}

	public void setFactory(Function<Builder, RetryPolicy> factory) {
		this.factory = factory;
	}

	RetryPolicy createRetryPolicy() {
		PropertyMapper map = PropertyMapper.get();
		RetryPolicy.Builder builder = RetryPolicy.builder();
		map.from(this::getMaxAttempts).to(builder::maxAttempts);
		map.from(this::getDelay).to(builder::delay);
		map.from(this::getMultiplier).to(builder::multiplier);
		map.from(this::getMaxDelay).to(builder::maxDelay);
		return (this.factory != null) ? this.factory.apply(builder) : builder.build();
	}

}
