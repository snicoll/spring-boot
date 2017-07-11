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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.EndpointDiscoverer;
import org.springframework.boot.endpoint.EndpointDiscoverer.EndpointOperationFactory;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.EndpointOperationType;
import org.springframework.boot.endpoint.ReflectiveOperationInvoker;
import org.springframework.boot.endpoint.Selector;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.ClassUtils;

/**
 * Discovers the {@link WebEndpoint web endpoints} in an {@link ApplicationContext}. Web
 * endpoints include all {@link Endpoint standard endpoints} and any {@link WebEndpoint
 * web-specific} additions and overrides.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class WebEndpointDiscoverer {

	private final EndpointDiscoverer endpointDiscoverer;

	private final WebEndpointOperationFactory operationFactory;

	/**
	 * Creates a new {@link WebEndpointDiscoverer} that will discover {@link WebEndpoint
	 * web endpoints} using the given {@code endpointDiscoverer}.
	 *
	 * @param endpointDiscoverer the endpoint discoverer
	 * @param conversionService the conversion service used to convert arguments when an
	 * operation is invoked
	 * @param basePath the path to prepend to the path of each discovered operation
	 * @param consumedMediaTypes the media types consumed by web endpoint operations
	 * @param producedMediaTypes the media types produced by web endpoint operations
	 */
	public WebEndpointDiscoverer(EndpointDiscoverer endpointDiscoverer,
			ConversionService conversionService, String basePath,
			Collection<String> consumedMediaTypes,
			Collection<String> producedMediaTypes) {
		this.endpointDiscoverer = endpointDiscoverer;
		this.operationFactory = new WebEndpointOperationFactory(conversionService,
				basePath, consumedMediaTypes, producedMediaTypes);
	}

	/**
	 * Perform endpoint discovery.
	 * @return the discovered endpoints
	 */
	public Collection<EndpointInfo<WebEndpointOperation>> discoverEndpoints() {
		Collection<EndpointInfo<WebEndpointOperation>> baseEndpoints = this.endpointDiscoverer
				.discoverEndpoints(Endpoint.class, this.operationFactory);
		verifyThatOperationsHaveDistinctPredicates(baseEndpoints);
		Collection<EndpointInfo<WebEndpointOperation>> overridingEndpoints = this.endpointDiscoverer
				.discoverEndpoints(WebEndpoint.class, this.operationFactory);
		verifyThatOperationsHaveDistinctPredicates(overridingEndpoints);
		return merge(baseEndpoints, overridingEndpoints);
	}

	private void verifyThatOperationsHaveDistinctPredicates(
			Collection<EndpointInfo<WebEndpointOperation>> endpoints) {
		Map<OperationRequestPredicate, List<WebEndpointOperation>> operations = new HashMap<>();
		endpoints.forEach((endpoint) -> {
			endpoint.getOperations().forEach((operation) -> {
				operations.merge(operation.getRequestPredicate(),
						Arrays.asList(operation), (existingOperations, newOperations) -> {
					List<WebEndpointOperation> combined = new ArrayList<>(
							existingOperations);
					combined.addAll(newOperations);
					return combined;
				});
			});
		});
		List<List<WebEndpointOperation>> clashes = operations.values().stream().filter(
				(operationsWithSamePredicate) -> operationsWithSamePredicate.size() > 1)
				.collect(Collectors.toList());
		if (clashes.isEmpty()) {
			return;
		}
		StringBuilder message = new StringBuilder(
				"Found multiple web operations with matching request predicates:");
		clashes.forEach((clash) -> {
			message.append("    " + clash.get(0).getRequestPredicate() + ":");
			clash.forEach((operation) -> {
				message.append("        " + operation);
			});
		});
		throw new IllegalStateException(message.toString());
	}

	/**
	 * Merges two lists of {@link EndpointInfo EndpointInfos} into one. When a base
	 * endpoint has the same id as an overriding endpoint, an operation on the overriding
	 * endpoint will override an operation on the base endpoint with the same type.
	 * @param baseEndpoints the base endpoints
	 * @param overridingEndpoints the overriding endpoints
	 * @return the merged list of endpoints
	 */
	private Collection<EndpointInfo<WebEndpointOperation>> merge(
			Collection<EndpointInfo<WebEndpointOperation>> baseEndpoints,
			Collection<EndpointInfo<WebEndpointOperation>> overridingEndpoints) {
		Map<String, EndpointInfo<WebEndpointOperation>> endpointsById = new HashMap<>();
		for (EndpointInfo<WebEndpointOperation> baseEndpoint : baseEndpoints) {
			endpointsById.put(baseEndpoint.getId(), baseEndpoint);
		}
		for (EndpointInfo<WebEndpointOperation> webEndpoint : overridingEndpoints) {
			endpointsById.merge(webEndpoint.getId(), webEndpoint, this::merge);
		}
		return Collections.unmodifiableCollection(endpointsById.values());
	}

	/**
	 * Merges two {@link EndpointInfo EndpointInfos} into a single {@code EndpointInfo}.
	 * When the two endpoints have an operation with the same
	 * {@link OperationRequestPredicate request predicate}, the operation on the
	 * {@code baseEndpoint} is overridden by the operation on the
	 * {@code overridingEndpoint}.
	 * @param baseEndpoint the base endpoint
	 * @param overridingEndpoint the overriding endpoint
	 * @return the merged endpoint
	 */
	private EndpointInfo<WebEndpointOperation> merge(
			EndpointInfo<WebEndpointOperation> baseEndpoint,
			EndpointInfo<WebEndpointOperation> overridingEndpoint) {
		Map<OperationRequestPredicate, WebEndpointOperation> operations = new HashMap<>();
		Consumer<WebEndpointOperation> operationConsumer = (operation) -> operations
				.put(operation.getRequestPredicate(), operation);
		baseEndpoint.getOperations().forEach(operationConsumer);
		overridingEndpoint.getOperations().forEach(operationConsumer);
		return new EndpointInfo<WebEndpointOperation>(baseEndpoint.getId(),
				operations.values());
	}

	private static final class WebEndpointOperationFactory
			implements EndpointOperationFactory<WebEndpointOperation> {

		private static boolean REACTIVE_STREAMS_PRESENT = ClassUtils.isPresent(
				"org.reactivestreams.Publisher",
				WebEndpointOperationFactory.class.getClassLoader());

		private final ConversionService conversionService;

		private final String basePath;

		private final Collection<String> consumedMediaTypes;

		private final Collection<String> producedMediaTypes;

		private WebEndpointOperationFactory(ConversionService conversionService,
				String basePath, Collection<String> consumedMediaTypes,
				Collection<String> producedMediaTypes) {
			this.conversionService = conversionService;
			this.basePath = normalizeBasePath(basePath);
			this.consumedMediaTypes = consumedMediaTypes;
			this.producedMediaTypes = producedMediaTypes;
		}

		private static String normalizeBasePath(String basePath) {
			if (!basePath.startsWith("/")) {
				basePath = "/" + basePath;
			}
			if (!basePath.endsWith("/")) {
				basePath = basePath + "/";
			}
			return basePath;
		}

		@Override
		public WebEndpointOperation createOperation(
				AnnotationAttributes endpointAttributes,
				AnnotationAttributes operationAttributes, Object target, Method method,
				EndpointOperationType type) {
			OperationRequestPredicate requestPredicate = new OperationRequestPredicate(
					determinePath(endpointAttributes.getString("id"), method),
					determineHttpMethod(type), determineConsumedMediaTypes(method),
					this.producedMediaTypes);
			WebEndpointOperation operation = new WebEndpointOperation(type,
					new ReflectiveOperationInvoker(this.conversionService, target,
							method),
					determineBlocking(method), requestPredicate);
			return operation;
		}

		private String determinePath(String endpointId, Method operationMethod) {
			StringBuilder path = new StringBuilder(this.basePath + endpointId);
			Stream.of(operationMethod.getParameters()).filter((parameter) -> {
				return parameter.getAnnotation(Selector.class) != null;
			}).map((parameter) -> "/{" + parameter.getName() + "}").forEach(path::append);
			return path.toString();
		}

		private Collection<String> determineConsumedMediaTypes(Method method) {
			return consumesRequestBody(method) ? this.consumedMediaTypes
					: Collections.emptyList();
		}

		private boolean consumesRequestBody(Method method) {
			return Stream.of(method.getParameters()).anyMatch(
					(parameter) -> parameter.getAnnotation(Selector.class) == null);
		}

		private WebEndpointHttpMethod determineHttpMethod(
				EndpointOperationType operationType) {
			if (operationType == EndpointOperationType.WRITE) {
				return WebEndpointHttpMethod.POST;
			}
			return WebEndpointHttpMethod.GET;
		}

		private boolean determineBlocking(Method method) {
			if (REACTIVE_STREAMS_PRESENT) {
				return !Publisher.class.isAssignableFrom(method.getReturnType());
			}
			else {
				return true;
			}
		}

	}

}
