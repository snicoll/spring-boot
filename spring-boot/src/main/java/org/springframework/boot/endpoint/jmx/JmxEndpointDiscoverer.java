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
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.EndpointDiscoverer;
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
 * Discovers the {@link Endpoint endpoints} in an {@link ApplicationContext} with
 * {@link JmxEndpointExtension JMX extensions} applied to them.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class JmxEndpointDiscoverer
		extends EndpointDiscoverer<JmxEndpointOperation, String> {

	private static final AnnotationJmxAttributeSource jmxAttributeSource = new AnnotationJmxAttributeSource();

	/**
	 * Creates a new {@link JmxEndpointDiscoverer} that will discover {@link Endpoint
	 * endpoints} and {@link JmxEndpointExtension jmx extensions} using the given
	 * {@link ApplicationContext}.
	 *
	 * @param applicationContext the application context
	 * @param conversionService the conversion service used to convert arguments when an
	 * operation is invoked
	 */
	public JmxEndpointDiscoverer(ApplicationContext applicationContext,
			ConversionService conversionService) {
		super(applicationContext, new JmxEndpointOperationFactory(conversionService),
				JmxEndpointOperation::getOperationName);
	}

	@Override
	public Collection<EndpointInfo<JmxEndpointOperation>> discoverEndpoints() {
		Collection<EndpointInfoDescriptor<JmxEndpointOperation, String>> endpointDescriptors = discoverEndpointsWithExtension(
				JmxEndpointExtension.class);
		verifyThatOperationsHaveDistinctName(endpointDescriptors);
		return endpointDescriptors.stream().map(EndpointInfoDescriptor::getEndpointInfo)
				.collect(Collectors.toList());
	}

	private void verifyThatOperationsHaveDistinctName(
			Collection<EndpointInfoDescriptor<JmxEndpointOperation, String>> endpointDescriptors) {
		List<List<JmxEndpointOperation>> clashes = new ArrayList<>();
		endpointDescriptors.forEach(descriptor -> {
			clashes.addAll(descriptor.findDuplicateOperations().values());
		});
		if (!clashes.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append(
					String.format("Found multiple JMX operations with the same name:%n"));
			clashes.forEach((clash) -> {
				message.append("    ").append(clash.get(0).getOperationName())
						.append(String.format(":%n"));
				clash.forEach((operation) -> {
					message.append("        ").append(String.format("%s%n", operation));
				});
			});
			throw new IllegalStateException(message.toString());
		}
	}

	private static class JmxEndpointOperationFactory
			implements EndpointOperationFactory<JmxEndpointOperation> {

		private final ConversionService conversionService;

		JmxEndpointOperationFactory(ConversionService conversionService) {
			this.conversionService = conversionService;
		}

		@Override
		public JmxEndpointOperation createOperation(String endpointId,
				AnnotationAttributes operationAttributes, Object target, Method method,
				EndpointOperationType type) {
			String operationName = method.getName();
			Class<?> outputType = mapParameterType(method.getReturnType());
			String description = getDescription(method,
					() -> "Invoke " + operationName + " for endpoint " + endpointId);
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
