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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * An overall {@link HealthIndicator} that creates a {@link Health} based on all the known
 * indicators of a {@link HealthIndicatorRegistry}.
 *
 * @author Stephane Nicoll
 * @since 2.2.0
 */
public final class OverallHealthIndicator implements HealthIndicator {

	private final HealthAggregator aggregator;

	private final HealthIndicatorRegistry registry;

	/**
	 * Create a new instance with the specified {@link HealthAggregator} and
	 * {@link HealthIndicatorRegistry}.
	 * @param aggregator the health aggregator to compute the overall status
	 * @param registry the registry to use to identify the indicators to invoke
	 */
	public OverallHealthIndicator(HealthAggregator aggregator, HealthIndicatorRegistry registry) {
		this.aggregator = aggregator;
		this.registry = registry;
	}

	HealthIndicatorRegistry getRegistry() {
		return this.registry;
	}

	@Override
	public Health health() {
		AggregatedHealth aggregatedHealth = new AggregatedHealth(this.registry);
		Map<String, Health> allIndicators = new LinkedHashMap<>();
		this.registry.getAll().keySet().forEach((name) -> {
			Health health = aggregatedHealth.health(name);
			if (health != null) {
				allIndicators.put(name, health);
			}
		});
		return this.aggregator.aggregate(allIndicators);
	}

}
