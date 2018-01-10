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

package org.springframework.boot.actuate.endpoint.annotation;

import java.util.Collection;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.util.Assert;

/**
 * {@link EndpointInfo} derived from an annotated object. Provides access to the original
 * source to allow further processing if needed.
 *
 * @param <T> The operation type
 * @author Phillip Webb
 * @since 2.0.0
 */
public class AnnotatedEndpointInfo<T extends Operation> extends EndpointInfo<T> {

	private final Object source;

	public AnnotatedEndpointInfo(Object source, String id, boolean enableByDefault,
			Collection<T> operations) {
		super(id, enableByDefault, operations);
		Assert.notNull(source, "Source must not be null");
		this.source = source;
	}

	/**
	 * Return the annotated source object that was used to derive the endpoint info.
	 * @return the annotated source object
	 */
	public Object getSource() {
		return this.source;
	}

	/**
	 * Create a copy of this endpoint with a different set of operations.
	 * @param operations the new operations
	 * @return a new {@link AnnotatedEndpointInfo} instance
	 */
	AnnotatedEndpointInfo<T> withNewOperations(Collection<T> operations) {
		return new AnnotatedEndpointInfo<>(getSource(), getId(), isEnableByDefault(),
				operations);
	}

}
