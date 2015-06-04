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

/**
 * A simple MBean contract to control the lifecycle of a {@code SpringApplication} via
 * JMX. Intended for internal use only.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
public interface SpringApplicationLifecycleMXBean {

	/**
	 * Specify if the application has fully started and is now ready.
	 * @return {@code true} if the application is ready
	 * @see org.springframework.boot.context.event.ApplicationReadyEvent
	 */
	boolean isReady();

	/**
	 * Specify if the application runs in an embedded web container. Can return
	 * {@code null} if that information is not yet available. It is preferable to
	 * wait for the application to be {@link #isReady() ready}.
	 * @return {@code true} if the application runs in an embedded web container
	 * @see #isReady()
	 */
	boolean isEmbeddedWebApplication();

	/**
	 * Return the HTTP port used by the application or {@code null} if the
	 * application is not an embedded web application or has not fully started yet.
	 * @return the HTTP port of the web application or {@code null}
	 * @see #isEmbeddedWebApplication()
	 * @see #isReady()
	 */
	Integer getLocalServerPort();

	/**
	 * Return the dedicated HTTP port used by the management service or
	 * {@code null} if any of the following apply:
	 * <ol>
	 * <li>The application is not an embedded web application,</li>
	 * <li>The application has not fully started yet</li>
	 * <li>There is no management service or no dedicated servlet
	 * container for it.</li>
	 * </ol>
	 * If this returns {@code null}, it is sane to assume that the management
	 * service might run as part of the application.
	 * @return the HTTP port of the management service or {@code null}
	 * @see #isEmbeddedWebApplication()
	 * @see #isReady()
	 * @see #getLocalServerPort()
	 */
	Integer getLocalManagementPort();

	/**
	 * Shutdown the application.
	 * @see org.springframework.context.ConfigurableApplicationContext#close()
	 */
	void shutdown();

}
