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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.health.ApplicationHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicatorReactiveAdapter;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveRedisHealthIndicator;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.ContextConsumer;
import org.springframework.boot.test.context.ContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HealthIndicatorAutoConfiguration} with reactive.
 *
 * @author Stephane Nicoll
 */
public class ReactiveHealthIndicatorAutoConfigurationTests {

	public final ContextLoader contextLoader = new ContextLoader()
			.autoConfig(HealthIndicatorAutoConfiguration.class,
					ManagementServerProperties.class);

	@Test
	public void defaultHealthIndicator() {
		this.contextLoader.env("management.health.diskspace.enabled:false").load(hasOnlyApplicationHealthIndicator());
	}

	@Test
	public void defaultHealthIndicatorsDisabled() {
		this.contextLoader.env().load(hasOnlyApplicationHealthIndicator());
	}

	@Test
	public void redisHealthIndicator() {
		this.contextLoader.autoConfigFirst(RedisAutoConfiguration.class,
				RedisReactiveAutoConfiguration.class).load(
						hasSingleHealthIndicator(ReactiveRedisHealthIndicator.class));
	}

	@Test
	public void notRedisHealthIndicator() {
		this.contextLoader.autoConfigFirst(RedisAutoConfiguration.class,
				RedisReactiveAutoConfiguration.class)
				.env("management.health.redis.enabled:false")
				.load(hasOnlyApplicationHealthIndicator());
	}

	private ContextConsumer hasOnlyApplicationHealthIndicator() {
		return context -> {
			Map<String, ReactiveHealthIndicator> beans = context
					.getBeansOfType(ReactiveHealthIndicator.class);
			assertThat(beans).hasSize(1);
			ReactiveHealthIndicator healthIndicator = beans.values().iterator().next();
			assertThat(healthIndicator.getClass())
					.isEqualTo(HealthIndicatorReactiveAdapter.class);
			assertThat(((HealthIndicatorReactiveAdapter) healthIndicator)
					.getHealthIndicator().getClass()).isEqualTo(
					ApplicationHealthIndicator.class);
		};
	}

	private ContextConsumer hasSingleHealthIndicator(
			Class<? extends ReactiveHealthIndicator> type) {
		return context -> {
			Map<String, ReactiveHealthIndicator> beans = context
					.getBeansOfType(ReactiveHealthIndicator.class);
			assertThat(beans).hasSize(1);
			assertThat(beans.values().iterator().next().getClass()).isEqualTo(type);
		};
	}

}
