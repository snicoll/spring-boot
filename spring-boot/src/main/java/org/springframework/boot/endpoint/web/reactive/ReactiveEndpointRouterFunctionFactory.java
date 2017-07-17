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

package org.springframework.boot.endpoint.web.reactive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.EndpointOperation;
import org.springframework.boot.endpoint.OperationInvoker;
import org.springframework.boot.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.endpoint.web.WebEndpointOperation;
import org.springframework.boot.endpoint.web.WebEndpointResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.ServerResponse.BodyBuilder;

/**
 * A factory for creating a {@link RouterFunction} for web endpoint operations.
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class ReactiveEndpointRouterFunctionFactory {

	private final Scheduler scheduler = Schedulers.elastic();

	/**
	 * Creates a {@link RouterFunction} for the given {@code webEndpoints}.
	 * @param webEndpoints the web endpoints
	 * @return the {@code RouterFunction} for the endpoints
	 */
	public RouterFunction<ServerResponse> createRouterFunction(
			Collection<EndpointInfo<WebEndpointOperation>> webEndpoints) {
		List<RouterFunction<ServerResponse>> routes = new ArrayList<>();
		for (EndpointInfo<WebEndpointOperation> endpointInfo : webEndpoints) {
			for (WebEndpointOperation operationInfo : endpointInfo.getOperations()) {
				EndpointHandler handler = createEndpointHandler(operationInfo);
				routes.add(RouterFunctions.route(predicate(endpointInfo, operationInfo),
						handler::apply));
			}
		}
		// TODO: empty router rather than null?
		return routes.stream().reduce(RouterFunction::and).orElse(null);
	}

	private RequestPredicate predicate(EndpointInfo<WebEndpointOperation> endpointInfo,
			WebEndpointOperation operation) {
		OperationRequestPredicate operationRequestPredicate = operation
				.getRequestPredicate();
		RequestPredicate requestPredicate = RequestPredicates.method(
				HttpMethod.valueOf(operationRequestPredicate.getHttpMethod().name()));
		if (!CollectionUtils.isEmpty(operationRequestPredicate.getConsumes())) {
			requestPredicate = requestPredicate.and(RequestPredicates
					.contentType(toMediaTypes(operationRequestPredicate.getConsumes())));
		}
		if (!CollectionUtils.isEmpty(operationRequestPredicate.getProduces())) {
			requestPredicate = requestPredicate.and(RequestPredicates
					.accept(toMediaTypes(operationRequestPredicate.getProduces())));
		}
		return requestPredicate
				.and((RequestPredicates.path(operationRequestPredicate.getPath())));
	}

	private MediaType[] toMediaTypes(Collection<String> collection) {
		List<MediaType> mediaTypeList = collection.stream().map(MediaType::parseMediaType)
				.collect(Collectors.toList());
		return mediaTypeList.toArray(new MediaType[mediaTypeList.size()]);
	}

	private EndpointHandler createEndpointHandler(EndpointOperation operation) {
		if (!operation.isBlocking()) {
			return new EndpointHandler(operation.getOperationInvoker());
		}
		return new EndpointHandler(arguments -> {
			return Mono.create((sink) -> {
				ReactiveEndpointRouterFunctionFactory.this.scheduler.schedule(() -> {
					try {
						sink.success(operation.getOperationInvoker().invoke(arguments));
					}
					catch (Exception ex) {
						sink.error(ex);
					}
				});
			});
		});
	}

	private static class EndpointHandler {

		private static final ParameterizedTypeReference<Map<String, String>> BODY_TYPE = new ParameterizedTypeReference<Map<String, String>>() {

		};

		private final OperationInvoker operationInvoker;

		EndpointHandler(OperationInvoker operationInvoker) {
			this.operationInvoker = operationInvoker;
		}

		public Mono<ServerResponse> apply(ServerRequest serverRequest) {
			return invoke(serverRequest)
					.switchIfEmpty(Mono.just(new WebEndpointResponse<>(
							serverRequest.method() == HttpMethod.GET
									? HttpStatus.NOT_FOUND.value()
									: HttpStatus.NO_CONTENT.value())))
					.map((entity) -> entity instanceof WebEndpointResponse
							? (WebEndpointResponse<?>) entity
							: new WebEndpointResponse<>(entity))
					.flatMap((entity) -> {
						BodyBuilder bodyBuilder = ServerResponse
								.status(HttpStatus.valueOf(entity.getStatus()));
						return (entity.getBody() == null) ? bodyBuilder.build()
								: bodyBuilder.syncBody(entity.getBody());
					});
		}

		@SuppressWarnings("unchecked")
		private Mono<Object> invoke(ServerRequest request) {
			// TODO Better handling of an empty request body
			Mono<Map<String, String>> mapBody = request
					.body(BodyExtractors.toMono(BODY_TYPE));
			if (!request.headers().contentType().isPresent()) {
				mapBody = mapBody.onErrorResume(ex -> Mono.just(Collections.emptyMap()));
			}
			return mapBody.switchIfEmpty(Mono.just(Collections.emptyMap()))
					.flatMap((body) -> {
						Object result = this.operationInvoker.invoke(endpointArguments(
								request.pathVariables(), request.queryParams(), body));
						return result instanceof Mono ? (Mono<Object>) result
								: Mono.just(result);
					});
		}

		private Map<String, Object> endpointArguments(Map<String, String> pathVariables,
				MultiValueMap<String, String> queryParameters, Map<String, String> body) {
			Map<String, Object> arguments = new HashMap<>(pathVariables);
			arguments.putAll(body);
			queryParameters.forEach((name, values) ->
					arguments.put(name, values.size() == 1 ? values.get(0) : values));
			return arguments;
		}

	}

}
