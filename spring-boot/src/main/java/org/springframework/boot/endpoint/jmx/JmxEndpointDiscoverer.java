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
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.boot.endpoint.AbstractEndpointDiscoverer;
import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.EndpointDiscoverer;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.EndpointOperationInfo;
import org.springframework.boot.endpoint.EndpointOperationType;
import org.springframework.context.ApplicationContext;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.metadata.ManagedOperation;
import org.springframework.jmx.export.metadata.ManagedOperationParameter;
import org.springframework.util.StringUtils;

/**
 * Discovers the {@link JmxEndpoint jmx endpoints} in an {@link ApplicationContext}. Jmx
 * endpoints include all {@link Endpoint standard endpoints} and any {@link JmxEndpoint
 * jmx-specific} additions and overrides.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class JmxEndpointDiscoverer
		extends AbstractEndpointDiscoverer<JmxEndpointOperationInfo> {

	private static final AnnotationJmxAttributeSource jmxAttributeSource = new AnnotationJmxAttributeSource();

	private final EndpointDiscoverer endpointDiscoverer;

	protected JmxEndpointDiscoverer(EndpointDiscoverer endpointDiscoverer,
			ApplicationContext applicationContext) {
		super(JmxEndpoint.class, applicationContext,
				(endpointAttributes, operationAttributes, beanName, method, type) ->
						createOperationInfo(endpointAttributes.getString("id"),
								beanName, method, type)
		);
		this.endpointDiscoverer = endpointDiscoverer;
	}

	@Override
	public Collection<EndpointInfo<JmxEndpointOperationInfo>> discoverEndpoints() {
		Collection<EndpointInfo<EndpointOperationInfo>> standardEndpoints =
				this.endpointDiscoverer.discoverEndpoints();
		Collection<EndpointInfo<JmxEndpointOperationInfo>> webEndpoints = super.discoverEndpoints();
		return merge(standardEndpoints.stream().map(this::convert)
				.collect(Collectors.toList()), webEndpoints);
	}

	private Collection<EndpointInfo<JmxEndpointOperationInfo>> merge(
			Collection<EndpointInfo<JmxEndpointOperationInfo>> baseEndpoints,
			Collection<EndpointInfo<JmxEndpointOperationInfo>> overridingEndpoints) {
		Map<String, EndpointInfo<JmxEndpointOperationInfo>> endpointsById = new HashMap<>();
		for (EndpointInfo<JmxEndpointOperationInfo> standardEndpoint : baseEndpoints) {
			endpointsById.put(standardEndpoint.getId(), standardEndpoint);
		}
		for (EndpointInfo<JmxEndpointOperationInfo> webEndpoint : overridingEndpoints) {
			endpointsById.merge(webEndpoint.getId(), webEndpoint, this::merge);
		}
		return Collections.unmodifiableCollection(endpointsById.values());
	}

	/**
	 * Convert an {@link EndpointOperationInfo} into a {@link JmxEndpointOperationInfo}.
	 * @param endpointInfo the {@code EndpointOperationInfo} to convert
	 * @return the {@code WebEndpointOperationInfo}
	 */
	private EndpointInfo<JmxEndpointOperationInfo> convert(
			EndpointInfo<EndpointOperationInfo> endpointInfo) {
		List<JmxEndpointOperationInfo> operations = endpointInfo.getOperations()
				.stream()
				.map(operationInfo -> createOperationInfo(endpointInfo.getId(),
						operationInfo.getBeanName(), operationInfo.getOperationMethod(),
						operationInfo.getType()))
				.collect(Collectors.toList());
		return new EndpointInfo<>(endpointInfo.getId(), operations);
	}

	/**
	 * Merges two {@link EndpointInfo EndpointInfos} into a single {@code EndpointInfo}.
	 * When the two endpoints have an operation with the same {@link EndpointOperationType
	 * type} and path, the operation on the {@code baseEndpoint} is overridden by the
	 * operation on the {@code overridingEndpoint}.
	 * @param baseEndpoint the base endpoint
	 * @param overridingEndpoint the overriding endpoint
	 * @return the merged endpoint
	 */
	private EndpointInfo<JmxEndpointOperationInfo> merge(
			EndpointInfo<JmxEndpointOperationInfo> baseEndpoint,
			EndpointInfo<JmxEndpointOperationInfo> overridingEndpoint) {
		Map<String, JmxEndpointOperationInfo> operations = new HashMap<>();
		Consumer<? super JmxEndpointOperationInfo> operationConsumer = (operation)
				-> operations.put(operation.getOperationName(), operation);
		baseEndpoint.getOperations().forEach(operationConsumer);
		overridingEndpoint.getOperations().forEach(operationConsumer);
		return new EndpointInfo<>(baseEndpoint.getId(), operations.values());
	}

	private static JmxEndpointOperationInfo createOperationInfo(String endpointId,
			String beanName, Method method, EndpointOperationType type) {
		String operationName = method.getName();
		Class<?> outputType = mapParameterType(method.getReturnType());
		String description = getDescription(method, ()
				-> "Invoke " + operationName + " for endpoint " + endpointId);
		List<JmxEndpointOperationParameterInfo> parameters = getParameters(method);
		return new JmxEndpointOperationInfo(beanName, method, type, operationName,
				outputType, description, parameters);
	}

	private static String getDescription(Method method, Supplier<String> fallback) {
		ManagedOperation managedOperation = jmxAttributeSource.getManagedOperation(method);
		if (managedOperation != null
				&& StringUtils.hasText(managedOperation.getDescription())) {
			return managedOperation.getDescription();
		}
		return fallback.get();
	}

	private static List<JmxEndpointOperationParameterInfo> getParameters(Method method) {
		List<JmxEndpointOperationParameterInfo> parameters = new ArrayList<>();
		Parameter[] methodParameters = method.getParameters();
		if (methodParameters.length == 0) {
			return parameters;
		}
		ManagedOperationParameter[] managedOperationParameters = jmxAttributeSource
				.getManagedOperationParameters(method);
		if (managedOperationParameters.length > 0) {
			for (int i = 0; i < managedOperationParameters.length; i++) {
				ManagedOperationParameter mBeanParameter = managedOperationParameters[i];
				Parameter methodParameter = methodParameters[i];
				parameters.add(new JmxEndpointOperationParameterInfo(
						mBeanParameter.getName(),
						mapParameterType(methodParameter.getType()),
						mBeanParameter.getDescription()));
			}
		}
		else {
			for (Parameter parameter : methodParameters) {
				parameters.add(new JmxEndpointOperationParameterInfo(parameter.getName(),
						mapParameterType(parameter.getType()), null));
			}
		}
		return parameters;
	}

	private static Class<?> mapParameterType(Class<?> parameter) {
		if (parameter.isEnum()) {
			return String.class;
		}
		if (parameter.getName().startsWith("java.")) {
			return parameter;
		}
		if (parameter.equals(Void.TYPE)) {
			return parameter;
		}
		return Object.class;
	}

}
