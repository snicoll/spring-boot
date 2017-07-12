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

package org.springframework.boot.endpoint;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointDiscoverer}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
public class EndpointDiscovererTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void discoverWorksWhenThereAreNoEndpoints() {
		load(EmptyConfiguration.class, (context) -> assertThat(
				new TestEndpointDiscoverer(context).discoverEndpoints().isEmpty()));
	}

	@Test
	public void endpointIsDiscovered() {
		load(TestEndpointConfiguration.class, (context) -> {
			Collection<EndpointInfo<TestEndpointOperation>> endpoints = new TestEndpointDiscoverer(
					context).discoverEndpoints();
			assertThat(endpoints).hasSize(1);
			EndpointInfo<TestEndpointOperation> endpoint = endpoints.iterator().next();
			assertThat(endpoint.getId()).isEqualTo("test");
			Collection<TestEndpointOperation> operations = endpoint.getOperations();
			assertThat(operations).hasSize(3);
			Map<Method, EndpointOperation> operationByMethod = new HashMap<>();
			operations.forEach((operation) -> {
				operationByMethod.put(operation.getOperationMethod(), operation);
			});
			assertThat(operationByMethod).containsKeys(
					ReflectionUtils.findMethod(TestEndpoint.class, "getAll"),
					ReflectionUtils.findMethod(TestEndpoint.class, "getOne",
							String.class),
					ReflectionUtils.findMethod(TestEndpoint.class, "update", String.class,
							String.class));
		});
	}

	@Test
	public void discoveryFailsWhenTwoEndpointsHaveTheSameId() {
		load(ClashingEndpointConfiguration.class, (context) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Found two endpoints with the id 'test': ");
			new TestEndpointDiscoverer(context).discoverEndpoints();
		});
	}

	private void load(Class<?> configuration,
			Consumer<AnnotationConfigApplicationContext> consumer) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configuration);
		try {
			consumer.accept(context);
		}
		finally {
			context.close();
		}
	}

	@Configuration
	static class EmptyConfiguration {

	}

	@Endpoint(id = "test")
	static class TestEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

		@ReadOperation
		public Object getOne(@Selector String id) {
			return null;
		}

		@WriteOperation
		public void update(String foo, String bar) {

		}

		public void someOtherMethod() {

		}

	}

	@Configuration
	static class TestEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

	}

	@Configuration
	static class ClashingEndpointConfiguration {

		@Bean
		public TestEndpoint testEndpointTwo() {
			return new TestEndpoint();
		}

		@Bean
		public TestEndpoint testEndpointOne() {
			return new TestEndpoint();
		}
	}

	private static final class TestEndpointOperation extends EndpointOperation {

		private final Method operationMethod;

		private TestEndpointOperation(EndpointOperationType type,
				OperationInvoker operationInvoker, Method operationMethod) {
			super(type, operationInvoker, true);
			this.operationMethod = operationMethod;
		}

		private Method getOperationMethod() {
			return this.operationMethod;
		}

	}

	private static class TestEndpointDiscoverer
			extends EndpointDiscoverer<TestEndpointOperation, Method> {

		TestEndpointDiscoverer(ApplicationContext applicationContext) {
			super(applicationContext, endpointOperationFactory(),
					TestEndpointOperation::getOperationMethod);
		}

		@Override
		public Collection<EndpointInfo<TestEndpointOperation>> discoverEndpoints() {
			return discoverEndpointsWithExtension(null).stream()
					.map(EndpointInfoDescriptor::getEndpointInfo)
					.collect(Collectors.toList());
		}

		private static EndpointOperationFactory<TestEndpointOperation> endpointOperationFactory() {
			return new EndpointOperationFactory<TestEndpointOperation>() {

				@Override
				public TestEndpointOperation createOperation(String endpointId,
						AnnotationAttributes operationAttributes, Object target,
						Method operationMethod, EndpointOperationType operationType) {
					return new TestEndpointOperation(operationType, null,
							operationMethod);
				}
			};
		}

	}

}
