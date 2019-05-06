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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link GroupHealthIndicator}.
 *
 * @author Stephane Nicoll
 */
class GroupHealthIndicatorTests {

	private final HealthAggregator aggregator = new OrderedHealthAggregator();

	private final HealthIndicatorRegistry registry = new DefaultHealthIndicatorRegistry();

	@Test
	void groupHealthInvokesIndicatorsByName() {
		AggregatedHealth aggregatedHealth = mock(AggregatedHealth.class);
		given(aggregatedHealth.health("one")).willReturn(Health.up().build());
		given(aggregatedHealth.health("two")).willReturn(Health.up().build());
		Health health = createGroupHealthIndicator("one", "two").health(aggregatedHealth);
		assertThat(health.getDetails()).containsOnlyKeys("one", "two");
		verify(aggregatedHealth).health("one");
		verify(aggregatedHealth).health("two");
		verifyZeroInteractions(aggregatedHealth);
	}

	@Test
	void groupHealthSkipDisabledIndicator() {
		AggregatedHealth aggregatedHealth = mock(AggregatedHealth.class);
		given(aggregatedHealth.health("one")).willReturn(Health.up().build());
		given(aggregatedHealth.health("two")).willReturn(null);
		given(aggregatedHealth.health("three")).willReturn(Health.up().build());
		Health health = createGroupHealthIndicator("one", "two").health(aggregatedHealth);
		assertThat(health.getDetails()).containsOnlyKeys("one");
		verify(aggregatedHealth).health("one");
		verify(aggregatedHealth).health("two");
		verifyZeroInteractions(aggregatedHealth);
	}

	private GroupHealthIndicator createGroupHealthIndicator(String... indicatorNames) {
		return new GroupHealthIndicator(this.aggregator, this.registry,
				new LinkedHashSet<>(Arrays.asList(indicatorNames)));
	}

}
