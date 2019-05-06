/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.health;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link OverallReactiveHealthIndicator}.
 *
 * @author Stephane Nicoll
 */
class OverallReactiveHealthIndicatorTests {

	@Mock
	private ReactiveHealthIndicator one;

	@Mock
	private ReactiveHealthIndicator two;

	private final HealthAggregator healthAggregator = new OrderedHealthAggregator();

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		given(this.one.health())
				.willReturn(Mono.just(Health.up().withDetail("1", "1").build()));
		given(this.two.health())
				.willReturn(Mono.just(Health.up().withDetail("2", "2").build()));
	}

	@Test
	void overallHealthUsesAllIndicatorsFromRegistry() {
		ReactiveHealthIndicatorRegistry registry = new DefaultReactiveHealthIndicatorRegistry(
				Collections.singletonMap("one", this.one));
		registry.register("two", this.two);
		StepVerifier
				.create(new OverallReactiveHealthIndicator(this.healthAggregator,
						registry).health())
				.consumeNextWith((health) -> assertThat(health.getDetails())
						.containsOnlyKeys("one", "two"))
				.verifyComplete();
	}

	@Test
	void overallHealthIsUsedByNestedAggregatedHealthIndicator() {
		AggregatedReactiveHealthIndicator aggregate = mock(
				AggregatedReactiveHealthIndicator.class);
		given(aggregate.health())
				.willThrow(new IllegalStateException("Should not be called"));
		given(aggregate.health(any(ReactiveAggregatedHealth.class)))
				.willReturn(Mono.just(Health.up().build()));
		ReactiveHealthIndicatorRegistry registry = new DefaultReactiveHealthIndicatorRegistry(
				Collections.singletonMap("test", aggregate));
		StepVerifier
				.create(new OverallReactiveHealthIndicator(this.healthAggregator,
						registry).health())
				.consumeNextWith((health) -> assertThat(health.getDetails())
						.containsOnly(entry("test", Health.up().build())))
				.verifyComplete();
		verify(aggregate).health(any(ReactiveAggregatedHealth.class));
	}

	@Test
	void overallHealthSkipUnregisteredIndicators() {
		ReactiveHealthIndicatorRegistry registry = mock(
				ReactiveHealthIndicatorRegistry.class);
		Map<String, ReactiveHealthIndicator> allIndicators = new HashMap<>();
		allIndicators.put("one", this.one);
		allIndicators.put("two", this.two);
		given(registry.getAll()).willReturn(allIndicators);
		given(registry.get("one")).willReturn(this.one);
		StepVerifier
				.create(new OverallReactiveHealthIndicator(this.healthAggregator,
						registry).health())
				.consumeNextWith((health) -> assertThat(health.getDetails()).containsOnly(
						entry("one", Health.up().withDetail("1", "1").build())))
				.verifyComplete();
		verify(this.one).health();
		verifyZeroInteractions(this.two);
	}

	@Test
	void overallHealthUseIdenticalHealthForKnownIndicator() {
		ReactiveHealthIndicator random = mock(ReactiveHealthIndicator.class);
		given(random.health()).willReturn(Mono.just(
				Health.up().withDetail("uuid", UUID.randomUUID().toString()).build()));
		Map<String, ReactiveHealthIndicator> allIndicators = new HashMap<>();
		allIndicators.put("one", this.one);
		allIndicators.put("two", this.two);
		allIndicators.put("random", random);
		ReactiveHealthIndicatorRegistry registry = new DefaultReactiveHealthIndicatorRegistry(
				allIndicators);
		registry.register("group1",
				new GroupReactiveHealthIndicator(this.healthAggregator, registry,
						new LinkedHashSet<>(Arrays.asList("one", "random"))));
		registry.register("group2",
				new GroupReactiveHealthIndicator(this.healthAggregator, registry,
						new LinkedHashSet<>(Arrays.asList("two", "random"))));
		StepVerifier.create(
				new OverallReactiveHealthIndicator(this.healthAggregator, registry)
						.health())
				.consumeNextWith((health) -> {
					Object uuid = ((Health) health.getDetails().get("random"))
							.getDetails().get("uuid");
					assertThat(health.getDetails()).containsOnly(
							entry("one", Health.up().withDetail("1", "1").build()),
							entry("two", Health.up().withDetail("2", "2").build()),
							entry("random", Health.up().withDetail("uuid", uuid).build()),
							entry("group1", Health.up()
									.withDetail("one",
											Health.up().withDetail("1", "1").build())
									.withDetail("random",
											Health.up().withDetail("uuid", uuid).build())
									.build()),
							entry("group2", Health.up()
									.withDetail("two",
											Health.up().withDetail("2", "2").build())
									.withDetail("random",
											Health.up().withDetail("uuid", uuid).build())
									.build()));
				}).verifyComplete();
		verify(random).health();
		verify(this.one).health();
		verify(this.two).health();
	}

}
