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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * A overall {@link ReactiveHealthIndicator} that creates a {@link Health} based on all
 * the known indicators of a {@link ReactiveHealthIndicatorRegistry}.
 *
 * @author Stephane Nicoll
 * @since 2.2.0
 */
public class OverallReactiveHealthIndicator implements ReactiveHealthIndicator {

	private final HealthAggregator healthAggregator;

	private final ReactiveHealthIndicatorRegistry registry;

	public OverallReactiveHealthIndicator(HealthAggregator healthAggregator, ReactiveHealthIndicatorRegistry registry) {
		this.healthAggregator = healthAggregator;
		this.registry = registry;
	}

	public ReactiveHealthIndicatorRegistry getRegistry() {
		return this.registry;
	}

	@Override
	public Mono<Health> health() {
		ReactiveAggregatedHealth aggregatedHealth = new ReactiveAggregatedHealth(this.registry);
		return Flux.fromIterable(this.registry.getAll().entrySet())
				.flatMap((entry) -> Mono.zip(Mono.just(entry.getKey()), aggregatedHealth.health(entry.getKey())))
				.collectMap(Tuple2::getT1, Tuple2::getT2).map(this.healthAggregator::aggregate);
	}

}
