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

package org.springframework.boot.endpoint.web.reactive;

import org.springframework.boot.endpoint.web.AbstractWebEndpointIntegrationTests;
import org.springframework.boot.endpoint.web.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.reactive.context.ReactiveWebServerInitializedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * Integration tests for web endpoints exposed using WebFlux.
 *
 * @author Andy Wilkinson
 */
public class ReactiveWebEndpointIntegrationTests
		extends AbstractWebEndpointIntegrationTests<ReactiveWebServerApplicationContext> {

	public ReactiveWebEndpointIntegrationTests() {
		super(ReactiveConfiguration.class);
	}

	@Override
	protected ReactiveWebServerApplicationContext createApplicationContext(
			Class<?>... config) {
		return new ReactiveWebServerApplicationContext(config);
	}

	@Override
	protected int getPort(ReactiveWebServerApplicationContext context) {
		return context.getBean(ReactiveConfiguration.class).port;
	}

	@Configuration
	@EnableWebFlux
	static class ReactiveConfiguration {

		private int port;

		@Bean
		public NettyReactiveWebServerFactory netty() {
			return new NettyReactiveWebServerFactory(0);
		}

		@Bean
		public HttpHandler httpHandler(ApplicationContext applicationContext) {
			return WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		}

		@Bean
		public RouterFunction<ServerResponse> endpointsRouter(
				WebAnnotationEndpointDiscoverer endpointDiscoverer) {
			return new ReactiveEndpointRouterFunctionFactory()
					.createRouterFunction(endpointDiscoverer.discoverEndpoints());
		}

		@Bean
		public ApplicationListener<ReactiveWebServerInitializedEvent> serverInitializedListener() {
			return event -> {
				this.port = event.getWebServer().getPort();
			};
		}

	}

}
