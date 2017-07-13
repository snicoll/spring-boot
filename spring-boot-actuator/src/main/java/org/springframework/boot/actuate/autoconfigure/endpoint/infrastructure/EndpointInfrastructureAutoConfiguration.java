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

package org.springframework.boot.actuate.autoconfigure.endpoint.infrastructure;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.mvc.ActuatorMediaTypes;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.endpoint.jmx.JmxAnnotationEndpointDiscoverer;
import org.springframework.boot.endpoint.web.WebAnnotationEndpointDiscoverer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.jmx.export.MBeanExporter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the endpoint infrastructure used
 * by the Actuator.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
public class EndpointInfrastructureAutoConfiguration {

	@Bean
	public JmxAnnotationEndpointDiscoverer jmxEndpointDiscoverer(
			ApplicationContext applicationContext) {
		return new JmxAnnotationEndpointDiscoverer(applicationContext,
				DefaultConversionService.getSharedInstance());
	}

	@ConditionalOnSingleCandidate(MBeanExporter.class)
	@Bean
	public JmxEndpointExporter jmxMBeanExporter(MBeanExporter mBeanExporter,
			JmxAnnotationEndpointDiscoverer endpointDiscoverer,
			ObjectProvider<ObjectMapper> objectMapper) {
		return new JmxEndpointExporter(mBeanExporter, endpointDiscoverer,
				objectMapper.getIfAvailable(ObjectMapper::new));
	}

	@ConditionalOnWebApplication
	@Import(ManagementContextConfigurationImportSelector.class)
	static class WebInfrastructureConfiguration {

		@Bean
		public WebAnnotationEndpointDiscoverer webEndpointDiscoverer(
				ApplicationContext applicationContext) {
			List<String> mediaTypes = Arrays.asList("application/json",
					ActuatorMediaTypes.APPLICATION_ACTUATOR_V2_JSON_VALUE);
			return new WebAnnotationEndpointDiscoverer(applicationContext,
					DefaultConversionService.getSharedInstance(), "application",
					mediaTypes, mediaTypes);
		}

	}

}
