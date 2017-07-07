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

import org.springframework.context.ApplicationContext;

/**
 * An {@link AbstractEndpointDiscoverer} that discovers {@link Endpoint} beans.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class EndpointDiscoverer
		extends AbstractEndpointDiscoverer<EndpointOperationInfo> {

	/**
	 * Creates a new {@link EndpointDiscoverer} that will discover {@link Endpoint}
	 * annotated beans in the given {@code applicationContext}.
	 * @param applicationContext the applicationContext
	 */
	public EndpointDiscoverer(ApplicationContext applicationContext) {
		super(Endpoint.class, applicationContext,
				(endpointAttributes, operationAttributes, beanName, method, type) -> {
					return new EndpointOperationInfo(beanName, method, type);
				});
	}

}
