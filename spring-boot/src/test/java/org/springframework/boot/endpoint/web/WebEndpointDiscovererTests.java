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
import java.util.Collection;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.EndpointDiscoverer;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.ReadOperation;
import org.springframework.boot.endpoint.Selector;
import org.springframework.boot.endpoint.WriteOperation;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebEndpointDiscoverer}.
 *
 * @author Andy Wilkinson
 */
public class WebEndpointDiscovererTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void discoveryWorksWhenThereAreNoEndpoints() {
		load(EmptyConfiguration.class, (discoverer) -> {
			assertThat(discoverer.discoverEndpoints()).isEmpty();
		});
	}

	@Test
	public void standardEndpointIsDiscovered() {
		load(TestWebEndpointConfiguration.class, (discoverer) -> {
			Collection<EndpointInfo<WebEndpointOperation>> endpoints = discoverer
					.discoverEndpoints();
			assertThat(endpoints).hasSize(1);
			EndpointInfo<WebEndpointOperation> endpoint = endpoints.iterator().next();
			assertThat(endpoint.getId()).isEqualTo("test");
			Collection<WebEndpointOperation> operations = endpoint.getOperations();
			assertThat(operations).hasSize(3);
		});
	}

	@Test
	public void webOnlyEndpointIsDiscovered() {
		load(TestWebEndpointConfiguration.class, (discoverer) -> {
			Collection<EndpointInfo<WebEndpointOperation>> endpoints = discoverer
					.discoverEndpoints();
			assertThat(endpoints).hasSize(1);
			EndpointInfo<WebEndpointOperation> endpoint = endpoints.iterator().next();
			assertThat(endpoint.getId()).isEqualTo("test");
			Collection<WebEndpointOperation> operations = endpoint.getOperations();
			assertThat(operations).hasSize(3);
		});
	}

	@Test
	public void oneOperationIsDiscoveredWhenWebEndpointOverridesOperation() {
		load(OverriddenOperationWebEndpointConfiguration.class, (discoverer) -> {
			Collection<EndpointInfo<WebEndpointOperation>> endpoints = discoverer
					.discoverEndpoints();
			assertThat(endpoints).hasSize(1);
			EndpointInfo<WebEndpointOperation> endpoint = endpoints.iterator().next();
			assertThat(endpoint.getId()).isEqualTo("test");
			Collection<WebEndpointOperation> operations = endpoint.getOperations();
			assertThat(operations).hasSize(1);
		});
	}

	@Test
	public void twoOperationsAreDiscoveredWhenWebEndpointAddsOperation() {
		load(AdditionalOperationWebEndpointConfiguration.class, (discoverer) -> {
			Collection<EndpointInfo<WebEndpointOperation>> endpoints = discoverer
					.discoverEndpoints();
			assertThat(endpoints).hasSize(1);
			EndpointInfo<WebEndpointOperation> endpoint = endpoints.iterator().next();
			assertThat(endpoint.getId()).isEqualTo("test");
			Collection<WebEndpointOperation> operations = endpoint.getOperations();
			assertThat(operations).hasSize(2);
		});
	}

	@Test
	public void discoveryFailsWhenTwoWebEndpointsHaveTheSameId() {
		load(ClashingWebEndpointConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Found two endpoints with the id 'test': ");
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void discoveryFailsWhenTwoStandardEndpointsHaveTheSameId() {
		load(ClashingStandardEndpointConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage("Found two endpoints with the id 'test': ");
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void discoveryFailsWhenStandardEndpointHasClashingOperations() {
		load(ClashingOperationsEndpointConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage(
					"Found multiple web operations with matching request predicates:");
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void discoveryFailsWhenWebEndpointHasClashingOperations() {
		load(ClashingOperationsWebEndpointConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage(
					"Found multiple web operations with matching request predicates:");
			discoverer.discoverEndpoints();
		});
	}

	@Test
	public void twoOperationsOnSameEndpointClashWhenSelectorsHaveDifferentNames() {
		load(ClashingSelectorsWebEndpointConfiguration.class, (discoverer) -> {
			this.thrown.expect(IllegalStateException.class);
			this.thrown.expectMessage(
					"Found multiple web operations with matching request predicates:");
			discoverer.discoverEndpoints();
		});
	}

	private void load(Class<?> configuration, Consumer<WebEndpointDiscoverer> consumer) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configuration);
		try {
			consumer.accept(new WebEndpointDiscoverer(new EndpointDiscoverer(context),
					Arrays.asList("application/json"),
					Arrays.asList("application/json")));
		}
		finally {
			context.close();
		}
	}

	@Configuration
	static class EmptyConfiguration {

	}

	@WebEndpoint(id = "test")
	static class TestWebEndpoint {

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

	@Endpoint(id = "test")
	static class TestEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	@WebEndpoint(id = "test")
	static class OverriddenOperationWebEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

	}

	@WebEndpoint(id = "test")
	static class AdditionalOperationWebEndpoint {

		@ReadOperation
		public Object getOne(@Selector String id) {
			return null;
		}

	}

	@Endpoint(id = "test")
	static class ClashingOperationsEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

		@ReadOperation
		public Object getAgain() {
			return null;
		}

	}

	@WebEndpoint(id = "test")
	static class ClashingOperationsWebEndpoint {

		@ReadOperation
		public Object getAll() {
			return null;
		}

		@ReadOperation
		public Object getAgain() {
			return null;
		}

	}

	@WebEndpoint(id = "test")
	static class ClashingSelectorsWebEndpoint {

		@ReadOperation
		public Object readOne(@Selector String oneA, @Selector String oneB) {
			return null;
		}

		@ReadOperation
		public Object readTwo(@Selector String twoA, @Selector String twoB) {
			return null;
		}

	}

	@Configuration
	static class TestWebEndpointConfiguration {

		@Bean
		public TestWebEndpoint testEndpoint() {
			return new TestWebEndpoint();
		}

	}

	@Configuration
	static class ClashingOperationsEndpointConfiguration {

		@Bean
		public ClashingOperationsEndpoint testEndpoint() {
			return new ClashingOperationsEndpoint();
		}

	}

	@Configuration
	static class ClashingOperationsWebEndpointConfiguration {

		@Bean
		public ClashingOperationsWebEndpoint testEndpoint() {
			return new ClashingOperationsWebEndpoint();
		}

	}

	@Configuration
	@Import(TestEndpointConfiguration.class)
	static class OverriddenOperationWebEndpointConfiguration {

		@Bean
		public OverriddenOperationWebEndpoint overriddenOperationWebEndpoint() {
			return new OverriddenOperationWebEndpoint();
		}

	}

	@Configuration
	@Import(TestEndpointConfiguration.class)
	static class AdditionalOperationWebEndpointConfiguration {

		@Bean
		public AdditionalOperationWebEndpoint additionalOperationWebEndpoint() {
			return new AdditionalOperationWebEndpoint();
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
	static class ClashingWebEndpointConfiguration {

		@Bean
		public TestWebEndpoint testEndpointTwo() {
			return new TestWebEndpoint();
		}

		@Bean
		public TestWebEndpoint testEndpointOne() {
			return new TestWebEndpoint();
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

	@Configuration
	static class ClashingSelectorsWebEndpointConfiguration {

		@Bean
		public ClashingSelectorsWebEndpoint clashingSelectorsWebEndpoint() {
			return new ClashingSelectorsWebEndpoint();
		}

	}

}
