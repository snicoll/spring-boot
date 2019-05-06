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

package org.springframework.boot.actuate.autoconfigure.health;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import reactor.core.publisher.Flux;

import org.springframework.boot.actuate.health.ApplicationHealthIndicator;
import org.springframework.boot.actuate.health.GroupHealthIndicator;
import org.springframework.boot.actuate.health.GroupReactiveHealthIndicator;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicatorRegistry;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicatorRegistry;
import org.springframework.boot.actuate.health.ReactiveHealthIndicatorRegistryFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link HealthIndicator}s.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Vedran Pavic
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ HealthIndicatorProperties.class })
public class HealthIndicatorAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean({ HealthIndicator.class, ReactiveHealthIndicator.class })
	public ApplicationHealthIndicator applicationHealthIndicator() {
		return new ApplicationHealthIndicator();
	}

	@Bean
	@ConditionalOnMissingBean(HealthAggregator.class)
	public OrderedHealthAggregator healthAggregator(
			HealthIndicatorProperties properties) {
		OrderedHealthAggregator healthAggregator = new OrderedHealthAggregator();
		if (properties.getStatus().getOrder() != null) {
			healthAggregator.setStatusOrder(properties.getStatus().getOrder());
		}
		return healthAggregator;
	}

	@Bean
	@ConditionalOnMissingBean(HealthIndicatorRegistry.class)
	public HealthIndicatorRegistry healthIndicatorRegistry(
			HealthIndicatorProperties properties, HealthAggregator healthAggregator,
			ApplicationContext applicationContext) {
		HealthIndicatorRegistry registry = HealthIndicatorRegistryBeans
				.get(applicationContext);
		extractGroups(properties, registry::get)
				.forEach((groupName, groupHealthIndicators) -> registry
						.register(groupName, new GroupHealthIndicator(healthAggregator,
								registry, groupHealthIndicators)));
		return registry;
	}

	private static <T> Map<String, Set<String>> extractGroups(
			HealthIndicatorProperties properties,
			Function<String, T> healthIndicatorByName) {
		Map<String, Set<String>> groupDefinitions = new LinkedHashMap<>();
		properties.getGroups().forEach((groupName, indicatorNames) -> {
			if (healthIndicatorByName.apply(groupName) != null) {
				throw new IllegalArgumentException(
						"Could not register health indicator group named '" + groupName
								+ "', an health indicator with that name is already registered");
			}
			Set<String> groupHealthIndicators = new LinkedHashSet<>();
			indicatorNames.forEach((name) -> {
				T healthIndicator = healthIndicatorByName.apply(name);
				if (healthIndicator != null) {
					groupHealthIndicators.add(name);
				}
			});
			groupDefinitions.put(groupName, groupHealthIndicators);
		});
		return groupDefinitions;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(Flux.class)
	static class ReactiveHealthIndicatorConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public ReactiveHealthIndicatorRegistry reactiveHealthIndicatorRegistry(
				HealthIndicatorProperties properties, HealthAggregator healthAggregator,
				Map<String, ReactiveHealthIndicator> reactiveHealthIndicators,
				Map<String, HealthIndicator> healthIndicators) {
			ReactiveHealthIndicatorRegistry registry = new ReactiveHealthIndicatorRegistryFactory()
					.createReactiveHealthIndicatorRegistry(reactiveHealthIndicators,
							healthIndicators);
			extractGroups(properties, registry::get).forEach(
					(groupName, groupHealthIndicators) -> registry.register(groupName,
							new GroupReactiveHealthIndicator(healthAggregator, registry,
									groupHealthIndicators)));
			return registry;
		}

	}

}
