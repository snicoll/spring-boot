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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.ConnectException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import org.springframework.boot.loader.tools.JavaExecutable;
import org.springframework.boot.loader.tools.RunProcess;

/**
 *
 * @author Stephane Nicoll
 */
@Mojo(name = "start", requiresProject = true, defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class StartMojo extends AbstractRunMojo {

	private static final String ENABLE_MBEAN_PROPERTY = "--spring.application.lifecycle.enabled=true";

	private static final String JMX_NAME_PROPERTY_PREFIX = "--spring.application.lifecycle.jmx-name=";

	/**
	 * The JMX name of the automatically deployed MBean managing the lifecycle
	 * of the Spring application.
	 */
	@Parameter
	private String jmxName = SpringApplicationLifecycleHelper.DEFAULT_OBJECT_NAME;

	/**
	 * The port to use to expose the platform MBeanServer if the application
	 * needs to be forked.
	 */
	@Parameter
	private int jmxPort = 9001;

	private final Object lock = new Object();

	protected void runWithMavenJvm(String startClassName, String... arguments)
			throws MojoExecutionException, MojoFailureException {
		IsolatedThreadGroup threadGroup = new IsolatedThreadGroup(startClassName);
		Thread launchThread = new Thread(threadGroup, new LaunchRunner(startClassName,
				resolveArguments(arguments)), startClassName + ".main()");
		launchThread.setContextClassLoader(new URLClassLoader(getClassPathUrls()));
		launchThread.start();
		waitForSpringApplication();
	}

	@Override
	protected void runWithForkedJvm(List<String> args) throws MojoExecutionException, MojoFailureException {
		RunProcess runProcess;
		try {
			runProcess = new RunProcess(new JavaExecutable().toString());
			runProcess.run(false, args.toArray(new String[args.size()]));
		}
		catch (Exception ex) {
			throw new MojoExecutionException("Could not exec java", ex);
		}

		try {
			waitForSpringApplication();
		}
		catch (MojoExecutionException e) {
			runProcess.kill();
			throw e;
		}
		catch (MojoFailureException e) {
			runProcess.kill();
			throw e;
		}
	}

	@Override
	protected RunArguments resolveJvmArguments() {
		RunArguments jvmArguments = super.resolveJvmArguments();
		List<String> remoteJmxArguments = new ArrayList<String>();
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote");
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote.port=" + jmxPort);
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote.authenticate=false");
		remoteJmxArguments.add("-Dcom.sun.management.jmxremote.ssl=false");
		jvmArguments.getArgs().addAll(remoteJmxArguments);
		return jvmArguments;
	}

	private String[] resolveArguments(String... arguments) {
		List<String> args = new ArrayList<String>(Arrays.asList(arguments));
		args.add(ENABLE_MBEAN_PROPERTY);
		args.add(JMX_NAME_PROPERTY_PREFIX + this.jmxName);
		return args.toArray(new String[args.size()]);
	}

	private void waitForSpringApplication() throws MojoFailureException, MojoExecutionException {
		try {
			if (Boolean.TRUE.equals(isFork())) {
				waitForForkedSpringApplication();
			}
			else {
				doWaitForSpringApplication(ManagementFactory.getPlatformMBeanServer());
			}
		}
		catch (IOException e) {
			throw new MojoFailureException("Could not contact Spring Boot application", e);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Could not figure out if the application has started", e);
		}

	}

	private void waitForForkedSpringApplication() throws IOException, MojoFailureException, MojoExecutionException {
		final JMXConnector jmxConnector;
		try {
			getLog().debug("Connecting to local MBeanServer at port " + this.jmxPort);
			jmxConnector = execute(1000, 60, new RetryCallback<JMXConnector>() {
				@Override
				public JMXConnector retry() throws Exception {
					try {
						return SpringApplicationLifecycleHelper.createLocalJmxConnector(jmxPort);
					}
					catch (IOException e) {
						if (hasCauseWithType(e, ConnectException.class)) { // Not there yet
							getLog().debug("MBean server at port " + jmxPort + " is not up yet...");
							return null;
						}
						else {
							throw e;
						}
					}
				}
			});
			getLog().debug("Connected to local MBeanServer at port " + this.jmxPort);
			try {
				MBeanServerConnection mBeanServerConnection = jmxConnector.getMBeanServerConnection();
				doWaitForSpringApplication(mBeanServerConnection);
			}
			finally {
				jmxConnector.close();
			}
		}
		catch (IOException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MojoExecutionException("Failed to connect to MBean server at port " + this.jmxPort, e);
		}
	}

	private void doWaitForSpringApplication(MBeanServerConnection connection)
			throws IOException, MojoExecutionException, MojoFailureException {

		final SpringApplicationLifecycleHelper helper =
				new SpringApplicationLifecycleHelper(connection, this.jmxName);
		try {
			execute(500, 60, new RetryCallback<Boolean>() {
				@Override
				public Boolean retry() throws Exception {
					boolean ready = helper.isReady();
					// Wait until the app is ready
					return (ready ? true : null);
				}
			});
		}
		catch (TimeoutException e) {
			throw new MojoExecutionException(e.getMessage());
		}
		catch (ReflectionException e) {
			throw new MojoExecutionException("Unable to retrieve Ready attribute", e.getCause());
		}
		catch (Exception e) {
			throw new MojoFailureException("Could not invoke shutdown operation", e);
		}
	}

	public <T> T execute(int wait, int maxAttempts, RetryCallback<T> callback) throws Exception {
		for (int i = 0; i < maxAttempts; i++) {
			T result = callback.retry();
			if (result != null) {
				return result;
			}
			getLog().debug("Application is not ready yet, waiting " + wait + "ms (attempt " + (i + 1) + ")");
			synchronized (this.lock) {
				try {
					this.lock.wait(wait);
				}
				catch (InterruptedException e) {
					throw new IllegalStateException("Interrupted while waiting for Spring Boot app to start.");
				}
			}
		}
		throw new TimeoutException("Spring Boot application did not start before the configured timeout");
	}

	private static boolean hasCauseWithType(Throwable t, Class<? extends Exception> type) {
		if (type.isAssignableFrom(t.getClass())) {
			return true;
		}
		else
			return t.getCause() != null && hasCauseWithType(t.getCause(), type);
	}


	interface RetryCallback<T> {

		/**
		 * Attempt to execute an operation. Throws an exception in case of fatal
		 * exception, returns {@code null} to indicate another attempt should be
		 * made if possible.
		 */
		T retry() throws Exception;
	}

}
