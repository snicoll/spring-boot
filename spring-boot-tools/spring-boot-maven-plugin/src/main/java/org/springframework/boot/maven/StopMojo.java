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

package org.springframework.boot.maven;

import java.lang.management.ManagementFactory;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 *
 * @author Stephane Nicoll
 */
@Mojo(name = "stop", requiresProject = true, defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST)
public class StopMojo extends AbstractMojo {

	//Note: see org.springframework.boot.autoconfigure.test.ApplicationShutdownAutoConfiguration
	static final String OBJECT_NAME = "org.springframework.boot.test:type=SpringApplicationLifecycle";

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		//TODO: support fork?
		getLog().info("Stopping Spring Boot application.");
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName objectName = new ObjectName(OBJECT_NAME);
			server.invoke(objectName, "shutdown", null, null);
		}
		catch (MalformedObjectNameException e) {
			throw new IllegalStateException("Invalid object name", e);
		}
		catch (InstanceNotFoundException e) {
			throw new MojoFailureException("Application shutdown hook not found. Could not " +
					"stop application gracefully", e);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Could not stop application", e);
		}

	}
}
