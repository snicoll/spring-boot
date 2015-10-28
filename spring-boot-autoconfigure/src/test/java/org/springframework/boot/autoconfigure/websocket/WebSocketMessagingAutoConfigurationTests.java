/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.websocket;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.test.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

/**
 * Tests for {@link WebSocketMessagingAutoConfiguration}.
 *
 * @author Andy Wilkinson
 */
public class WebSocketMessagingAutoConfigurationTests {

	private AnnotationConfigEmbeddedWebApplicationContext context = new AnnotationConfigEmbeddedWebApplicationContext();

	private SockJsClient sockJsClient;

	@Before
	public void setup() {
		List<Transport> transports = Arrays.asList(
				new WebSocketTransport(new StandardWebSocketClient()),
				new RestTemplateXhrTransport(new RestTemplate()));
		this.sockJsClient = new SockJsClient(transports);
	}

	@After
	public void tearDown() {
		this.context.close();
		this.sockJsClient.stop();
	}

	@Test
	public void basicMessagingWithJson() throws Throwable {

	}

	@Configuration
	@EnableWebSocket
	@EnableConfigurationProperties
	@EnableWebSocketMessageBroker
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class,
			EmbeddedServletContainerAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			WebSocketMessagingAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class })
	static class WebSocketMessagingConfiguration
			extends AbstractWebSocketMessageBrokerConfigurer {

		@Override
		public void registerStompEndpoints(StompEndpointRegistry registry) {
			registry.addEndpoint("/messaging").withSockJS();
		}

		@Override
		public void configureMessageBroker(MessageBrokerRegistry registry) {
			registry.setApplicationDestinationPrefixes("/app");
		}

		@Bean
		public MessagingController messagingController() {
			return new MessagingController();
		}

		@Bean
		public TomcatEmbeddedServletContainerFactory tomcat() {
			return new TomcatEmbeddedServletContainerFactory(0);
		}

		@Bean
		public TomcatWebSocketContainerCustomizer tomcatCuztomiser() {
			return new TomcatWebSocketContainerCustomizer();
		}

	}

	@Controller
	static class MessagingController {

		@SubscribeMapping("/data")
		Data getData() {
			return new Data(5, "baz");
		}

	}

	static class Data {

		private int foo;

		private String bar;

		Data(int foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}

		public int getFoo() {
			return this.foo;
		}

		public String getBar() {
			return this.bar;
		}

	}

}
