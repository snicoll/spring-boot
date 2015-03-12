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

package org.springframework.boot.autoconfigure.test;

import java.lang.management.ManagementFactory;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link SpringApplicationLifecycleAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class SpringApplicationLifecycleAutoConfigurationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void notRegisteredByDefault() throws MalformedObjectNameException, InstanceNotFoundException {
		load();
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName objectName = new ObjectName(SpringApplicationLifecycleAutoConfiguration.OBJECT_NAME);


		thrown.expect(InstanceNotFoundException.class);
		server.getObjectInstance(objectName);
	}

	@Test
	public void registeredWithProperty() throws Exception {
		load("spring.application.lifecycle.enabled=true");
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		ObjectName objectName = new ObjectName(SpringApplicationLifecycleAutoConfiguration.OBJECT_NAME);

		ObjectInstance objectInstance = server.getObjectInstance(objectName);
		assertNotNull(objectInstance);

		assertEquals("Simple context does not trigger proper event",
				false, server.getAttribute(objectName, "Ready"));
		this.context.publishEvent(new ApplicationStartedEvent(new SpringApplication(), null));
		assertEquals("Application should be ready",
				true, server.getAttribute(objectName, "Ready"));

		assertTrue("context has been started", this.context.isActive());
		server.invoke(objectName, "shutdown", null, null);
		assertFalse("Context should have been closed", this.context.isActive());

		thrown.expect(InstanceNotFoundException.class); // JMX cleanup
		server.getObjectInstance(objectName);
	}


	private void load(String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(JmxAutoConfiguration.class, SpringApplicationLifecycleAutoConfiguration.class);
		applicationContext.refresh();
		this.context = applicationContext;
	}

}

