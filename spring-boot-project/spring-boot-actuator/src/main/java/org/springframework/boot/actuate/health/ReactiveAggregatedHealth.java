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

import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Mono;

/**
 * Aggregates the overall health of a system by caching {@link Health} per indicator.
 *
 * @author Stephane Nicoll
 * @since 2.2.0
 */
public class ReactiveAggregatedHealth {

	private static final Mono<Health> UNKNOWN_HEALTH = Mono.empty();

	private final ReactiveHealthIndicatorRegistry registry;

	private final Map<String, Mono<Health>> healths;

	/**
	 * Create an instance based on the specified {@link ReactiveHealthIndicatorRegistry}.
	 * @param registry the registry to use to retrieve an indicator by name
	 */
	public ReactiveAggregatedHealth(ReactiveHealthIndicatorRegistry registry) {
		this.registry = registry;
		this.healths = new HashMap<>();
	}

	/**
	 * Return the {@link Health} of the indicator with the specified {@code name} for this
	 * instance or {@link Mono#empty()} if no such indicator exists. When calling this
	 * method several times for a given indicator, the same {@link Health} instance is
	 * returned.
	 * @param name the name of a {@link HealthIndicator}
	 * @return a cached {@link Mono} that provides the {@link Health} of the indicator
	 * with the specified name
	 */
	public Mono<Health> health(String name) {
		return this.healths.computeIfAbsent(name, (indicator) -> determineHealth(name).cache());
	}

	private Mono<Health> determineHealth(String name) {
		ReactiveHealthIndicator healthIndicator = this.registry.get(name);
		if (healthIndicator == null) {
			return UNKNOWN_HEALTH;
		}
		if (healthIndicator instanceof AggregatedReactiveHealthIndicator) {
			return ((AggregatedReactiveHealthIndicator) healthIndicator).health(this);
		}
		return healthIndicator.health();
	}

}
