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

import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.LinkedList;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 *
 * @author Stephane Nicoll
 */
@Mojo(name = "start", requiresProject = true, defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class StartMojo extends AbstractRunMojo {

	private static final String ENABLE_SHUTDOWN = "--spring.application.lifecycle.enabled=true";

	private final Object lock = new Object();

	protected void runWithForkedJvm(String startClassName) throws MojoExecutionException {
		throw new UnsupportedOperationException("Fork mode not supported by start goal");
	}

	protected void runWithMavenJvm(String startClassName, String... arguments) throws MojoExecutionException {
		IsolatedThreadGroup threadGroup = new IsolatedThreadGroup(startClassName);
		Thread launchThread = new Thread(threadGroup, new LaunchRunner(startClassName,
				resolveArguments(arguments)), startClassName + ".main()");
		launchThread.setContextClassLoader(new URLClassLoader(getClassPathUrls()));
		launchThread.start();

		// TODO: waaaaaaat?
		synchronized (this.lock) {
			try {
				this.lock.wait(10000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private String[] resolveArguments(String... arguments) {
		LinkedList<String> args = (arguments != null ?
				new LinkedList<String>(Arrays.asList(arguments)) : new LinkedList<String>());
		args.addFirst(ENABLE_SHUTDOWN);
		return args.toArray(new String[args.size()]);
	}
}
