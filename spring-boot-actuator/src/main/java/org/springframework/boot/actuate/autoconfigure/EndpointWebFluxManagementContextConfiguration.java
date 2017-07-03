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
import java.util.List;
import java.util.Set;

import reactor.core.publisher.Flux;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.ReactiveHealthEndpoint;
import org.springframework.boot.actuate.endpoint.webflux.HealthWebFluxEndpoint;
import org.springframework.boot.actuate.endpoint.webflux.ReactiveEndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.webflux.ReactiveEndpointHandlerMappingCustomizer;
import org.springframework.boot.actuate.endpoint.webflux.WebFluxEndpoint;
import org.springframework.boot.actuate.endpoint.webflux.WebFluxEndpoints;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * Configuration to expose {@link Endpoint} instances over Spring WebFlux.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@ManagementContextConfiguration
@ConditionalOnClass({ Flux.class, WebFluxConfigurer.class })
@EnableConfigurationProperties(HealthMvcEndpointProperties.class)
public class EndpointWebFluxManagementContextConfiguration {

	private final HealthMvcEndpointProperties healthMvcEndpointProperties;

	private final ManagementServerProperties managementServerProperties;

	private final List<ReactiveEndpointHandlerMappingCustomizer> mappingCustomizers;

	public EndpointWebFluxManagementContextConfiguration(
			HealthMvcEndpointProperties healthMvcEndpointProperties,
			ManagementServerProperties managementServerProperties,
			ObjectProvider<List<ReactiveEndpointHandlerMappingCustomizer>> mappingCustomizers) {
		this.healthMvcEndpointProperties = healthMvcEndpointProperties;
		this.managementServerProperties = managementServerProperties;
		List<ReactiveEndpointHandlerMappingCustomizer> providedCustomizers = mappingCustomizers
				.getIfAvailable();
		this.mappingCustomizers = providedCustomizers == null
				? Collections.emptyList()
				: providedCustomizers;
	}

	@Bean
	@ConditionalOnMissingBean
	public ReactiveEndpointHandlerMapping webFluxEndpointHandlerMapping() {
		Set<WebFluxEndpoint> endpoints = webFluxEndpoints().getEndpoints();
		ReactiveEndpointHandlerMapping mapping = new ReactiveEndpointHandlerMapping(endpoints);
		mapping.setPrefix(this.managementServerProperties.getContextPath());
		mapping.setOrder(-2);
		for (ReactiveEndpointHandlerMappingCustomizer customizer : this.mappingCustomizers) {
			customizer.customize(mapping);
		}
		return mapping;
	}

	@Bean
	@ConditionalOnMissingBean
	public WebFluxEndpoints webFluxEndpoints() {
		return new WebFluxEndpoints();
	}

	@Bean
	@ConditionalOnBean(ReactiveHealthEndpoint.class)
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint("health")
	public HealthWebFluxEndpoint healthWebFluxEndpoint(
			ReactiveHealthEndpoint delegate) {
		return new HealthWebFluxEndpoint(delegate);
	}

}
