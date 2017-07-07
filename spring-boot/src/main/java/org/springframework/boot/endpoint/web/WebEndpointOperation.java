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

import org.springframework.boot.endpoint.EndpointOperation;
import org.springframework.boot.endpoint.EndpointOperationType;
import org.springframework.boot.endpoint.OperationInvoker;

/**
 * An operation on a web endpoint.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class WebEndpointOperation extends EndpointOperation {

	private final OperationRequestPredicate requestPredicate;

	/**
	 * Creates a new {@code WebEndpointOperation} with the given {@code type}. The
	 * operation can be performed using the given {@code operationInvoker}. The operation
	 * can handle requests that match the given {@code requestPredicate}.
	 * @param type the type of the operation
	 * @param operationInvoker used to perform the operation
	 * @param blocking whether or not this is a blocking operation
	 * @param requestPredicate the predicate for requests that can be handled by the
	 * operation
	 */
	public WebEndpointOperation(EndpointOperationType type,
			OperationInvoker operationInvoker, boolean blocking,
			OperationRequestPredicate requestPredicate) {
		super(type, operationInvoker, blocking);
		this.requestPredicate = requestPredicate;
	}

	/**
	 * Returns the predicate for requests that can be handled by this operation.
	 * @return the predicate
	 */
	public OperationRequestPredicate getRequestPredicate() {
		return this.requestPredicate;
	}

}
