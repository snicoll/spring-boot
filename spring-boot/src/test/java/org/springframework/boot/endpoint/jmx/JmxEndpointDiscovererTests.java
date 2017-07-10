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

import org.junit.Test;

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

	@Test
	public void discoveryWorksWhenThereAreNoEndpoints() {
		load(EmptyConfiguration.class, (discoverer) -> {
			assertThat(discoverer.discoverEndpoints()).isEmpty();
		});
	}


	@Test
	public void standardEndpointIsDiscovered() {
		load(TestEndpoint.class, d -> {
			Map<String, EndpointInfo<JmxEndpointOperationInfo>> endpoints = discover(d);
			assertThat(endpoints).containsOnlyKeys("test");
			Map<String, JmxEndpointOperationInfo> operationByName = mapOperations(
					endpoints.get("test").getOperations());
			assertThat(operationByName).containsOnlyKeys(
					"getAll", "getSomething", "update");
			JmxEndpointOperationInfo getAll = operationByName.get("getAll");
			assertThat(getAll.getDescription()).isEqualTo("Invoke getAll for endpoint test");
			assertThat(getAll.getGetOutputType()).isEqualTo(Object.class);
			assertThat(getAll.getParameters()).isEmpty();
			JmxEndpointOperationInfo getSomething = operationByName.get("getSomething");
			assertThat(getSomething.getDescription()).isEqualTo("Invoke getSomething for endpoint test");
			assertThat(getSomething.getGetOutputType()).isEqualTo(String.class);
			assertThat(getSomething.getParameters()).hasSize(1);
			hasParameter(getSomething, 0, String.class, null);
			JmxEndpointOperationInfo update = operationByName.get("update");
			assertThat(update.getDescription()).isEqualTo("Invoke update for endpoint test");
			assertThat(update.getGetOutputType()).isEqualTo(Void.TYPE);
			assertThat(update.getParameters()).hasSize(1);
			hasParameter(update, 0, String.class, null);
		});

	}

	@Test
	public void jmxOnlyEndpointIsDiscovered() {
		load(TestJmxEndpoint.class, d -> {
			Map<String, EndpointInfo<JmxEndpointOperationInfo>> endpoints = discover(d);
			assertThat(endpoints).containsOnlyKeys("test");
			assertJmxTestEndpoint(endpoints.get("test"));
		});

	}

	@Test
	public void jmxEndpointOverridesStandardEndpoint() {
		load(OverriddenOperationJmxEndpointConfiguration.class, d -> {
			Map<String, EndpointInfo<JmxEndpointOperationInfo>> endpoints = discover(d);
			assertThat(endpoints).containsOnlyKeys("test");
			assertJmxTestEndpoint(endpoints.get("test"));
		});

	}

	private void assertJmxTestEndpoint(EndpointInfo<JmxEndpointOperationInfo> endpoint) {
		Map<String, JmxEndpointOperationInfo> operationByName = mapOperations(
				endpoint.getOperations());
		assertThat(operationByName).containsOnlyKeys(
				"getAll", "getSomething", "update");
		JmxEndpointOperationInfo getAll = operationByName.get("getAll");
		assertThat(getAll.getDescription()).isEqualTo("Get all the things");
		assertThat(getAll.getGetOutputType()).isEqualTo(Object.class);
		assertThat(getAll.getParameters()).isEmpty();
		JmxEndpointOperationInfo getSomething = operationByName.get("getSomething");
		assertThat(getSomething.getDescription()).isEqualTo("Get something based on a timeUnit");
		assertThat(getSomething.getGetOutputType()).isEqualTo(String.class);
		assertThat(getSomething.getParameters()).hasSize(1);
		hasNamedParameter(getSomething, 0, "unitMs", Long.class, "Number of milliseconds");
		JmxEndpointOperationInfo update = operationByName.get("update");
		assertThat(update.getDescription()).isEqualTo("Update something based on bar");
		assertThat(update.getGetOutputType()).isEqualTo(Void.TYPE);
		assertThat(update.getParameters()).hasSize(1);
		hasNamedParameter(update, 0, "bar", String.class, "Test argument");
	}

	private void hasParameter(JmxEndpointOperationInfo operation, int index,
			Class<?> type, String description) {
		assertThat(index).isLessThan(operation.getParameters().size());
		JmxEndpointOperationParameterInfo parameter = operation.getParameters().get(index);
		assertThat(parameter.getType()).isEqualTo(type);
		assertThat(parameter.getDescription()).isEqualTo(description);
	}

	private void hasNamedParameter(JmxEndpointOperationInfo operation, int index,
			String name, Class<?> type, String description) {
		assertThat(index).isLessThan(operation.getParameters().size());
		JmxEndpointOperationParameterInfo parameter = operation.getParameters().get(index);
		assertThat(parameter.getName()).isEqualTo(name);
		assertThat(parameter.getType()).isEqualTo(type);
		assertThat(parameter.getDescription()).isEqualTo(description);
	}

	private Map<String, EndpointInfo<JmxEndpointOperationInfo>> discover(
			JmxEndpointDiscoverer discoverer) {
		Map<String, EndpointInfo<JmxEndpointOperationInfo>> endpointsById = new HashMap<>();
		discoverer.discoverEndpoints().forEach((endpoint) -> {
			endpointsById.put(endpoint.getId(), endpoint);
		});
		return endpointsById;
	}

	private Map<String, JmxEndpointOperationInfo> mapOperations(
			Collection<JmxEndpointOperationInfo> operations) {
		Map<String, JmxEndpointOperationInfo> operationByName = new HashMap<>();
		operations.forEach((operation) -> {
			operationByName.put(operation.getOperationName(), operation);
		});
		return operationByName;
	}

	private void load(Class<?> configuration,
			Consumer<JmxEndpointDiscoverer> consumer) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configuration)) {
			EndpointDiscoverer endpointDiscoverer = new EndpointDiscoverer(context);
			consumer.accept(new JmxEndpointDiscoverer(endpointDiscoverer, context));
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
		public void update(String foo) {

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
				@ManagedOperationParameter(name = "bar", description = "Test argument") })
		public void update(String bar) {

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

}