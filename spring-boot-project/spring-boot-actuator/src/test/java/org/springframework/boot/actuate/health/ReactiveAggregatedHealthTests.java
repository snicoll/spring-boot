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

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ReactiveAggregatedHealth}.
 *
 * @author Stephane Nicoll
 */
class ReactiveAggregatedHealthTests {

	@Mock
	private ReactiveHealthIndicator one;

	private ReactiveHealthIndicatorRegistry registry;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		given(this.one.health()).willReturn(Mono.just(new Health.Builder().up().withDetail("1", "1").build()));
		this.registry = new DefaultReactiveHealthIndicatorRegistry(Collections.singletonMap("one", this.one));
	}

	@Test
	void healthForKnownIndicator() {
		ReactiveAggregatedHealth aggregatedHealth = new ReactiveAggregatedHealth(this.registry);
		StepVerifier.create(aggregatedHealth.health("one")).consumeNextWith((h) -> {
			assertThat(h.getStatus()).isEqualTo(Status.UP);
			assertThat(h.getDetails()).containsOnly(entry("1", "1"));
		}).verifyComplete();
	}

	@Test
	void healthForKnownIndicatorInvokesTargetHealthIndicatorOnce() {
		ReactiveAggregatedHealth aggregatedHealth = new ReactiveAggregatedHealth(this.registry);
		Health first = aggregatedHealth.health("one").block();
		Health second = aggregatedHealth.health("one").block();
		assertThat(first).isSameAs(second);
		verify(this.one, times(1)).health();
	}

	@Test
	void healthForKnownAggregatedHealthIndicatorUsesAggregatedHealth() {
		ReactiveAggregatedHealth aggregatedHealth = new ReactiveAggregatedHealth(this.registry);
		AggregatedReactiveHealthIndicator two = mock(AggregatedReactiveHealthIndicator.class);
		given(two.health(aggregatedHealth)).willReturn(Mono.just(Health.up().withDetail("2", "2").build()));
		this.registry.register("two", two);
		assertThat(aggregatedHealth.health("two").block())
				.isEqualTo(new Health.Builder().up().withDetail("2", "2").build());
		verify(two, times(1)).health(aggregatedHealth);
	}

	@Test
	void healthForUnknownIndicator() {
		ReactiveAggregatedHealth aggregatedHealth = new ReactiveAggregatedHealth(this.registry);
		StepVerifier.create(aggregatedHealth.health("unknown")).verifyComplete();
	}

	@Test
	void healthForUnknownIndicatorIsCached() {
		ReactiveAggregatedHealth aggregatedHealth = new ReactiveAggregatedHealth(this.registry);
		StepVerifier.create(aggregatedHealth.health("unknown")).verifyComplete();
		// Register indicator after it has been requested for this round
		this.registry.register("unknown", () -> Mono.just(Health.unknown().build()));
		StepVerifier.create(aggregatedHealth.health("unknown")).verifyComplete();
	}

}
