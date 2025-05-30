/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.webflux.actuate.autoconfigure.health;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.webflux.actuate.endpoint.web.AdditionalHealthEndpointPathsWebFluxHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebFluxHealthEndpointExtensionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
class WebFluxHealthEndpointExtensionAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withUserConfiguration(HealthIndicatorsConfiguration.class)
		.withConfiguration(AutoConfigurations.of(HealthContributorAutoConfiguration.class,
				HealthEndpointAutoConfiguration.class, WebFluxHealthEndpointExtensionAutoConfiguration.class));

	@Test
	void additionalReactiveHealthEndpointsPathsTolerateHealthEndpointThatIsNotWebExposed() {
		this.contextRunner
			.withConfiguration(
					AutoConfigurations.of(EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class))
			.withPropertyValues("management.endpoints.web.exposure.exclude=*",
					"management.endpoints.cloudfoundry.exposure.include=*", "spring.main.cloud-platform=cloud_foundry")
			.run((context) -> assertThat(context).hasNotFailed()
				.hasSingleBean(AdditionalHealthEndpointPathsWebFluxHandlerMapping.class));
	}

	@Configuration(proxyBeanMethods = false)
	static class HealthIndicatorsConfiguration {

		@Bean
		ReactiveHealthIndicator simpleHealthIndicator() {
			return () -> Mono.just(Health.up().build());
		}

		@Bean
		HealthIndicator additionalHealthIndicator() {
			return () -> Health.up().build();
		}

	}

}
