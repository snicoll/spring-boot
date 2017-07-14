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
import java.util.List;
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
import org.springframework.context.annotation.Import;
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
		load(TestEndpointConfiguration.class, client -> {
			client.get().uri("/test").accept(MediaType.APPLICATION_JSON).exchange()
					.expectStatus().isOk().expectBody(String.class).isEqualTo("All");
		});
	}

	@Test
	public void readOperationWithSelector() {
		load(TestEndpointConfiguration.class, client -> {
			client.get().uri("/test/one").accept(MediaType.APPLICATION_JSON).exchange()
					.expectStatus().isOk().expectBody(String.class).isEqualTo("Part one");
		});
	}

	@Test
	public void readOperationWithSingleQueryParameters() {
		load(QueryEndpointConfiguration.class, client -> {
			client.get().uri("/query?one=1&two=2").accept(MediaType.APPLICATION_JSON)
					.exchange().expectStatus().isOk().expectBody(String.class)
					.isEqualTo("Query 1 2");
		});
	}

	@Test
	public void readOperationWithSingleQueryParametersAndMultipleValues() {
		load(QueryEndpointConfiguration.class, client -> {
			client.get().uri("/query?one=1&two=2&two=2")
					.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
					.expectBody(String.class).isEqualTo("Query 1 2,2");
		});
	}

	@Test
	public void readOperationWithListQueryParameterAndSingleValue() {
		load(QueryWithListEndpointConfiguration.class, client -> {
			client.get().uri("/query?one=1&two=2").accept(MediaType.APPLICATION_JSON)
					.exchange().expectStatus().isOk().expectBody(String.class)
					.isEqualTo("Query 1 [2]");
		});
	}

	@Test
	public void readOperationWithListQueryParameterAndMultipleValues() {
		load(QueryWithListEndpointConfiguration.class, client -> {
			client.get().uri("/query?one=1&two=2&two=2")
					.accept(MediaType.APPLICATION_JSON).exchange().expectStatus().isOk()
					.expectBody(String.class).isEqualTo("Query 1 [2, 2]");
		});
	}

	@Test
	public void writeOperation() {
		load(TestEndpointConfiguration.class, client -> {
			Map<String, Object> body = new HashMap<>();
			body.put("foo", "one");
			body.put("bar", "two");
			client.post().uri("/test").syncBody(body).accept(MediaType.APPLICATION_JSON)
					.exchange().expectStatus().isNoContent().expectBody().isEmpty();
		});
	}

	@Test
	public void writeOperationWithEmptyBody() {
		load(EmptyWriteEndpointConfiguration.class, (context, client) -> {
			client.post().uri("/emptywrite").accept(MediaType.APPLICATION_JSON).exchange()
					.expectStatus().isNoContent().expectBody().isEmpty();
			verify(context.getBean(EndpointDelegate.class)).write();
		});
	}

	@Test
	public void nullIsPassedToTheOperationWhenArgumentIsNotFoundInPostRequestBody() {
		load(TestEndpointConfiguration.class, (context, client) -> {
			Map<String, Object> body = new HashMap<>();
			body.put("foo", "one");
			client.post().uri("/test").syncBody(body).accept(MediaType.APPLICATION_JSON)
					.exchange().expectStatus().isNoContent().expectBody().isEmpty();
			verify(context.getBean(EndpointDelegate.class)).write("one", null);
		});
	}

	@Test
	public void nullsArePassedToTheOperationWhenPostRequestHasNoBody() {
		load(TestEndpointConfiguration.class, (context, client) -> {
			client.post().uri("/test").contentType(MediaType.APPLICATION_JSON)
					.accept(MediaType.APPLICATION_JSON).exchange().expectStatus()
					.isNoContent().expectBody().isEmpty();
			verify(context.getBean(EndpointDelegate.class)).write(null, null);
		});
	}

	@Test
	public void nullResponseFromOperationResultsInNoContentResponseStatus() {
		load(NullResponseEndpointConfiguration.class, (context, client) -> {
			client.get().uri("/null").accept(MediaType.APPLICATION_JSON).exchange()
					.expectStatus().isNoContent().expectBody().isEmpty();
		});
	}

	protected abstract T createApplicationContext(Class<?>... config);

	protected abstract int getPort(T context);

	private void load(Class<?> configuration,
			BiConsumer<ApplicationContext, WebTestClient> consumer) {
		T context = createApplicationContext(configuration, this.exporterConfiguration);
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

	private void load(Class<?> configuration, Consumer<WebTestClient> clientConsumer) {
		load(configuration, (context, client) -> {
			clientConsumer.accept(client);
		});
	}

	@Configuration
	static class BaseConfiguration {

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

	@Configuration
	@Import(BaseConfiguration.class)
	static class TestEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpoint(EndpointDelegate endpointDelegate) {
			return new TestEndpoint(endpointDelegate);
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class QueryEndpointConfiguration {

		@Bean
		public QueryEndpoint queryEndpoint() {
			return new QueryEndpoint();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class QueryWithListEndpointConfiguration {

		@Bean
		public QueryWithListEndpoint queryEndpoint() {
			return new QueryWithListEndpoint();
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class EmptyWriteEndpointConfiguration {

		@Bean
		public EmptyWriteEndpoint emptyWriteEndpoint(EndpointDelegate delegate) {
			return new EmptyWriteEndpoint(delegate);
		}

	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class NullResponseEndpointConfiguration {

		@Bean
		public NullResponseEndpoint nullResponseEndpoint() {
			return new NullResponseEndpoint();
		}

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		private final EndpointDelegate endpointDelegate;

		TestEndpoint(EndpointDelegate endpointDelegate) {
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

	@Endpoint(id = "query")
	static class QueryEndpoint {

		@ReadOperation
		public String query(String one, String two) {
			return "Query " + one + " " + two;
		}

		@ReadOperation
		public String queryWithParameterList(@Selector String list, String one,
				List<String> two) {
			return "Query " + list + " " + one + " " + two;
		}

	}

	@Endpoint(id = "query")
	static class QueryWithListEndpoint {

		@ReadOperation
		public String queryWithParameterList(String one, List<String> two) {
			return "Query " + one + " " + two;
		}

	}

	@Endpoint(id = "emptywrite")
	static class EmptyWriteEndpoint {

		private final EndpointDelegate delegate;

		EmptyWriteEndpoint(EndpointDelegate delegate) {
			this.delegate = delegate;
		}

		@WriteOperation
		public void write() {
			this.delegate.write();
		}

	}

	@Endpoint(id = "null")
	static class NullResponseEndpoint {

		@ReadOperation
		public String readReturningNull() {
			return null;
		}

	}

	public interface EndpointDelegate {

		void write();

		void write(String foo, String bar);

	}

}
