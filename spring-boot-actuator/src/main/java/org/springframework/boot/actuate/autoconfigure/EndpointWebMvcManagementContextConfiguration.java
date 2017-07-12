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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.condition.ConditionalOnEnabledEndpoint;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.WebEndpointHandlerMappingCustomizer;
import org.springframework.boot.actuate.endpoint.web.HealthWebEndpointExtension;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.endpoint.web.WebEndpointDiscoverer;
import org.springframework.boot.endpoint.web.mvc.WebEndpointHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * Configuration to expose {@link Endpoint} instances over Spring MVC.
 *
 * @author Dave Syer
 * @author Ben Hale
 * @author Vedran Pavic
 * @author Madhura Bhave
 * @since 1.3.0
 */
@ManagementContextConfiguration
@EnableConfigurationProperties({ HealthMvcEndpointProperties.class,
		EndpointCorsProperties.class })
@ConditionalOnClass(DispatcherServlet.class)
public class EndpointWebMvcManagementContextConfiguration {

	private final HealthMvcEndpointProperties healthMvcEndpointProperties;

	private final ManagementServerProperties managementServerProperties;

	private final EndpointCorsProperties corsProperties;

	private final List<WebEndpointHandlerMappingCustomizer> mappingCustomizers;

	public EndpointWebMvcManagementContextConfiguration(
			HealthMvcEndpointProperties healthMvcEndpointProperties,
			ManagementServerProperties managementServerProperties,
			EndpointCorsProperties corsProperties,
			ObjectProvider<List<WebEndpointHandlerMappingCustomizer>> mappingCustomizers) {
		this.healthMvcEndpointProperties = healthMvcEndpointProperties;
		this.managementServerProperties = managementServerProperties;
		this.corsProperties = corsProperties;
		List<WebEndpointHandlerMappingCustomizer> providedCustomizers = mappingCustomizers
				.getIfAvailable();
		this.mappingCustomizers = providedCustomizers == null
				? Collections.<WebEndpointHandlerMappingCustomizer>emptyList()
				: providedCustomizers;
	}

	@Bean
	@ConditionalOnMissingBean
	public WebEndpointHandlerMapping webEndpointHandlerMapping(
			WebEndpointDiscoverer discoverer) {
		WebEndpointHandlerMapping handlerMapping = new WebEndpointHandlerMapping(
				discoverer.discoverEndpoints());
		for (WebEndpointHandlerMappingCustomizer customizer : this.mappingCustomizers) {
			customizer.customize(handlerMapping);
		}
		return handlerMapping;
	}

	private CorsConfiguration getCorsConfiguration(EndpointCorsProperties properties) {
		if (CollectionUtils.isEmpty(properties.getAllowedOrigins())) {
			return null;
		}
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(properties.getAllowedOrigins());
		if (!CollectionUtils.isEmpty(properties.getAllowedHeaders())) {
			configuration.setAllowedHeaders(properties.getAllowedHeaders());
		}
		if (!CollectionUtils.isEmpty(properties.getAllowedMethods())) {
			configuration.setAllowedMethods(properties.getAllowedMethods());
		}
		if (!CollectionUtils.isEmpty(properties.getExposedHeaders())) {
			configuration.setExposedHeaders(properties.getExposedHeaders());
		}
		if (properties.getMaxAge() != null) {
			configuration.setMaxAge(properties.getMaxAge());
		}
		if (properties.getAllowCredentials() != null) {
			configuration.setAllowCredentials(properties.getAllowCredentials());
		}
		return configuration;
	}

	// TODO Port to new infrastructure

	// @Bean
	// @ConditionalOnMissingBean
	// @ConditionalOnEnabledEndpoint("heapdump")
	// public HeapdumpEndpoint heapdumpMvcEndpoint() {
	// return new HeapdumpEndpoint();
	// }

	@Bean
	@ConditionalOnBean(HealthEndpoint.class)
	@ConditionalOnMissingBean
	@ConditionalOnEnabledEndpoint("health")
	public HealthWebEndpointExtension healthWebEndpointExtension(HealthEndpoint delegate,
			ManagementServerProperties managementServerProperties) {
		HealthWebEndpointExtension healthMvcEndpoint = new HealthWebEndpointExtension(
				delegate);
		if (this.healthMvcEndpointProperties.getMapping() != null) {
			healthMvcEndpoint
					.addStatusMapping(this.healthMvcEndpointProperties.getMapping());
		}
		return healthMvcEndpoint;
	}

	// TODO Port to new infrastructure

	// @Bean
	// @ConditionalOnEnabledEndpoint("logfile")
	// @Conditional(LogFileCondition.class)
	// public LogFileMvcEndpoint logfileMvcEndpoint() {
	// return new LogFileMvcEndpoint();
	// }
	//
	// @Bean
	// @ConditionalOnBean(AuditEventRepository.class)
	// @ConditionalOnEnabledEndpoint("auditevents")
	// public AuditEventsMvcEndpoint auditEventMvcEndpoint(
	// AuditEventRepository auditEventRepository) {
	// return new AuditEventsMvcEndpoint(auditEventRepository);
	// }

	private static class LogFileCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			Environment environment = context.getEnvironment();
			String config = environment.resolvePlaceholders("${logging.file:}");
			ConditionMessage.Builder message = ConditionMessage.forCondition("Log File");
			if (StringUtils.hasText(config)) {
				return ConditionOutcome
						.match(message.found("logging.file").items(config));
			}
			config = environment.resolvePlaceholders("${logging.path:}");
			if (StringUtils.hasText(config)) {
				return ConditionOutcome
						.match(message.found("logging.path").items(config));
			}
			config = environment.getProperty("endpoints.logfile.external-file");
			if (StringUtils.hasText(config)) {
				return ConditionOutcome.match(
						message.found("endpoints.logfile.external-file").items(config));
			}
			return ConditionOutcome.noMatch(message.didNotFind("logging file").atAll());
		}

	}

}
