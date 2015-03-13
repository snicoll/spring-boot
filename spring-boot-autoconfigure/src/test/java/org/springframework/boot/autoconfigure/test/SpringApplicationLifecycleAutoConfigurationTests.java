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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;

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

	public static final String ENABLE_LIFECYLE_PROP = "spring.application.lifecycle.enabled=true";

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	private MBeanServer mBeanServer;

	private ObjectName objectName;

	@Before
	public void setup() throws MalformedObjectNameException {
		this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
		this.objectName = new ObjectName(SpringApplicationLifecycleAutoConfiguration.OBJECT_NAME);
	}

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void notRegisteredByDefault() throws MalformedObjectNameException, InstanceNotFoundException {
		load();

		thrown.expect(InstanceNotFoundException.class);
		this.mBeanServer.getObjectInstance(this.objectName);
	}

	@Test
	public void registeredWithProperty() throws Exception {
		load(ENABLE_LIFECYLE_PROP);

		ObjectInstance objectInstance = this.mBeanServer.getObjectInstance(this.objectName);
		assertNotNull(objectInstance);

		assertEquals("Simple context does not trigger proper event",
				false, isCurrentApplicationReady());
		this.context.publishEvent(new ApplicationReadyEvent(this.context, null));
		assertEquals("Application should be ready",
				true, isCurrentApplicationReady());

		assertTrue("context has been started", this.context.isActive());
		mBeanServer.invoke(this.objectName, "shutdown", null, null);
		assertFalse("Context should have been closed", this.context.isActive());

		thrown.expect(InstanceNotFoundException.class); // JMX cleanup
		this.mBeanServer.getObjectInstance(this.objectName);
	}

	@Test
	public void registerWithSpringApplication() throws Exception {
		SpringApplication application = new SpringApplication(ExampleConfig.class,
				SpringApplicationLifecycleAutoConfiguration.class);
		application.setWebEnvironment(false);
		application.addListeners(new ApplicationListener<ContextRefreshedEvent>() {
			@Override
			public void onApplicationEvent(ContextRefreshedEvent event) {
				try {
					assertFalse("Application should not be ready yet", isCurrentApplicationReady());
				}
				catch (Exception e) {
					throw new IllegalStateException("Could not contact spring application lifecycle bean", e);
				}
			}
		});
		application.run("--" + ENABLE_LIFECYLE_PROP);
		assertTrue("application should be ready now", isCurrentApplicationReady());
	}

	private Boolean isCurrentApplicationReady() throws Exception {
		return (Boolean) this.mBeanServer.getAttribute(this.objectName, "Ready");
	}

	private void load(String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(JmxAutoConfiguration.class, SpringApplicationLifecycleAutoConfiguration.class);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	@Configuration
	static class ExampleConfig {

	}

}

