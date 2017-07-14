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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.reactivestreams.Publisher;

import org.springframework.boot.endpoint.AnnotationEndpointDiscoverer;
import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.EndpointOperationType;
import org.springframework.boot.endpoint.EndpointType;
import org.springframework.boot.endpoint.ReflectiveOperationInvoker;
import org.springframework.boot.endpoint.Selector;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.convert.ConversionService;
import org.springframework.util.ClassUtils;

/**
 * Discovers the {@link Endpoint endpoints} in an {@link ApplicationContext} with
 * {@link WebEndpointExtension web extensions} applied to them.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class WebAnnotationEndpointDiscoverer extends
		AnnotationEndpointDiscoverer<WebEndpointOperation, OperationRequestPredicate> {

	/**
	 * Creates a new {@link WebAnnotationEndpointDiscoverer} that will discover
	 * {@link Endpoint endpoints} and {@link WebEndpointExtension web extensions} using
	 * the given {@link ApplicationContext}.
	 * @param applicationContext the application context
	 * @param conversionService the conversion service used to convert arguments when an
	 * operation is invoked
	 * @param basePath the path to prepend to the path of each discovered operation
	 * @param consumedMediaTypes the media types consumed by web endpoint operations
	 * @param producedMediaTypes the media types produced by web endpoint operations
	 */
	public WebAnnotationEndpointDiscoverer(ApplicationContext applicationContext,
			ConversionService conversionService, String basePath,
			Collection<String> consumedMediaTypes,
			Collection<String> producedMediaTypes) {
		super(applicationContext,
				new WebEndpointOperationFactory(conversionService, basePath,
						consumedMediaTypes, producedMediaTypes),
				WebEndpointOperation::getRequestPredicate);
	}

	@Override
	public Collection<EndpointInfo<WebEndpointOperation>> discoverEndpoints() {
		Collection<EndpointInfoDescriptor<WebEndpointOperation, OperationRequestPredicate>> endpoints = discoverEndpointsWithExtension(
				WebEndpointExtension.class, EndpointType.WEB);
		verifyThatOperationsHaveDistinctPredicates(endpoints);
		return endpoints.stream().map(EndpointInfoDescriptor::getEndpointInfo)
				.collect(Collectors.toList());
	}

	private void verifyThatOperationsHaveDistinctPredicates(
			Collection<EndpointInfoDescriptor<WebEndpointOperation, OperationRequestPredicate>> endpointDescriptors) {
		List<List<WebEndpointOperation>> clashes = new ArrayList<>();
		endpointDescriptors.forEach(descriptor -> {
			clashes.addAll(descriptor.findDuplicateOperations().values());
		});
		if (!clashes.isEmpty()) {
			StringBuilder message = new StringBuilder();
			message.append(String.format(
					"Found multiple web operations with matching request predicates:%n"));
			clashes.forEach((clash) -> {
				message.append("    ").append(clash.get(0).getRequestPredicate())
						.append(String.format(":%n"));
				clash.forEach((operation) -> {
					message.append("        ").append(String.format("%s%n", operation));
				});
			});
			throw new IllegalStateException(message.toString());
		}
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
		public WebEndpointOperation createOperation(String endpointId,
				AnnotationAttributes operationAttributes, Object target, Method method,
				EndpointOperationType type) {
			WebEndpointHttpMethod httpMethod = determineHttpMethod(type);
			OperationRequestPredicate requestPredicate = new OperationRequestPredicate(
					determinePath(endpointId, method), httpMethod,
					determineConsumedMediaTypes(httpMethod, method),
					this.producedMediaTypes);
			return new WebEndpointOperation(type,
					new ReflectiveOperationInvoker(this.conversionService, target,
							method),
					determineBlocking(method), requestPredicate);
		}

		private String determinePath(String endpointId, Method operationMethod) {
			StringBuilder path = new StringBuilder(this.basePath + endpointId);
			Stream.of(operationMethod.getParameters()).filter((parameter) -> {
				return parameter.getAnnotation(Selector.class) != null;
			}).map((parameter) -> "/{" + parameter.getName() + "}").forEach(path::append);
			return path.toString();
		}

		private Collection<String> determineConsumedMediaTypes(
				WebEndpointHttpMethod httpMethod, Method method) {
			if (WebEndpointHttpMethod.POST == httpMethod && consumesRequestBody(method)) {
				return this.consumedMediaTypes;
			}
			return Collections.emptyList();
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
			return !REACTIVE_STREAMS_PRESENT
					|| !Publisher.class.isAssignableFrom(method.getReturnType());
		}

	}

}
