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

package org.springframework.boot.endpoint.jmx;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.EndpointDiscoverer;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.ReadOperation;
import org.springframework.boot.endpoint.WriteOperation;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JmxEndpointDiscoverer}.
 *
 * @author Stephane Nicoll
 */
public class JmxEndpointDiscovererTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void discoveryWorksWhenThereAreNoEndpoints() {
		load(EmptyConfiguration.class, discoverer -> {
			assertThat(discoverer.discoverEndpoints()).isEmpty();
		});
	}

	@Test
	public void standardEndpointIsDiscovered() {
		load(TestEndpoint.class, discoverer -> {
			Map<String, EndpointInfo<JmxEndpointOperation>> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys("test");
			Map<String, JmxEndpointOperation> operationByName = mapOperations(
					endpoints.get("test").getOperations());
			assertThat(operationByName).containsOnlyKeys("getAll", "getSomething",
					"update");
			JmxEndpointOperation getAll = operationByName.get("getAll");
			assertThat(getAll.getDescription())
					.isEqualTo("Invoke getAll for endpoint test");
			assertThat(getAll.getOutputType()).isEqualTo(Object.class);
			assertThat(getAll.getParameters()).isEmpty();
			JmxEndpointOperation getSomething = operationByName.get("getSomething");
			assertThat(getSomething.getDescription())
					.isEqualTo("Invoke getSomething for endpoint test");
			assertThat(getSomething.getOutputType()).isEqualTo(String.class);
			assertThat(getSomething.getParameters()).hasSize(1);
			hasDefaultParameter(getSomething, 0, String.class);
			JmxEndpointOperation update = operationByName.get("update");
			assertThat(update.getDescription())
					.isEqualTo("Invoke update for endpoint test");
			assertThat(update.getOutputType()).isEqualTo(Void.TYPE);
			assertThat(update.getParameters()).hasSize(2);
			hasDefaultParameter(update, 0, String.class);
			hasDefaultParameter(update, 1, String.class);
		});

	}

	@Test
	public void jmxOnlyEndpointIsDiscovered() {
		load(TestJmxEndpoint.class, discoverer -> {
			Map<String, EndpointInfo<JmxEndpointOperation>> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys("test");
			assertJmxTestEndpoint(endpoints.get("test"));
		});

	}

	@Test
	public void jmxEndpointOverridesStandardEndpoint() {
		load(OverriddenOperationJmxEndpointConfiguration.class, discoverer -> {
			Map<String, EndpointInfo<JmxEndpointOperation>> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys("test");
			assertJmxTestEndpoint(endpoints.get("test"));
		});

	}

	@Test
	public void jmxEndpointAddsExtraOperation() {
		load(AdditionalOperationJmxEndpointConfiguration.class, discoverer -> {
			Map<String, EndpointInfo<JmxEndpointOperation>> endpoints = discover(discoverer);
			assertThat(endpoints).containsOnlyKeys("test");
			Map<String, JmxEndpointOperation> operationByName = mapOperations(
					endpoints.get("test").getOperations());
			assertThat(operationByName).containsOnlyKeys(
					"getAll", "getSomething", "update", "getAnother");
			JmxEndpointOperation getAll = operationByName.get("getAnother");
			assertThat(getAll.getDescription()).isEqualTo("Get another thing");
			assertThat(getAll.getOutputType()).isEqualTo(Object.class);
			assertThat(getAll.getParameters()).isEmpty();
		});
	}

	@Test
	public void discoveryFailsWhenTwoWebEndpointsHaveTheSameId() {
		load(ClashingJmxEndpointConfiguration.class, discoverer -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Found two endpoints with the id 'test': ");
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void discoveryFailsWhenTwoStandardEndpointsHaveTheSameId() {
		load(ClashingStandardEndpointConfiguration.class, discoverer -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Found two endpoints with the id 'test': ");
			discoverer.discoverEndpoints();
		});
	}

	private void assertJmxTestEndpoint(EndpointInfo<JmxEndpointOperation> endpoint) {
		Map<String, JmxEndpointOperation> operationByName = mapOperations(
				endpoint.getOperations());
		assertThat(operationByName).containsOnlyKeys("getAll", "getSomething", "update");
		JmxEndpointOperation getAll = operationByName.get("getAll");
		assertThat(getAll.getDescription()).isEqualTo("Get all the things");
		assertThat(getAll.getOutputType()).isEqualTo(Object.class);
		assertThat(getAll.getParameters()).isEmpty();
		JmxEndpointOperation getSomething = operationByName.get("getSomething");
		assertThat(getSomething.getDescription())
				.isEqualTo("Get something based on a timeUnit");
		assertThat(getSomething.getOutputType()).isEqualTo(String.class);
		assertThat(getSomething.getParameters()).hasSize(1);
		hasDocumentedParameter(getSomething, 0, "unitMs", Long.class,
				"Number of milliseconds");
		JmxEndpointOperation update = operationByName.get("update");
		assertThat(update.getDescription()).isEqualTo("Update something based on bar");
		assertThat(update.getOutputType()).isEqualTo(Void.TYPE);
		assertThat(update.getParameters()).hasSize(2);
		hasDocumentedParameter(update, 0, "foo", String.class, "Foo identifier");
		hasDocumentedParameter(update, 1, "bar", String.class, "Bar value");
	}

	private void hasDefaultParameter(JmxEndpointOperation operation, int index,
			Class<?> type) {
		assertThat(index).isLessThan(operation.getParameters().size());
		JmxEndpointOperationParameterInfo parameter = operation.getParameters()
				.get(index);
		assertThat(parameter.getType()).isEqualTo(type);
		assertThat(parameter.getDescription()).isNull();
	}

	private void hasDocumentedParameter(JmxEndpointOperation operation, int index,
			String name, Class<?> type, String description) {
		assertThat(index).isLessThan(operation.getParameters().size());
		JmxEndpointOperationParameterInfo parameter = operation.getParameters()
				.get(index);
		assertThat(parameter.getName()).isEqualTo(name);
		assertThat(parameter.getType()).isEqualTo(type);
		assertThat(parameter.getDescription()).isEqualTo(description);
	}

	private Map<String, EndpointInfo<JmxEndpointOperation>> discover(
			JmxEndpointDiscoverer discoverer) {
		Map<String, EndpointInfo<JmxEndpointOperation>> endpointsById = new HashMap<>();
		discoverer.discoverEndpoints().forEach((endpoint) -> {
			endpointsById.put(endpoint.getId(), endpoint);
		});
		return endpointsById;
	}

	private Map<String, JmxEndpointOperation> mapOperations(
			Collection<JmxEndpointOperation> operations) {
		Map<String, JmxEndpointOperation> operationByName = new HashMap<>();
		operations.forEach((operation) -> {
			operationByName.put(operation.getOperationName(), operation);
		});
		return operationByName;
	}

	private void load(Class<?> configuration, Consumer<JmxEndpointDiscoverer> consumer) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configuration)) {
			EndpointDiscoverer endpointDiscoverer = new EndpointDiscoverer(context);
			consumer.accept(new JmxEndpointDiscoverer(endpointDiscoverer));
		}
	}

	@Endpoint(id = "test")
	private static class TestEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

		@ReadOperation
		public String getSomething(TimeUnit timeUnit) {
			return null;
		}

		@WriteOperation
		public void update(String foo, String bar) {

		}

	}

	@JmxEndpoint(id = "test")
	private static class TestJmxEndpoint {

		@ManagedOperation(description = "Get all the things")
		@ReadOperation
		public Object getAll() {
			return null;
		}

		@ReadOperation
		@ManagedOperation(description = "Get something based on a timeUnit")
		@ManagedOperationParameters({
				@ManagedOperationParameter(name = "unitMs", description = "Number of milliseconds") })
		public String getSomething(Long timeUnit) {
			return null;
		}

		@WriteOperation
		@ManagedOperation(description = "Update something based on bar")
		@ManagedOperationParameters({
				@ManagedOperationParameter(name = "foo", description = "Foo identifier"),
				@ManagedOperationParameter(name = "bar", description = "Bar value") })
		public void update(String foo, String bar) {

		}

	}

	@JmxEndpoint(id = "test")
	private static class AdditionalOperationJmxEndpoint {

		@ManagedOperation(description = "Get another thing")
		@ReadOperation
		public Object getAnother() {
			return null;
		}

	}

	@Configuration
	static class EmptyConfiguration {

	}

	@Configuration
	static class OverriddenOperationJmxEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		public TestJmxEndpoint testJmxEndpoint() {
			return new TestJmxEndpoint();
		}

	}

	@Configuration
	static class AdditionalOperationJmxEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

		@Bean
		public AdditionalOperationJmxEndpoint additionalOperationJmxEndpoint() {
			return new AdditionalOperationJmxEndpoint();
		}

	}

	@Configuration
	static class ClashingJmxEndpointConfiguration {

		@Bean
		public TestJmxEndpoint testEndpointTwo() {
			return new TestJmxEndpoint();
		}

		@Bean
		public TestJmxEndpoint testEndpointOne() {
			return new TestJmxEndpoint();
		}
	}

	@Configuration
	static class ClashingStandardEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpointTwo() {
			return new TestEndpoint();
		}

		@Bean
		public TestEndpoint testEndpointOne() {
			return new TestEndpoint();
		}
	}

}
