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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EndpointProperties;
import org.springframework.boot.actuate.endpoint.ReactiveHealthEndpoint;
import org.springframework.boot.actuate.health.HealthAggregator;
import org.springframework.boot.actuate.health.OrderedHealthAggregator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for common management
 * reactive {@link Endpoint}s.
 *
 * @author Stephane Nicoll
 */
@Configuration
@EnableConfigurationProperties(EndpointProperties.class)
public class ReactiveEndpointAutoConfiguration {

	private final HealthAggregator healthAggregator;

	private final Map<String, ReactiveHealthIndicator> healthIndicators;

	public ReactiveEndpointAutoConfiguration(ObjectProvider<HealthAggregator> healthAggregator,
			ObjectProvider<Map<String, ReactiveHealthIndicator>> healthIndicators) {
		this.healthAggregator = healthAggregator.getIfAvailable();
		this.healthIndicators = healthIndicators.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveHealthEndpoint healthEndpoint() {
		return new ReactiveHealthEndpoint(
				this.healthAggregator == null ? new OrderedHealthAggregator()
						: this.healthAggregator,
				this.healthIndicators == null
						? Collections.emptyMap()
						: this.healthIndicators);
	}

}
