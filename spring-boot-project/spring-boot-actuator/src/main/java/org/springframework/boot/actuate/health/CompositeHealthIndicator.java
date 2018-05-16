/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.health;

import java.util.Map;

/**
 * {@link HealthIndicator} that returns health indications from all registered delegates.
 *
 * @author Tyler J. Frederick
 * @author Phillip Webb
 * @author Christian Dupuis
 * @since 1.1.0
 */
public class CompositeHealthIndicator
		extends DefaultHealthIndicatorRegistry implements HealthIndicator {

	/**
	 * Create a new {@link CompositeHealthIndicator}.
	 * @param healthAggregator the health aggregator
	 */
	public CompositeHealthIndicator(HealthAggregator healthAggregator) {
		super(healthAggregator);
	}

	/**
	 * Create a new {@link CompositeHealthIndicator} from the specified indicators.
	 * @param healthAggregator the health aggregator
	 * @param indicators a map of {@link HealthIndicator}s with the key being used as an
	 * indicator name.
	 */
	public CompositeHealthIndicator(HealthAggregator healthAggregator,
			Map<String, HealthIndicator> indicators) {
		super(healthAggregator, indicators);
	}

	/**
	 * Registers the given {@code healthIndicator}, associating it with the given
	 * {@code name}.
	 * @param name the name of the indicator
	 * @param indicator the indicator
	 * @throws IllegalStateException if an indicator with the given {@code name} is
	 * already registered.
	 * @deprecated as of 2.1.0 in favour of {@link #register(String, HealthIndicator)}
	 */
	@Deprecated
	public void addHealthIndicator(String name, HealthIndicator indicator) {
		register(name, indicator);
	}

}
