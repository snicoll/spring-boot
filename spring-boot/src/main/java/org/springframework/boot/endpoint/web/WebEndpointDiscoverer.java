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

import org.springframework.boot.endpoint.AbstractEndpointDiscoverer;
import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.EndpointDiscoverer;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.EndpointOperationInfo;
import org.springframework.boot.endpoint.EndpointOperationType;
import org.springframework.boot.endpoint.Selector;
import org.springframework.context.ApplicationContext;

/**
 * Discovers the {@link WebEndpoint web endpoints} in an {@link ApplicationContext}. Web
 * endpoints include all {@link Endpoint standard endpoints} and any {@link WebEndpoint
 * web-specific} additions and overrides.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class WebEndpointDiscoverer
		extends AbstractEndpointDiscoverer<WebEndpointOperationInfo> {

	private final EndpointDiscoverer endpointDiscoverer;

	private final Collection<String> consumedMediaTypes;

	private final Collection<String> producedMediaTypes;

	/**
	 * Creates a new {@link WebEndpointDiscoverer} that will discover {@link WebEndpoint
	 * web endpoints} in the given {@code applicationContext}.
	 *
	 * @param endpointDiscoverer the discoverer for standard endpoints that will be
	 * combined with any web-specific endpoints
	 * @param applicationContext the application context
	 * @param consumedMediaTypes the media types consumed by web endpoint operations
	 * @param producedMediaTypes the media types produced by web endpoint operations
	 */
	public WebEndpointDiscoverer(EndpointDiscoverer endpointDiscoverer,
			ApplicationContext applicationContext, Collection<String> consumedMediaTypes,
			Collection<String> producedMediaTypes) {
		super(WebEndpoint.class, applicationContext,
				(endpointAttributes, operationAttributes, beanName, method, type) -> {
					OperationRequestPredicate requestPredicate = new OperationRequestPredicate(
							createPath(endpointAttributes.getString("id"), method),
							getHttpMethod(type), consumedMediaTypes, producedMediaTypes);
					WebEndpointOperationInfo operationInfo = new WebEndpointOperationInfo(
							beanName, method, type, requestPredicate);
					return operationInfo;
				});
		this.endpointDiscoverer = endpointDiscoverer;
		this.consumedMediaTypes = consumedMediaTypes;
		this.producedMediaTypes = producedMediaTypes;
	}

	@Override
	public Collection<EndpointInfo<WebEndpointOperationInfo>> discoverEndpoints() {
		List<EndpointInfo<WebEndpointOperationInfo>> baseEndpoints = this.endpointDiscoverer
				.discoverEndpoints().stream().map(this::convert)
				.collect(Collectors.toList());
		verifyThatOperationsHaveDistinctPredicates(baseEndpoints);
		Collection<EndpointInfo<WebEndpointOperationInfo>> webEndpoints = super.discoverEndpoints();
		verifyThatOperationsHaveDistinctPredicates(webEndpoints);
		return merge(baseEndpoints, webEndpoints);
	}

	private void verifyThatOperationsHaveDistinctPredicates(
			Collection<EndpointInfo<WebEndpointOperationInfo>> endpoints) {
		Map<OperationRequestPredicate, List<WebEndpointOperationInfo>> operations = new HashMap<>();
		endpoints.forEach((endpoint) -> {
			endpoint.getOperations().forEach((operation) -> {
				operations.merge(operation.getRequestPredicate(),
						Arrays.asList(operation), (existingOperations, newOperations) -> {
					List<WebEndpointOperationInfo> combined = new ArrayList<>(
							existingOperations);
					combined.addAll(newOperations);
					return combined;
				});
			});
		});
		List<List<WebEndpointOperationInfo>> clashes = operations.values().stream()
				.filter((operationsWithSamePredicate) -> operationsWithSamePredicate
						.size() > 1)
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
	private Collection<EndpointInfo<WebEndpointOperationInfo>> merge(
			Collection<EndpointInfo<WebEndpointOperationInfo>> baseEndpoints,
			Collection<EndpointInfo<WebEndpointOperationInfo>> overridingEndpoints) {
		Map<String, EndpointInfo<WebEndpointOperationInfo>> endpointsById = new HashMap<>();
		for (EndpointInfo<WebEndpointOperationInfo> baseEndpoint : baseEndpoints) {
			endpointsById.put(baseEndpoint.getId(), baseEndpoint);
		}
		for (EndpointInfo<WebEndpointOperationInfo> webEndpoint : overridingEndpoints) {
			endpointsById.merge(webEndpoint.getId(), webEndpoint, this::merge);
		}
		return Collections.unmodifiableCollection(endpointsById.values());
	}

	/**
	 * Convert an {@link EndpointOperationInfo} into a {@link WebEndpointOperationInfo}.
	 * @param endpointInfo the {@code EndpointOperationInfo} to convert
	 * @return the {@code WebEndpointOperationInfo}
	 */
	private EndpointInfo<WebEndpointOperationInfo> convert(
			EndpointInfo<EndpointOperationInfo> endpointInfo) {
		List<WebEndpointOperationInfo> webOperations = endpointInfo.getOperations()
				.stream()
				.map((operationInfo) -> new WebEndpointOperationInfo(operationInfo,
						createRequestPredicate(endpointInfo, operationInfo)))
				.collect(Collectors.toList());
		return new EndpointInfo<WebEndpointOperationInfo>(endpointInfo.getId(),
				webOperations);
	}

	private OperationRequestPredicate createRequestPredicate(
			EndpointInfo<EndpointOperationInfo> endpointInfo,
			EndpointOperationInfo operationInfo) {
		OperationRequestPredicate requestPredicate = new OperationRequestPredicate(
				createPath(endpointInfo.getId(), operationInfo.getOperationMethod()),
				getHttpMethod(operationInfo.getType()), this.consumedMediaTypes,
				this.producedMediaTypes);
		return requestPredicate;
	}

	private static String createPath(String endpointId, Method operationMethod) {
		StringBuilder path = new StringBuilder("/" + endpointId);
		Stream.of(operationMethod.getParameters()).filter((parameter) -> {
			return parameter.getAnnotation(Selector.class) != null;
		}).map((parameter) -> "/{" + parameter.getName() + "}").forEach(path::append);
		return path.toString();
	}

	private static WebEndpointHttpMethod getHttpMethod(
			EndpointOperationType operationType) {
		if (operationType == EndpointOperationType.WRITE) {
			return WebEndpointHttpMethod.POST;
		}
		return WebEndpointHttpMethod.GET;
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
	private EndpointInfo<WebEndpointOperationInfo> merge(
			EndpointInfo<WebEndpointOperationInfo> baseEndpoint,
			EndpointInfo<WebEndpointOperationInfo> overridingEndpoint) {
		Map<OperationRequestPredicate, WebEndpointOperationInfo> operations = new HashMap<>();
		Consumer<WebEndpointOperationInfo> operationConsumer = (operation) -> operations
				.put(operation.getRequestPredicate(), operation);
		baseEndpoint.getOperations().forEach(operationConsumer);
		overridingEndpoint.getOperations().forEach(operationConsumer);
		return new EndpointInfo<WebEndpointOperationInfo>(baseEndpoint.getId(),
				operations.values());
	}

}
