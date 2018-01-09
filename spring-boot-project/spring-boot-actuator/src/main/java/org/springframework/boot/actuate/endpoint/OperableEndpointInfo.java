/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Collection;

import org.springframework.util.Assert;

/**
 * An {@link EndpointInfo} that exposes its {@link Operation operations}.
 *
 * @param <T> the type of the endpoint's operations
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class OperableEndpointInfo<T extends Operation> extends EndpointInfo {

	private final Collection<T> operations;

	/**
	 * Creates a new {@code EndpointInfo} describing an endpoint with the given {@code id}
	 * and {@code operations}.
	 * @param id the id of the endpoint
	 * @param enableByDefault if the endpoint is enabled by default
	 * @param operations the operations of the endpoint
	 */
	public OperableEndpointInfo(String id, boolean enableByDefault, Collection<T> operations) {
		super(id, enableByDefault);
		Assert.notNull(operations, "Operations must not be null");
		this.operations = operations;
	}

	/**
	 * Returns the operations of the endpoint.
	 * @return the operations
	 */
	public Collection<T> getOperations() {
		return this.operations;
	}

}
