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

package org.springframework.boot.actuate.endpoint.web.servlet.annotation;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.annotation.AnnotatedEndpointInfo;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;
import org.springframework.web.accept.PathExtensionContentNegotiationStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * {@link HandlerMapping} that exposes {@link WebMvcEndpoint @WebMvcEndpoint} annotated
 * endpoints over Spring MVC.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public class WebMvcEndpointRequestMappingHandlerMapping
		extends RequestMappingHandlerMapping {

	private final EndpointMapping endpointMapping;

	private final EndpointPathResolver endpointPathResolver;

	private final Set<Object> handlers;

	private final CorsConfiguration corsConfiguration;

	/**
	 * Create a new {@link WebMvcEndpointRequestMappingHandlerMapping} instance providing
	 * mappings for the specified endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpointPathResolver the endpoint path resolver
	 * @param endpoints the web endpoints operations
	 */
	public WebMvcEndpointRequestMappingHandlerMapping(EndpointMapping endpointMapping,
			EndpointPathResolver endpointPathResolver,
			Collection<EndpointInfo<WebOperation>> endpoints) {
		this(endpointMapping, endpointPathResolver, endpoints, null);
	}

	/**
	 * Create a new {@link WebMvcEndpointRequestMappingHandlerMapping} instance providing
	 * mappings for the specified endpoints.
	 * @param endpointMapping the base mapping for all endpoints
	 * @param endpointPathResolver the endpoint path resolver
	 * @param endpoints the web endpoints operations
	 * @param corsConfiguration the CORS configuration for the endpoints or {@code null}
	 */
	public WebMvcEndpointRequestMappingHandlerMapping(EndpointMapping endpointMapping,
			EndpointPathResolver endpointPathResolver,
			Collection<EndpointInfo<WebOperation>> endpoints,
			CorsConfiguration corsConfiguration) {
		Assert.notNull(endpointMapping, "EndpointMapping must not be null");
		Assert.notNull(endpointPathResolver, "EndpointPathResolver must not be null");
		Assert.notNull(endpoints, "Endpoints must not be null");
		this.endpointMapping = endpointMapping;
		this.endpointPathResolver = endpointPathResolver;
		this.handlers = endpoints.stream().filter(AnnotatedEndpointInfo.class::isInstance)
				.map(this::getSource).filter(this::isWebMvcEndpoint)
				.collect(Collectors.toSet());
		this.corsConfiguration = corsConfiguration;
		setOrder(-100);
		setUseSuffixPatternMatch(false);
	}

	private Object getSource(EndpointInfo<?> info) {
		return ((AnnotatedEndpointInfo<?>) info).getSource();
	}

	private boolean isWebMvcEndpoint(Object source) {
		return AnnotatedElementUtils.hasAnnotation(source.getClass(),
				WebMvcEndpoint.class);
	}

	@Override
	protected void initHandlerMethods() {
		getHandlers().forEach(this::detectHandlerMethods);
	}

	public Set<Object> getHandlers() {
		return this.handlers;
	}

	@Override
	protected void registerHandlerMethod(Object handler, Method method,
			RequestMappingInfo mapping) {
		mapping = updateMapping(handler, mapping);
		super.registerHandlerMethod(handler, method, withEndpointMappedPath(mapping));
	}

	private RequestMappingInfo updateMapping(Object handler, RequestMappingInfo mapping) {
		String[] subPathPatterns = mapping.getPatternsCondition().getPatterns().stream()
				.map(this.endpointMapping::createSubPath).toArray(String[]::new);
		return withNewPatterns(mapping, subPathPatterns);
	}

	private String getEndpointMappedPath(String pattern) {
		this.endpointMapping.endpointMapping.createSubPath(path);

	}

	private RequestMappingInfo withNewPatterns(RequestMappingInfo mapping,
			String[] patterns) {
		PatternsRequestCondition patternsCondition = new PatternsRequestCondition(
				patterns, null, null, useSuffixPatternMatch(), useTrailingSlashMatch(),
				null);
		return new RequestMappingInfo(patternsCondition, mapping.getMethodsCondition(),
				mapping.getParamsCondition(), mapping.getHeadersCondition(),
				mapping.getConsumesCondition(), mapping.getProducesCondition(),
				mapping.getCustomCondition());
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method,
			RequestMappingInfo mapping) {
		return this.corsConfiguration;
	}

	@Override
	protected void extendInterceptors(List<Object> interceptors) {
		interceptors.add(new SkipPathExtensionContentNegotiation());
	}

	/**
	 * {@link HandlerInterceptorAdapter} to ensure that
	 * {@link PathExtensionContentNegotiationStrategy} is skipped for web endpoints.
	 */
	private static final class SkipPathExtensionContentNegotiation
			extends HandlerInterceptorAdapter {

		private static final String SKIP_ATTRIBUTE = PathExtensionContentNegotiationStrategy.class
				.getName() + ".SKIP";

		@Override
		public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
				Object handler) throws Exception {
			request.setAttribute(SKIP_ATTRIBUTE, Boolean.TRUE);
			return true;
		}

	}

}
