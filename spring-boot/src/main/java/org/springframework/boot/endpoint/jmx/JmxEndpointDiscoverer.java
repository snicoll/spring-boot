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

import java.lang.annotation.Annotation;
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

import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.EndpointDiscoverer;
import org.springframework.boot.endpoint.EndpointDiscoverer.EndpointOperationFactory;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.EndpointOperationType;
import org.springframework.boot.endpoint.ReflectiveOperationInvoker;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.convert.ConversionService;
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
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class JmxEndpointDiscoverer {

	private static final AnnotationJmxAttributeSource jmxAttributeSource = new AnnotationJmxAttributeSource();

	private final JmxEndpointOperationFactory operationFactory;

	private final EndpointDiscoverer endpointDiscoverer;

	/**
	 * Creates a new {@link JmxEndpointDiscoverer} that will discover {@link JmxEndpoint
	 * jmx endpoints} using the given {@code endpointDiscoverer}.
	 *
	 * @param endpointDiscoverer the endpoint discoverer
	 * @param conversionService the conversion service used to convert arguments when an
	 * operation is invoked
	 */
	public JmxEndpointDiscoverer(EndpointDiscoverer endpointDiscoverer,
			ConversionService conversionService) {
		this.endpointDiscoverer = endpointDiscoverer;
		this.operationFactory = new JmxEndpointOperationFactory(conversionService);
	}

	/**
	 * Perform endpoint discovery.
	 * @return the discovered endpoints
	 */
	public Collection<EndpointInfo<JmxEndpointOperation>> discoverEndpoints() {
		Collection<EndpointInfo<JmxEndpointOperation>> baseEndpoints = discoverEndpoints(
				Endpoint.class);
		Collection<EndpointInfo<JmxEndpointOperation>> overridingEndpoints = discoverEndpoints(
				JmxEndpoint.class);
		return merge(baseEndpoints, overridingEndpoints);
	}

	private Collection<EndpointInfo<JmxEndpointOperation>> discoverEndpoints(
			Class<? extends Annotation> endpointType) {
		Collection<EndpointInfo<JmxEndpointOperation>> endpoints = this.endpointDiscoverer
				.discoverEndpoints(endpointType, this.operationFactory);
		for (EndpointInfo<JmxEndpointOperation> endpoint : endpoints) {
			Map<String, JmxEndpointOperation> operations = new HashMap<>();
			for (JmxEndpointOperation operation : endpoint.getOperations()) {
				JmxEndpointOperation existing = operations
						.put(operation.getOperationName(), operation);
				if (existing != null) {
					throw new IllegalStateException(String.format(
							"Found two operations named '%s' for endpoint with id '%s'",
							operation.getOperationName(), endpoint.getId()));
				}
			}
		}
		return endpoints;
	}

	private Collection<EndpointInfo<JmxEndpointOperation>> merge(
			Collection<EndpointInfo<JmxEndpointOperation>> baseEndpoints,
			Collection<EndpointInfo<JmxEndpointOperation>> overridingEndpoints) {
		Map<String, EndpointInfo<JmxEndpointOperation>> endpointsById = new HashMap<>();
		for (EndpointInfo<JmxEndpointOperation> standardEndpoint : baseEndpoints) {
			endpointsById.put(standardEndpoint.getId(), standardEndpoint);
		}
		for (EndpointInfo<JmxEndpointOperation> webEndpoint : overridingEndpoints) {
			endpointsById.merge(webEndpoint.getId(), webEndpoint, this::merge);
		}
		return Collections.unmodifiableCollection(endpointsById.values());
	}

	/**
	 * Merges two {@link EndpointInfo EndpointInfos} into a single {@code EndpointInfo}.
	 * When the two endpoints have an operation with the same name, the operation on the
	 * {@code baseEndpoint} is overridden by the operation on the
	 * {@code overridingEndpoint}.
	 * @param baseEndpoint the base endpoint
	 * @param overridingEndpoint the overriding endpoint
	 * @return the merged endpoint
	 */
	private EndpointInfo<JmxEndpointOperation> merge(
			EndpointInfo<JmxEndpointOperation> baseEndpoint,
			EndpointInfo<JmxEndpointOperation> overridingEndpoint) {
		Map<String, JmxEndpointOperation> operations = new HashMap<>();
		Consumer<? super JmxEndpointOperation> operationConsumer = (
				operation) -> operations.put(operation.getOperationName(), operation);
		baseEndpoint.getOperations().forEach(operationConsumer);
		overridingEndpoint.getOperations().forEach(operationConsumer);
		return new EndpointInfo<>(baseEndpoint.getId(),
				overridingEndpoint.isEnabledByDefault(), operations.values());
	}

	private static class JmxEndpointOperationFactory
			implements EndpointOperationFactory<JmxEndpointOperation> {

		private final ConversionService conversionService;

		JmxEndpointOperationFactory(ConversionService conversionService) {
			this.conversionService = conversionService;
		}

		@Override
		public JmxEndpointOperation createOperation(
				AnnotationAttributes endpointAttributes,
				AnnotationAttributes operationAttributes, Object target, Method method,
				EndpointOperationType type) {
			String operationName = method.getName();
			Class<?> outputType = mapParameterType(method.getReturnType());
			String description = getDescription(method, () -> "Invoke " + operationName
					+ " for endpoint " + endpointAttributes.getString("id"));
			List<JmxEndpointOperationParameterInfo> parameters = getParameters(method);
			return new JmxEndpointOperation(type,
					new ReflectiveOperationInvoker(this.conversionService, target,
							method),
					operationName, outputType, description, parameters);
		}

		private static String getDescription(Method method, Supplier<String> fallback) {
			ManagedOperation managedOperation = jmxAttributeSource
					.getManagedOperation(method);
			if (managedOperation != null
					&& StringUtils.hasText(managedOperation.getDescription())) {
				return managedOperation.getDescription();
			}
			return fallback.get();
		}

		private static List<JmxEndpointOperationParameterInfo> getParameters(
				Method method) {
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
					parameters.add(
							new JmxEndpointOperationParameterInfo(parameter.getName(),
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

}
