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
import java.util.LinkedHashSet;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link GroupReactiveHealthIndicator}.
 *
 * @author Stephane Nicoll
 */
class GroupReactiveHealthIndicatorTests {

	private final HealthAggregator aggregator = new OrderedHealthAggregator();

	private final ReactiveHealthIndicatorRegistry registry = new DefaultReactiveHealthIndicatorRegistry();

	@Test
	void groupHealthInvokesIndicatorsByName() {
		ReactiveAggregatedHealth aggregatedHealth = mock(ReactiveAggregatedHealth.class);
		given(aggregatedHealth.health("one")).willReturn(Mono.just(Health.up().build()));
		given(aggregatedHealth.health("two")).willReturn(Mono.just(Health.up().build()));
		StepVerifier
				.create(createGroupReactiveHealthIndicator("one", "two")
						.health(aggregatedHealth))
				.consumeNextWith(
						(h) -> assertThat(h.getDetails()).containsOnlyKeys("one", "two"))
				.verifyComplete();
		verify(aggregatedHealth).health("one");
		verify(aggregatedHealth).health("two");
		verifyZeroInteractions(aggregatedHealth);
	}

	@Test
	void groupHealthSkipDisabledIndicator() {
		ReactiveAggregatedHealth aggregatedHealth = mock(ReactiveAggregatedHealth.class);
		given(aggregatedHealth.health("one")).willReturn(Mono.just(Health.up().build()));
		given(aggregatedHealth.health("two")).willReturn(Mono.empty());
		given(aggregatedHealth.health("three"))
				.willReturn(Mono.just(Health.up().build()));
		StepVerifier
				.create(createGroupReactiveHealthIndicator("one", "two")
						.health(aggregatedHealth))
				.consumeNextWith(
						(h) -> assertThat(h.getDetails()).containsOnlyKeys("one"))
				.verifyComplete();
		verify(aggregatedHealth).health("one");
		verify(aggregatedHealth).health("two");
		verifyZeroInteractions(aggregatedHealth);
	}

	private GroupReactiveHealthIndicator createGroupReactiveHealthIndicator(
			String... indicatorNames) {
		return new GroupReactiveHealthIndicator(this.aggregator, this.registry,
				new LinkedHashSet<>(Arrays.asList(indicatorNames)));
	}

}
