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

package org.springframework.boot.context;

import java.lang.management.ManagementFactory;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SpringApplicationLifecycleRegistrar}.
 *
 * @author Stephane Nicoll
 */
public class SpringApplicationLifecycleRegistrarTests {

	private static final String OBJECT_NAME = "org.springframework.boot:type=Test,name=springApplicationLifecycle";

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private MBeanServer mBeanServer;

	private ConfigurableApplicationContext context;

	@Before
	public void setup() throws MalformedObjectNameException {
		this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
	}

	@After
	public void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void validateReadyFlag() {
		final ObjectName objectName = createObjectName(OBJECT_NAME);
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		application.addListeners(new ApplicationListener<ContextRefreshedEvent>() {
			@Override
			public void onApplicationEvent(ContextRefreshedEvent event) {
				try {
					assertThat(isApplicationReady(objectName), is(false));
				}
				catch (Exception ex) {
					throw new IllegalStateException(
							"Could not contact spring application lifecycle bean", ex);
				}
			}
		});
		this.context = application.run();
		assertThat(isApplicationReady(objectName), is(true));
	}

	@Test
	public void notAWebApplication() {
		final ObjectName objectName = createObjectName(OBJECT_NAME);
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(isApplicationReady(objectName), is(true));
		assertThat(isApplicationEmbeddedWebApplication(objectName), is(false));
		assertThat(getLocalServerPort(objectName), is(nullValue()));
		assertThat(getLocalManagementPort(objectName), is(nullValue()));
	}

	@Test
	public void shutdownApp() throws InstanceNotFoundException {
		final ObjectName objectName = createObjectName(OBJECT_NAME);
		SpringApplication application = new SpringApplication(Config.class);
		application.setWebEnvironment(false);
		this.context = application.run();
		assertThat(this.context.isRunning(), is(true));
		invokeShutdown(objectName);
		assertThat(this.context.isRunning(), is(false));
		this.thrown.expect(InstanceNotFoundException.class); // JMX cleanup
		this.mBeanServer.getObjectInstance(objectName);
	}

	@Test
	public void registerHttpPort() throws Exception {
		final ObjectName objectName = createObjectName(OBJECT_NAME);
		SpringApplicationLifecycleRegistrar registrar = new SpringApplicationLifecycleRegistrar(OBJECT_NAME);
		try {
			registrar.afterPropertiesSet();
			registrar.handleEmbeddedServletContainerInitializedEvent(
					createEmbeddedServletContainerInitializedEvent("server", 8080));
			assertThat(getLocalServerPort(objectName), is(8080));
			assertThat(getLocalManagementPort(objectName), is(nullValue()));
		}
		finally {
			registrar.destroy();
		}
	}

	@Test
	public void registerNullPortAndManagementPort() throws Exception {
		final ObjectName objectName = createObjectName(OBJECT_NAME);
		SpringApplicationLifecycleRegistrar registrar = new SpringApplicationLifecycleRegistrar(OBJECT_NAME);
		try {
			registrar.afterPropertiesSet();
			registrar.handleEmbeddedServletContainerInitializedEvent(
					createEmbeddedServletContainerInitializedEvent("", 7070));
			registrar.handleEmbeddedServletContainerInitializedEvent(
					createEmbeddedServletContainerInitializedEvent("management", 9090));
			assertThat(getLocalServerPort(objectName), is(7070));
			assertThat(getLocalManagementPort(objectName), is(9090));
		}
		finally {
			registrar.destroy();
		}
	}

	private EmbeddedServletContainerInitializedEvent createEmbeddedServletContainerInitializedEvent(
			String namespace, int port) {
		EmbeddedServletContainerInitializedEvent event = mock(EmbeddedServletContainerInitializedEvent.class);
		EmbeddedWebApplicationContext context = mock(EmbeddedWebApplicationContext.class);
		when(context.getNamespace()).thenReturn(namespace);
		when(event.getApplicationContext()).thenReturn(context);
		EmbeddedServletContainer container = mock(EmbeddedServletContainer.class);
		when(container.getPort()).thenReturn(port);
		when(event.getEmbeddedServletContainer()).thenReturn(container);
		return event;
	}


	private Boolean isApplicationReady(ObjectName objectName) {
		return getAttribute(objectName, Boolean.class, "Ready");
	}

	private Boolean isApplicationEmbeddedWebApplication(ObjectName objectName) {
		return getAttribute(objectName, Boolean.class, "EmbeddedWebApplication");
	}

	private Integer getLocalServerPort(ObjectName objectName) {
		return getAttribute(objectName, Integer.class, "LocalServerPort");
	}

	private Integer getLocalManagementPort(ObjectName objectName) {
		return getAttribute(objectName, Integer.class, "LocalManagementPort");
	}

	private <T> T getAttribute(ObjectName objectName, Class<T> type, String attribute) {
		try {
			Object value = this.mBeanServer.getAttribute(objectName, attribute);
			assertThat((value == null || type.isInstance(value)), is(true));
			return type.cast(value);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
	}

	private void invokeShutdown(ObjectName objectName) {
		try {
			this.mBeanServer.invoke(objectName, "shutdown", null, null);
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex.getMessage(), ex);
		}
	}

	private ObjectName createObjectName(String jmxName) {
		try {
			return new ObjectName(jmxName);
		}
		catch (MalformedObjectNameException e) {
			throw new IllegalStateException("Invalid jmx name " + jmxName, e);
		}
	}

	@Configuration
	static class Config {

		@Bean
		public SpringApplicationLifecycleRegistrar springApplicationLifecycle()
				throws MalformedObjectNameException {
			return new SpringApplicationLifecycleRegistrar(OBJECT_NAME);
		}

	}

}
