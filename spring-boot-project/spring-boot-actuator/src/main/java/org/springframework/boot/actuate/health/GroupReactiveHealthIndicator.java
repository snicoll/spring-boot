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

import java.util.Set;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * A {@link AggregatedReactiveHealthIndicator} that groups existing indicators together.
 *
 * @author Stephane Nicoll
 * @since 2.2.0
 */
public class GroupReactiveHealthIndicator implements AggregatedReactiveHealthIndicator {

	private final HealthAggregator aggregator;

	private final ReactiveHealthIndicatorRegistry registry;

	private final Set<String> indicatorNames;

	/**
	 * Create a group using the specified {@link HealthAggregator} and indicators.
	 * @param aggregator the health aggregator to use
	 * @param registry the registry
	 * @param indicatorNames the names of the health indicators to include in the group
	 */
	public GroupReactiveHealthIndicator(HealthAggregator aggregator, ReactiveHealthIndicatorRegistry registry,
			Set<String> indicatorNames) {
		this.aggregator = aggregator;
		this.registry = registry;
		this.indicatorNames = indicatorNames;
	}

	@Override
	public Mono<Health> health() {
		ReactiveAggregatedHealth aggregatedHealth = new ReactiveAggregatedHealth(this.registry);
		return health(aggregatedHealth);
	}

	@Override
	public Mono<Health> health(ReactiveAggregatedHealth aggregatedHealth) {
		return Flux.fromIterable(this.indicatorNames)
				.flatMap((name) -> Mono.zip(Mono.just(name), aggregatedHealth.health(name)))
				.collectMap(Tuple2::getT1, Tuple2::getT2).map(this.aggregator::aggregate);
	}

}
