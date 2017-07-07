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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.Resource.Builder;

import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.OperationInvoker;
import org.springframework.boot.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.endpoint.web.WebEndpointOperation;
import org.springframework.boot.endpoint.web.WebEndpointResponse;
import org.springframework.util.CollectionUtils;

/**
 * A factory for creating Jersey {@link Resource Resources} for web endpoint operations.
 *
 * @author Andy Wilkinson
 */
public class JerseyEndpointResourceFactory {

	/**
	 * Creates {@link Resource Resources} for the operations of the given
	 * {@code webEndpoints}.
	 * @param webEndpoints the web endpoints
	 * @return the resources for the operations
	 */
	public Collection<Resource> createEndpointResources(
			Collection<EndpointInfo<WebEndpointOperation>> webEndpoints) {
		return webEndpoints.stream()
				.flatMap((endpointInfo) -> endpointInfo.getOperations().stream())
				.map(this::createResource).collect(Collectors.toList());
	}

	private Resource createResource(WebEndpointOperation operation) {
		OperationRequestPredicate requestPredicate = operation.getRequestPredicate();
		Builder resourceBuilder = Resource.builder().path(requestPredicate.getPath());
		resourceBuilder.addMethod(requestPredicate.getHttpMethod().name())
				.consumes(toStringArray(requestPredicate.getConsumes()))
				.produces(toStringArray(requestPredicate.getProduces())).handledBy(
						new EndpointInvokingInflector(operation.getOperationInvoker()));
		return resourceBuilder.build();
	}

	private String[] toStringArray(Collection<String> collection) {
		return collection.toArray(new String[collection.size()]);
	}

	private static final class EndpointInvokingInflector
			implements Inflector<ContainerRequestContext, Object> {

		private final OperationInvoker operationInvoker;

		private EndpointInvokingInflector(OperationInvoker operationInvoker) {
			this.operationInvoker = operationInvoker;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object apply(ContainerRequestContext data) {
			Map<String, Object> body = ((ContainerRequest) data).readEntity(Map.class);
			Map<String, Object> arguments = body == null ? new HashMap<>()
					: new HashMap<>(body);
			arguments.putAll(extractPathParmeters(data));

			return convertIfNecessary(this.operationInvoker.invoke(arguments));
		}

		private Map<String, String> extractPathParmeters(
				ContainerRequestContext requestContext) {
			Map<String, String> pathParameters = new HashMap<>();
			requestContext.getUriInfo().getPathParameters().forEach((name, values) -> {
				if (!CollectionUtils.isEmpty(values)) {
					pathParameters.put(name, values.get(0));
				}
			});
			return pathParameters;
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
