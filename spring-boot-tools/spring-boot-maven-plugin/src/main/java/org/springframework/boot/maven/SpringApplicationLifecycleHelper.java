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
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 *
 * @author Stephane Nicoll
 */
class SpringApplicationLifecycleHelper {

	//Note: see org.springframework.boot.autoconfigure.test.SpringApplicationLifecycleAutoConfiguration
	static final String DEFAULT_OBJECT_NAME =
			"org.springframework.boot.test:type=SpringApplicationLifecycle";

	private final MBeanServerConnection mBeanServerConnection;

	private final ObjectName objectName;

	public SpringApplicationLifecycleHelper(MBeanServerConnection mBeanServerConnection, String jmxName) {
		this.mBeanServerConnection = mBeanServerConnection;
		this.objectName = toObjectName(jmxName);
	}

	public static JMXConnector createLocalJmxConnector(int port) throws IOException {
		String url = "service:jmx:rmi:///jndi/rmi://127.0.0.1:" + port + "/jmxrmi";
		JMXServiceURL serviceUrl = new JMXServiceURL(url);
		return JMXConnectorFactory.connect(serviceUrl, null);
	}

	public boolean isReady() throws IOException, ReflectionException, MBeanException {
		try {
			return (Boolean) this.mBeanServerConnection.getAttribute(this.objectName, "Ready");
		}
		catch (InstanceNotFoundException e) {
			return false; // Instance not available yet
		}
		catch (AttributeNotFoundException e) {
			throw new IllegalStateException("Unexpected: attribute 'Ready' not available", e);
		}
	}

	public void stop() throws MBeanException, InstanceNotFoundException, ReflectionException, IOException {
		this.mBeanServerConnection.invoke(this.objectName, "shutdown", null, null);
	}

	private ObjectName toObjectName(String name) {
		try {
			return new ObjectName(name);
		}
		catch (MalformedObjectNameException ex) {
			throw new IllegalArgumentException("Invalid jmx name '" + name + "'");
		}
	}

}
