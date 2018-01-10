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

package org.springframework.boot.actuate.endpoint.web.servlet.annotation;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.convert.ConversionServiceParameterMapper;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Integration tests for {@link WebMvcEndpointRequestMappingHandlerMapping}.
 *
 * @author Phillip Webb
 */
public class WebMvcEndpointRequestMappingHandlerMappingIntegrationTests {

	public WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
			AnnotationConfigServletWebServerApplicationContext::new)
					.withUserConfiguration(EndpointConfiguration.class,
							ExampleMvcEndpoint.class);

	@Test
	public void test() {
		this.contextRunner.run((context) -> {
			context.getBean(TomcatServletWebServerFactory.class);
		});
	}

	@Configuration
	@EnableWebMvc
	static class EndpointConfiguration {

		@Bean
		public TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean
		public EndpointMediaTypes endpointMediaTypes() {
			List<String> mediaTypes = Arrays.asList("application/vnd.test+json",
					"application/json");
			return new EndpointMediaTypes(mediaTypes, mediaTypes);
		}

		@Bean
		public WebAnnotationEndpointDiscoverer webEndpointDiscoverer(
				ApplicationContext applicationContext) {
			ParameterMapper parameterMapper = new ConversionServiceParameterMapper(
					DefaultConversionService.getSharedInstance());
			return new WebAnnotationEndpointDiscoverer(applicationContext,
					parameterMapper, endpointMediaTypes(),
					EndpointPathResolver.useEndpointId(), null, null);
		}

		@Bean
		public PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
			return new PropertyPlaceholderConfigurer();
		}

		@Bean
		public WebMvcEndpointRequestMappingHandlerMapping webEndpointHandlerMapping(
				Environment environment,
				EndpointDiscoverer<WebOperation> webEndpointDiscoverer) {
			return new WebMvcEndpointRequestMappingHandlerMapping(
					new EndpointMapping("actuator"), EndpointPathResolver.useEndpointId(),
					webEndpointDiscoverer.discoverEndpoints());
		}

	}

	@WebMvcEndpoint(id = "example")
	@ResponseBody
	public static class ExampleMvcEndpoint {

		@GetMapping("foo")
		public String foo() {
			return "Foo";
		}

	}

}
