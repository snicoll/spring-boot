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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * An {@link AggregatedHealthIndicator} that groups existing indicators together.
 *
 * @author Stephane Nicoll
 * @since 2.2.0
 */
public class GroupHealthIndicator implements AggregatedHealthIndicator {

	private final HealthAggregator aggregator;

	private final HealthIndicatorRegistry registry;

	private final Set<String> indicatorNames;

	/**
	 * Create a group using the specified {@link HealthAggregator} and indicators.
	 * @param aggregator the health aggregator to use
	 * @param registry the registry
	 * @param indicatorNames the names of the health indicators to include in the group
	 */
	public GroupHealthIndicator(HealthAggregator aggregator, HealthIndicatorRegistry registry,
			Set<String> indicatorNames) {
		this.aggregator = aggregator;
		this.registry = registry;
		this.indicatorNames = new LinkedHashSet<>(indicatorNames);
	}

	@Override
	public Health health() {
		return health(new AggregatedHealth(this.registry));
	}

	@Override
	public Health health(AggregatedHealth aggregatedHealth) {
		Map<String, Health> healths = new LinkedHashMap<>();
		for (String indicatorName : this.indicatorNames) {
			Health health = aggregatedHealth.health(indicatorName);
			if (health != null) {
				healths.put(indicatorName, health);
			}
		}
		return this.aggregator.aggregate(healths);
	}

}
