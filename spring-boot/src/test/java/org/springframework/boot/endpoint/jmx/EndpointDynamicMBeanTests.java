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
import java.util.UUID;
import java.util.function.Consumer;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.EndpointDiscoverer;
import org.springframework.boot.endpoint.ReadOperation;
import org.springframework.boot.endpoint.WriteOperation;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointDynamicMBean}.
 *
 * @author Stephane Nicoll
 */
public class EndpointDynamicMBeanTests {

	private final JmxEndpointMBeanFactory jmxEndpointMBeanFactory =
			new JmxEndpointMBeanFactory(r -> (r != null ? r.toString().toUpperCase() : null));

	private MBeanServer server;

	@Before
	public void createMBeanServer() {
		this.server = MBeanServerFactory.createMBeanServer();
	}

	@After
	public void disposeMBeanServer() {
		if (this.server != null) {
			MBeanServerFactory.releaseMBeanServer(this.server);
		}
	}

	@Test
	public void createSimpleEndpoint() {
		load(FooEndpoint.class, discoverer -> {
			Collection<EndpointDynamicMBean> mBeans = this.jmxEndpointMBeanFactory
					.createMBeans(discoverer.discoverEndpoints());
			assertThat(mBeans).hasSize(1);
			ObjectName objectName = register(mBeans.iterator().next());
			try {
				// getAll
				Object allResponse = this.server.invoke(objectName, "getAll",
						new Object[0], new String[0]);
				assertThat(allResponse).isEqualTo("[ONE, TWO]");

				// getOne
				Object oneResponse = this.server.invoke(objectName, "getOne",
						new Object[] { "one" }, new String[] { String.class.getName() });
				assertThat(oneResponse).isEqualTo("ONE");

				// update
				Object updateResponse = this.server.invoke(objectName, "update",
						new Object[] { "one", "1" }, new String[] {
								String.class.getName(), String.class.getName() });
				assertThat(updateResponse).isNull();

				// getOne validation after update
				Object updatedOneResponse = this.server.invoke(objectName, "getOne",
						new Object[] { "one" }, new String[] { String.class.getName() });
				assertThat(updatedOneResponse).isEqualTo("1");

			}
			catch (Exception ex) {
				throw new AssertionError("Failed to invoke getAll method", ex);
			}

		});
	}

	private ObjectName register(Object mbean) {
		try {
			ObjectName objectName = getRandomObjectName();
			this.server.registerMBean(mbean, objectName);
			return objectName;
		}
		catch (Exception ex) {
			throw new IllegalStateException("Invalid MBean " + mbean + "", ex);
		}
	}

	private ObjectName getRandomObjectName() {
		String name = String.format("org.springframework.boot.test:type=Endpoint,name=%s",
				UUID.randomUUID().toString());
		try {
			return new ObjectName(name);
		}
		catch (MalformedObjectNameException ex) {
			throw new IllegalStateException("Invalid object name", ex);
		}
	}


	private void load(Class<?> configuration, Consumer<JmxEndpointDiscoverer> consumer) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				configuration)) {
			EndpointDiscoverer endpointDiscoverer = new EndpointDiscoverer(context);
			consumer.accept(new JmxEndpointDiscoverer(endpointDiscoverer));
		}
	}


	@Endpoint(id = "foo")
	static class FooEndpoint {

		private final Map<String, Foo> all = new HashMap<>();

		FooEndpoint() {
			this.all.put("one", new Foo("one"));
			this.all.put("two", new Foo("two"));
		}

		@ReadOperation
		public Collection<Foo> getAll() {
			return this.all.values();
		}

		@ReadOperation
		public Foo getOne(String name) {
			return this.all.get(name);
		}

		@WriteOperation
		public void update(String name, String value) {
			this.all.put(name, new Foo(value));
		}

	}

	static class Foo {

		private final String name;

		Foo(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}

}
