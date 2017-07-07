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

package org.springframework.boot.endpoint.web.jersey;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.Resource.Builder;

import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.endpoint.web.WebEndpointOperationArgumentResolver;
import org.springframework.boot.endpoint.web.WebEndpointOperationInfo;
import org.springframework.boot.endpoint.web.WebEndpointResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A factory for creating Jersey {@link Resource Resources} for web endpoint operations.
 *
 * @author Andy Wilkinson
 */
public class JerseyEndpointResourceFactory {

	private final ApplicationContext applicationContext;

	public JerseyEndpointResourceFactory(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Creates {@link Resource Resources} for the operations of the given
	 * {@code webEndpoints}.
	 * @param webEndpoints the web endpoints
	 * @return the resources for the operations
	 */
	public Collection<Resource> createEndpointResources(
			List<EndpointInfo<WebEndpointOperationInfo>> webEndpoints) {
		return webEndpoints.stream()
				.flatMap((endpointInfo) -> endpointInfo.getOperations().stream())
				.map(this::createResource).collect(Collectors.toList());
	}

	private Resource createResource(WebEndpointOperationInfo operationInfo) {
		OperationRequestPredicate requestPredicate = operationInfo.getRequestPredicate();
		Builder resourceBuilder = Resource.builder().path(requestPredicate.getPath());
		resourceBuilder.addMethod(requestPredicate.getHttpMethod().name())
				.consumes(toStringArray(requestPredicate.getConsumes()))
				.produces(toStringArray(requestPredicate.getProduces()))
				.handledBy(new EndpointInvokingInflector(
						this.applicationContext.getBean(operationInfo.getBeanName()),
						operationInfo.getOperationMethod()));
		return resourceBuilder.build();
	}

	private String[] toStringArray(Collection<String> collection) {
		return collection.toArray(new String[collection.size()]);
	}

	private static final class EndpointInvokingInflector
			implements Inflector<ContainerRequestContext, Object> {

		private final WebEndpointOperationArgumentResolver argumentResolver = new WebEndpointOperationArgumentResolver();

		private final Object bean;

		private final Method method;

		private EndpointInvokingInflector(Object bean, Method method) {
			this.bean = bean;
			this.method = method;
		}

		@Override
		public Object apply(ContainerRequestContext data) {
			MultivaluedMap<String, String> pathParameters = data.getUriInfo()
					.getPathParameters();
			Map<?, ?> body = ((ContainerRequest) data).readEntity(Map.class);
			return convertIfNecessary(ReflectionUtils.invokeMethod(this.method, this.bean,
					this.argumentResolver.resolveArguments(this.method, (selector) -> {
						List<String> values = pathParameters.get(selector);
						if (!CollectionUtils.isEmpty(values)) {
							return values.iterator().next();
						}
						return body.get(selector);
					})));
		}

		private Object convertIfNecessary(Object response) {
			if (!(response instanceof WebEndpointResponse)) {
				return Response.status(Status.OK).entity(response).build();
			}
			WebEndpointResponse<?> webEndpointResponse = (WebEndpointResponse<?>) response;
			return Response.status(webEndpointResponse.getStatus())
					.entity(webEndpointResponse.getBody()).build();
		}

	}

}
