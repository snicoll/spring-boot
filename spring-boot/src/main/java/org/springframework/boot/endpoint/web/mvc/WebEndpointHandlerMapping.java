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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.OperationInvoker;
import org.springframework.boot.endpoint.web.OperationRequestPredicate;
import org.springframework.boot.endpoint.web.WebEndpointOperation;
import org.springframework.boot.endpoint.web.WebEndpointResponse;
import org.springframework.http.HttpMethod;
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

	private final Method handle = ReflectionUtils.findMethod(OperationHandler.class,
			"handle", HttpServletRequest.class, Map.class);

	private final Collection<EndpointInfo<WebEndpointOperation>> webEndpoints;

	/**
	 * Creates a new {@code WebEndpointHandlerMapping} that provides mappings for the
	 * operations of the given {@code webEndpoints}.
	 * @param collection the web endpoints
	 */
	public WebEndpointHandlerMapping(
			Collection<EndpointInfo<WebEndpointOperation>> collection) {
		setOrder(-100);
		this.webEndpoints = collection;
	}

	@Override
	protected void initHandlerMethods() {
		this.webEndpoints.stream()
				.flatMap((webEndpoint) -> webEndpoint.getOperations().stream())
				.forEach(this::registerMappingForOperation);
	}

	private void registerMappingForOperation(WebEndpointOperation operation) {
		registerMapping(createRequestMappingInfo(operation),
				new OperationHandler(operation.getOperationInvoker()), this.handle);
	}

	private RequestMappingInfo createRequestMappingInfo(
			WebEndpointOperation operationInfo) {
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

		private final OperationInvoker operationInvoker;

		OperationHandler(OperationInvoker operationInvoker) {
			this.operationInvoker = operationInvoker;
		}

		@SuppressWarnings("unchecked")
		@ResponseBody
		public Object handle(HttpServletRequest request,
				@RequestBody(required = false) Map<String, String> body) {
			Map<String, Object> arguments = new HashMap<>((Map<String, String>) request
					.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE));
			if (body != null
					&& HttpMethod.POST == HttpMethod.valueOf(request.getMethod())) {
				arguments.putAll(body);
			}
			request.getParameterMap().forEach((name, values) -> {
				arguments.put(name,
						values.length == 1 ? values[0] : Arrays.asList(values));
			});
			return handleResult(this.operationInvoker.invoke(arguments));
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
