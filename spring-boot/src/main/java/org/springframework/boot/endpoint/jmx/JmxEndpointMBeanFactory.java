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

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.endpoint.EndpointInfo;

/**
 * A factory for creating JMX MBeans for web endpoint operations.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class JmxEndpointMBeanFactory {

	private final EndpointMBeanInfoAssembler assembler = new EndpointMBeanInfoAssembler();

	private final Function<Object, Object> operationResponseConverter;

	/**
	 * Create a new {@link JmxEndpointMBeanFactory} instance that will use the given
	 * {@code endpointResponseConverter} to convert an operation's response to a
	 * JMX-friendly form. The given {@code beanFactory} is used to look up the bean that
	 * implements an operation.
	 * @param operationResponseConverter the response converter
	 */
	public JmxEndpointMBeanFactory(Function<Object, Object> operationResponseConverter) {
		this.operationResponseConverter = operationResponseConverter;
	}

	/**
	 * Creates MBeans for the given {@code endpoints}.
	 *
	 * @param endpoints the endpoints
	 * @return the MBeans
	 */
	public Collection<EndpointDynamicMBean> createMBeans(
			Collection<EndpointInfo<JmxEndpointOperation>> endpoints) {
		return endpoints.stream().map((endpointInfo) -> {
			EndpointMBeanInfo endpointMBeanInfo = this.assembler
					.createEndpointMBeanInfo(endpointInfo);
			return new EndpointDynamicMBean(this.operationResponseConverter,
					endpointMBeanInfo);
		}).collect(Collectors.toList());
	}

}
