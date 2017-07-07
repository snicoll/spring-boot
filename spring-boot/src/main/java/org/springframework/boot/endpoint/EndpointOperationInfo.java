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

package org.springframework.boot.endpoint;

import java.lang.reflect.Method;

import org.springframework.core.style.ToStringCreator;
import org.springframework.util.ReflectionUtils;

/**
 * Information about an operation on an endpoint.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class EndpointOperationInfo {

	private final String beanName;

	private final Method operationMethod;

	private final EndpointOperationType type;

	/**
	 * Creates a new {@code EndpointOperationInfo} that describes an endpoint on the bean
	 * with the given {@code beanName}. The operation can be performed by invoking the
	 * given {@method} and has the given {@code type}.
	 * @param beanName the name of the bean
	 * @param method the method for the operation
	 * @param type the type of the operation
	 */
	public EndpointOperationInfo(String beanName, Method method,
			EndpointOperationType type) {
		this.beanName = beanName;
		this.operationMethod = method;
		ReflectionUtils.makeAccessible(method);
		this.type = type;
	}

	/**
	 * Returns the name of the bean that provides the operation.
	 * @return the bean name
	 */
	public String getBeanName() {
		return this.beanName;
	}

	/**
	 * Returns the {@link Method} that can be called to perform the operation.
	 * @return the method
	 */
	public Method getOperationMethod() {
		return this.operationMethod;
	}

	/**
	 * Returns the {@link EndpointOperationType type} of the operation.
	 * @return the type
	 */
	public EndpointOperationType getType() {
		return this.type;
	}

	@Override
	public String toString() {
		return new ToStringCreator(this).append("operationMethod", this.operationMethod)
				.toString();
	}

}
