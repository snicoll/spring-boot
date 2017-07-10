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

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.boot.endpoint.EndpointOperationInfo;
import org.springframework.boot.endpoint.EndpointOperationType;

/**
 * Information describing an operation on a jmx endpoint.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class JmxEndpointOperationInfo extends EndpointOperationInfo {

	private final String operationName;

	private final Class<?> getOutputType;

	private final String description;

	private final List<JmxEndpointOperationParameterInfo> parameters;

	public JmxEndpointOperationInfo(String beanName, Method method,
			EndpointOperationType type, String operationName, Class<?> getOutputType,
			String description, List<JmxEndpointOperationParameterInfo> parameters) {
		super(beanName, method, type);
		this.operationName = operationName;
		this.getOutputType = getOutputType;
		this.description = description;
		this.parameters = parameters;
	}

	public String getOperationName() {
		return this.operationName;
	}

	public Class<?> getGetOutputType() {
		return this.getOutputType;
	}

	public String getDescription() {
		return this.description;
	}

	public List<JmxEndpointOperationParameterInfo> getParameters() {
		return this.parameters;
	}

}
