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

package org.springframework.boot.endpoint.web;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;

import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.ReadOperation;
import org.springframework.boot.endpoint.Selector;
import org.springframework.boot.endpoint.WriteOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Abstract base class for web endpoint integration tests.
 *
 * @param <T> the type of application context used by the tests
 * @author Andy Wilkinson
 */
public abstract class AbstractWebEndpointIntegrationTests<T extends ConfigurableApplicationContext> {

	private final Class<?> exporterConfiguration;

	protected AbstractWebEndpointIntegrationTests(Class<?> exporterConfiguration) {
		this.exporterConfiguration = exporterConfiguration;
	}

	@Test
	public void readOperation() {
		load(client -> {
			client.get().uri("/test").accept(MediaType.APPLICATION_JSON).exchange()
					.expectStatus().isOk().expectBody(String.class).isEqualTo("All");
		});
	}

	@Test
	public void readOperationWithSelector() {
		load(client -> {
			client.get().uri("/test/one").accept(MediaType.APPLICATION_JSON).exchange()
					.expectStatus().isOk().expectBody(String.class).isEqualTo("Part one");
		});
	}

	@Test
	public void writeOperation() {
		load(client -> {
			Map<String, Object> body = new HashMap<>();
			body.put("foo", "one");
			body.put("bar", "two");
			client.post().uri("/test").syncBody(body).accept(MediaType.APPLICATION_JSON)
					.exchange().expectStatus().isOk().expectBody().isEmpty();
		});
	}

	@Test
	public void nullIsPassedToTheOperationWhenArgumentIsNotFoundInTheRequestBody() {
		load((context, client) -> {
			Map<String, Object> body = new HashMap<>();
			body.put("foo", "one");
			client.post().uri("/test").syncBody(body).accept(MediaType.APPLICATION_JSON)
					.exchange().expectStatus().isOk().expectBody().isEmpty();
			verify(context.getBean(EndpointDelegate.class)).write("one", null);
		});
	}

	protected abstract T createApplicationContext(Class<?>... config);

	protected abstract int getPort(T context);

	private void load(BiConsumer<ApplicationContext, WebTestClient> consumer) {
		T context = createApplicationContext(BaseConfiguration.class,
				this.exporterConfiguration);
		try {
			consumer.accept(context,
					WebTestClient.bindToServer()
							.baseUrl(
									"http://localhost:" + getPort(context) + "/endpoints")
							.build());
		}
		finally {
			context.close();
		}
	}

	private void load(Consumer<WebTestClient> clientConsumer) {
		load((context, client) -> {
			clientConsumer.accept(client);
		});
	}

	@Configuration
	static class BaseConfiguration {

		@Bean
		public TestWebEndpoint testWebEndpoint(EndpointDelegate endpointDelegate) {
			return new TestWebEndpoint(endpointDelegate);
		}

		@Bean
		public EndpointDelegate endpointDelegate() {
			return mock(EndpointDelegate.class);
		}

		@Bean
		public WebAnnotationEndpointDiscoverer webEndpointDiscoverer(
				ApplicationContext applicationContext) {
			return new WebAnnotationEndpointDiscoverer(applicationContext,
					DefaultConversionService.getSharedInstance(), "endpoints",
					Arrays.asList("application/json"), Arrays.asList("application/json"));
		}

	}

	@Endpoint(id = "test")
	static class TestWebEndpoint {

		private final EndpointDelegate endpointDelegate;

		TestWebEndpoint(EndpointDelegate endpointDelegate) {
			this.endpointDelegate = endpointDelegate;
		}

		@ReadOperation
		public String readAll() {
			return "All";
		}

		@ReadOperation
		public String readPart(@Selector String part) {
			return "Part " + part;
		}

		@WriteOperation
		public void write(String foo, String bar) {
			this.endpointDelegate.write(foo, bar);
		}

	}

	public interface EndpointDelegate {

		void write(String foo, String bar);

	}

}
