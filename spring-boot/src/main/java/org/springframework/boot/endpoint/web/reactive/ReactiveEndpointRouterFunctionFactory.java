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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.EndpointOperationInfo;
import org.springframework.boot.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.endpoint.web.WebEndpointOperationArgumentResolver;
import org.springframework.boot.endpoint.web.WebEndpointOperationInfo;
import org.springframework.boot.endpoint.web.WebEndpointResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.ReflectionUtils;
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

	private final ApplicationContext applicationContext;

	private final Scheduler scheduler = Schedulers.elastic();

	public ReactiveEndpointRouterFunctionFactory(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Creates a {@link RouterFunction} for the given {@code webEndpoints}.
	 * @param webEndpoints the web endpoints
	 * @return the {@code RouterFunction} for the endpoints
	 */
	public RouterFunction<ServerResponse> createRouterFunction(
			Collection<EndpointInfo<WebEndpointOperationInfo>> webEndpoints) {
		List<RouterFunction<ServerResponse>> routes = new ArrayList<>();
		for (EndpointInfo<WebEndpointOperationInfo> endpointInfo : webEndpoints) {
			for (WebEndpointOperationInfo operationInfo : endpointInfo.getOperations()) {
				EndpointHandler handler = createEndpointHandler(operationInfo);
				routes.add(RouterFunctions.route(predicate(endpointInfo, operationInfo),
						handler::apply));
			}
		}
		// TODO: empty router rather than null?
		return routes.stream().reduce(RouterFunction::and).orElse(null);
	}

	private RequestPredicate predicate(
			EndpointInfo<WebEndpointOperationInfo> endpointInfo,
			WebEndpointOperationInfo operation) {
		OperationRequestPredicate requestPredicate = operation.getRequestPredicate();
		return RequestPredicates
				.method(HttpMethod.valueOf(requestPredicate.getHttpMethod().name()))
				.and(RequestPredicates
						.contentType(toMediaTypes(requestPredicate.getConsumes())))
				.and(RequestPredicates
						.accept(toMediaTypes(requestPredicate.getProduces())))
				.and((RequestPredicates.path(requestPredicate.getPath())));
	}

	private MediaType[] toMediaTypes(Collection<String> collection) {
		List<MediaType> mediaTypeList = collection.stream().map(MediaType::parseMediaType)
				.collect(Collectors.toList());
		return mediaTypeList.toArray(new MediaType[mediaTypeList.size()]);
	}

	private EndpointHandler createEndpointHandler(EndpointOperationInfo operation) {
		Object bean = this.applicationContext.getBean(operation.getBeanName());
		Method method = operation.getOperationMethod();
		ResolvableType returnType = ResolvableType.forMethodReturnType(method);
		ResolvableType generic = returnType.as(Publisher.class).getGeneric(0);
		ReflectiveInvoker reflectiveInvoker = new ReflectiveInvoker(method, bean);
		if (generic != ResolvableType.NONE) {
			return new EndpointHandler(reflectiveInvoker);
		}
		return new EndpointHandler((input) -> {
			return Mono.create((sink) -> {
				this.scheduler.schedule(() -> {
					try {
						sink.success(reflectiveInvoker.apply(input));
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

		private final Function<Map<String, String>, Object> invoker;

		EndpointHandler(Function<Map<String, String>, Object> invoker) {
			this.invoker = invoker;
		}

		public Mono<ServerResponse> apply(ServerRequest serverRequest) {
			return invoke(serverRequest)
					.switchIfEmpty(Mono.just(new WebEndpointResponse<>(null)))
					.map((entity) -> entity instanceof WebEndpointResponse
							? (WebEndpointResponse<?>) entity
							: new WebEndpointResponse<Object>(entity))
					.flatMap((entity) -> {
						BodyBuilder bodyBuilder = ServerResponse
								.status(HttpStatus.valueOf(entity.getStatus()));
						return (entity.getBody() == null) ? bodyBuilder.build()
								: bodyBuilder.syncBody(entity.getBody());
					});
		}

		@SuppressWarnings("unchecked")
		private Mono<Object> invoke(ServerRequest request) {
			Mono<Map<String, String>> bodyMono = request.headers().contentLength()
					.isPresent() ? request.body(BodyExtractors.toMono(BODY_TYPE))
							: Mono.just(Collections.emptyMap());
			return bodyMono.flatMap((body) -> {
				Object result = this.invoker
						.apply(endpointArguments(request.pathVariables(), body));
				return result instanceof Mono ? (Mono<Object>) result : Mono.just(result);
			});
		}

		private Map<String, String> endpointArguments(Map<String, String> pathVariables,
				Map<String, String> body) {
			Map<String, String> arguments = new HashMap<>(pathVariables);
			arguments.putAll(body);
			return arguments;
		}

	}

	private static final class ReflectiveInvoker
			implements Function<Map<String, String>, Object> {

		private final WebEndpointOperationArgumentResolver argumentResolver = new WebEndpointOperationArgumentResolver();

		private final Method method;

		private final Object bean;

		private ReflectiveInvoker(Method method, Object bean) {
			this.method = method;
			this.bean = bean;
		}

		@Override
		public Object apply(Map<String, String> input) {
			return ReflectionUtils.invokeMethod(this.method, this.bean,
					this.argumentResolver.resolveArguments(this.method,
							(parameter) -> input.get(parameter)));
		}

	}

}
