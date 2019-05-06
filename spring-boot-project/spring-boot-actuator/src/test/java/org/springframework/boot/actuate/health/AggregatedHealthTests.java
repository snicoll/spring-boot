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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link AggregatedHealth}.
 *
 * @author Stephane Nicoll
 */
class AggregatedHealthTests {

	@Mock
	private HealthIndicator one;

	private HealthIndicatorRegistry registry;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		given(this.one.health()).willReturn(new Health.Builder().up().withDetail("1", "1").build());
		this.registry = new DefaultHealthIndicatorRegistry(Collections.singletonMap("one", this.one));
	}

	@Test
	void healthForKnownIndicator() {
		AggregatedHealth aggregatedHealth = new AggregatedHealth(this.registry);
		assertThat(aggregatedHealth.health("one")).isEqualTo(new Health.Builder().up().withDetail("1", "1").build());
	}

	@Test
	void healthForKnownIndicatorInvokesTargetHealthIndicatorOnce() {
		AggregatedHealth aggregatedHealth = new AggregatedHealth(this.registry);
		Health first = aggregatedHealth.health("one");
		Health second = aggregatedHealth.health("one");
		assertThat(first).isSameAs(second);
		verify(this.one, times(1)).health();
	}

	@Test
	void healthForKnownAggregatedHealthIndicatorUsesAggregatedHealth() {
		AggregatedHealth aggregatedHealth = new AggregatedHealth(this.registry);
		AggregatedHealthIndicator two = mock(AggregatedHealthIndicator.class);
		given(two.health(aggregatedHealth)).willReturn(new Health.Builder().down().withDetail("2", "2").build());
		this.registry.register("two", two);
		assertThat(aggregatedHealth.health("two")).isEqualTo(new Health.Builder().down().withDetail("2", "2").build());
		verify(two, times(1)).health(aggregatedHealth);
	}

	@Test
	void healthForUnknownIndicator() {
		AggregatedHealth aggregatedHealth = new AggregatedHealth(this.registry);
		assertThat(aggregatedHealth.health("unknown")).isNull();
	}

	@Test
	void healthForUnknownIndicatorIsCached() {
		AggregatedHealth aggregatedHealth = new AggregatedHealth(this.registry);
		assertThat(aggregatedHealth.health("unknown")).isNull();
		// Register indicator after it has been requested for this round
		this.registry.register("unknown", () -> Health.unknown().build());
		assertThat(aggregatedHealth.health("unknown")).isNull();
	}

}
