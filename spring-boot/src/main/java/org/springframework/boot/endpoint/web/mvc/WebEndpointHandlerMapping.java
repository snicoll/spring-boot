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

package org.springframework.boot.endpoint.web.mvc;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.endpoint.web.WebEndpointOperationArgumentResolver;
import org.springframework.boot.endpoint.web.WebEndpointOperationInfo;
import org.springframework.boot.endpoint.web.WebEndpointResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.ProducesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

/**
 * A custom {@link RequestMappingInfoHandlerMapping} that makes web endpoints available
 * over HTTP using Spring MVC.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class WebEndpointHandlerMapping extends RequestMappingInfoHandlerMapping
		implements InitializingBean {

	private final WebEndpointOperationArgumentResolver argumentResolver = new WebEndpointOperationArgumentResolver();

	private final Method handle = ReflectionUtils.findMethod(OperationHandler.class,
			"handle", HttpServletRequest.class, Map.class);

	private final List<EndpointInfo<WebEndpointOperationInfo>> webEndpoints;

	private final ApplicationContext applicationContext;

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param webEndpoints the web endpoints
	 * @param applicationContext the application context used to look up endpoint beans
	 */
	public WebEndpointHandlerMapping(
			List<EndpointInfo<WebEndpointOperationInfo>> webEndpoints,
			ApplicationContext applicationContext) {
		setOrder(-100);
		this.webEndpoints = webEndpoints;
		this.applicationContext = applicationContext;
	}

	@Override
	protected void initHandlerMethods() {
		this.webEndpoints.stream()
				.flatMap((webEndpoint) -> webEndpoint.getOperations().stream())
				.forEach(this::registerMappingForOperation);
	}

	private void registerMappingForOperation(WebEndpointOperationInfo operation) {
		registerMapping(createRequestMappingInfo(operation),
				new OperationHandler(
						this.applicationContext.getBean(operation.getBeanName()),
						operation.getOperationMethod()),
				this.handle);
	}

	private RequestMappingInfo createRequestMappingInfo(
			WebEndpointOperationInfo operationInfo) {
		OperationRequestPredicate requestPredicate = operationInfo.getRequestPredicate();
		return new RequestMappingInfo(null,
				new PatternsRequestCondition(requestPredicate.getPath()),
				new RequestMethodsRequestCondition(
						RequestMethod.valueOf(requestPredicate.getHttpMethod().name())),
				null, null,
				new ConsumesRequestCondition(
						toStringArray(requestPredicate.getConsumes())),
				new ProducesRequestCondition(
						toStringArray(requestPredicate.getProduces())),
				null);
	}

	private String[] toStringArray(Collection<String> collection) {
		return collection.toArray(new String[collection.size()]);
	}

	@Override
	protected boolean isHandler(Class<?> beanType) {
		return false;
	}

	@Override
	protected RequestMappingInfo getMappingForMethod(Method method,
			Class<?> handlerType) {
		return null;
	}

	/**
	 * A handler for an endpoint operation.
	 */
	final class OperationHandler {

		private final Object endpoint;

		private final Method operation;

		OperationHandler(Object endpoint, Method operation) {
			this.endpoint = endpoint;
			this.operation = operation;
		}

		@SuppressWarnings("unchecked")
		@ResponseBody
		public Object handle(HttpServletRequest request,
				@RequestBody(required = false) Map<String, String> body) {
			Map<String, String> arguments = new HashMap<>((Map<String, String>) request
					.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
			if (body != null) {
				arguments.putAll(body);
			}
			return handleResult(
					ReflectionUtils.invokeMethod(this.operation, this.endpoint,
							WebEndpointHandlerMapping.this.argumentResolver
									.resolveArguments(this.operation,
											(parameter) -> arguments.get(parameter))));
		}

		private Object handleResult(Object result) {
			if (!(result instanceof WebEndpointResponse)) {
				return result;
			}
			WebEndpointResponse<?> response = (WebEndpointResponse<?>) result;
			return new ResponseEntity<Object>(response.getBody(),
					HttpStatus.valueOf(response.getStatus()));
		}

	}

}
