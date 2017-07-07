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

package org.springframework.boot.endpoint.web;

import java.lang.reflect.Method;

import org.springframework.boot.endpoint.EndpointOperationInfo;
import org.springframework.boot.endpoint.EndpointOperationType;

/**
 * Information describing an operation on a web endpoint.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class WebEndpointOperationInfo extends EndpointOperationInfo {

	private final OperationRequestPredicate requestPredicate;

	/**
	 * Creates a new {@code WebEndpointOperationInfo} that describes an operation on the
	 * bean with the given {@code beanName}. The operation can be performed by invoking
	 * the given {@method} and has the given {@code type}.
	 * @param beanName the name of the bean
	 * @param method the method for the operation
	 * @param type the type of the operation
	 * @param requestPredicate the predicate for requests that can be handled by the
	 * operation
	 */
	public WebEndpointOperationInfo(String beanName, Method method,
			EndpointOperationType type, OperationRequestPredicate requestPredicate) {
		super(beanName, method, type);
		this.requestPredicate = requestPredicate;
	}

	WebEndpointOperationInfo(EndpointOperationInfo operationInfo,
			OperationRequestPredicate requestPredicate) {
		this(operationInfo.getBeanName(), operationInfo.getOperationMethod(),
				operationInfo.getType(), requestPredicate);
	}

	/**
	 * Returns the predicate for requests that can be handled by this operation.
	 * @return the predicate
	 */
	public OperationRequestPredicate getRequestPredicate() {
		return this.requestPredicate;
	}

}
