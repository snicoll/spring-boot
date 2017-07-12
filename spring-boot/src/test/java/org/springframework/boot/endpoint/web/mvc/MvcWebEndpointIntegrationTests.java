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

package org.springframework.boot.endpoint.web.mvc;

import org.springframework.boot.endpoint.web.AbstractWebEndpointIntegrationTests;
import org.springframework.boot.endpoint.web.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Integration tests for web endpoints exposed using Spring MVC.
 *
 * @author Andy Wilkinson
 */
public class MvcWebEndpointIntegrationTests extends
		AbstractWebEndpointIntegrationTests<AnnotationConfigServletWebServerApplicationContext> {

	public MvcWebEndpointIntegrationTests() {
		super(WebMvcConfiguration.class);
	}

	@Override
	protected AnnotationConfigServletWebServerApplicationContext createApplicationContext(
			Class<?>... config) {
		return new AnnotationConfigServletWebServerApplicationContext(config);
	}

	@Override
	protected int getPort(AnnotationConfigServletWebServerApplicationContext context) {
		return context.getWebServer().getPort();
	}

	@Configuration
	@EnableWebMvc
	static class WebMvcConfiguration {

		@Bean
		public TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean
		public WebEndpointHandlerMapping webEndpointHandlerMapping(
				WebAnnotationEndpointDiscoverer webEndpointDiscoverer) {
			return new WebEndpointHandlerMapping(
					webEndpointDiscoverer.discoverEndpoints());
		}

	}

}
