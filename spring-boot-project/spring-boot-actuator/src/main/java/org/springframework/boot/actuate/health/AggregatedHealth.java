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

/**
 * Aggregates the overall health of a system by caching {@link Health} per indicator.
 *
 * @author Stephane Nicoll
 * @since 2.2.0
 */
public class AggregatedHealth {

	private static final Health UNKNOWN_HEALTH = Health.unknown().build();

	private final HealthIndicatorRegistry registry;

	private final Map<String, Health> healths;

	/**
	 * Create an instance based on the specified {@link HealthIndicatorRegistry}.
	 * @param registry the registry to use to retrieve an indicator by name
	 */
	public AggregatedHealth(HealthIndicatorRegistry registry) {
		this.registry = registry;
		this.healths = new HashMap<>();
	}

	/**
	 * Return the {@link Health} of the indicator with the specified {@code name} for this
	 * instance or {@code null} if no such indicator exists. When calling this method
	 * several times for a given indicator, the same {@link Health} instance is returned.
	 * @param name the name of a {@link HealthIndicator}
	 * @return the {@link Health} of the indicator with the specified name
	 */
	public Health health(String name) {
		Health health = this.healths.computeIfAbsent(name, this::determineHealth);
		return (health != UNKNOWN_HEALTH) ? health : null;
	}

	private Health determineHealth(String name) {
		HealthIndicator healthIndicator = this.registry.get(name);
		if (healthIndicator == null) {
			return UNKNOWN_HEALTH;
		}
		if (healthIndicator instanceof AggregatedHealthIndicator) {
			return ((AggregatedHealthIndicator) healthIndicator).health(this);
		}
		return healthIndicator.health();
	}

}
