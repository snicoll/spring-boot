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

package org.springframework.boot.actuate.endpoint.webflux;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.condition.PatternsRequestCondition;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

/**
 * {@link HandlerMapping} to map {@link Endpoint}s to URLs via {@link Endpoint#getId()}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class ReactiveEndpointHandlerMapping extends RequestMappingHandlerMapping {

	private final Set<WebFluxEndpoint> endpoints;

	private String prefix = "";

	private boolean disabled = false;

	public ReactiveEndpointHandlerMapping(Set<WebFluxEndpoint> endpoints) {
		this.endpoints = endpoints;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (!this.disabled) {
			for (WebFluxEndpoint endpoint : this.endpoints) {
				detectHandlerMethods(endpoint);
			}
		}
	}

	/**
	 * Since all handler beans are passed into the constructor there is no need to detect
	 * anything here.
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		return false;
	}

	@Override
	protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
		if (mapping == null) {
			return;
		}
		String[] patterns = getPatterns(handler, mapping);
		if (!ObjectUtils.isEmpty(patterns)) {
			super.registerHandlerMethod(handler, method,
					withNewPatterns(mapping, patterns));
		}
	}

	private String[] getPatterns(Object handler, RequestMappingInfo mapping) {
		if (handler instanceof String) {
			handler = getApplicationContext().getBean((String) handler);
		}
		Assert.state(handler instanceof MvcEndpoint, "Only MvcEndpoints are supported");
		String path = getPath((WebFluxEndpoint) handler);
		return (path == null ? null : getEndpointPatterns(path, mapping));
	}


	/**
	 * Return the path that should be used to map the given {@link MvcEndpoint}.
	 * @param endpoint the endpoint to map
	 * @return the path to use for the endpoint or {@code null} if no mapping is required
	 */
	protected String getPath(WebFluxEndpoint endpoint) {
		return endpoint.getPath();
	}

	private String[] getEndpointPatterns(String path, RequestMappingInfo mapping) {
		String patternPrefix = StringUtils.hasText(this.prefix) ? this.prefix + path
				: path;
		Set<PathPattern> defaultPatterns = mapping.getPatternsCondition().getPatterns();
		if (defaultPatterns.isEmpty()) {
			return new String[] { patternPrefix, patternPrefix + ".json" };
		}
		List<String> patterns = defaultPatterns.stream()
				.map(PathPattern::getPatternString)
				.collect(Collectors.toList());
		for (int i = 0; i < patterns.size(); i++) {
			patterns.set(i, patternPrefix + patterns.get(i));
		}
		return patterns.toArray(new String[patterns.size()]);
	}

	private RequestMappingInfo withNewPatterns(RequestMappingInfo mapping,
			String[] patternStrings) {
		PatternsRequestCondition patterns = new PatternsRequestCondition(patternStrings);
		return new RequestMappingInfo(patterns, mapping.getMethodsCondition(),
				mapping.getParamsCondition(), mapping.getHeadersCondition(),
				mapping.getConsumesCondition(), mapping.getProducesCondition(),
				mapping.getCustomCondition());
	}


	/**
	 * Set the prefix used in mappings.
	 * @param prefix the prefix
	 */
	public void setPrefix(String prefix) {
		Assert.isTrue("".equals(prefix) || StringUtils.startsWithIgnoreCase(prefix, "/"),
				"prefix must start with '/'");
		this.prefix = prefix;
	}

	/**
	 * Get the prefix used in mappings.
	 * @return the prefix
	 */
	public String getPrefix() {
		return this.prefix;
	}

	/**
	 * Get the path of the endpoint.
	 * @param endpoint the endpoint
	 * @return the path used in mappings
	 */
	public String getPath(String endpoint) {
		return this.prefix + endpoint;
	}

	/**
	 * Sets if this mapping is disabled.
	 * @param disabled if the mapping is disabled
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/**
	 * Returns if this mapping is disabled.
	 * @return {@code true} if the mapping is disabled
	 */
	public boolean isDisabled() {
		return this.disabled;
	}

}
