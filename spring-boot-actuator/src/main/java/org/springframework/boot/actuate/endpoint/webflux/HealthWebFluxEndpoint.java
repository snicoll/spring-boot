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

import java.util.HashMap;
import java.util.Map;

import reactor.core.publisher.Mono;

import org.springframework.boot.actuate.endpoint.ReactiveHealthEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.AbstractEndpointMvcAdapter;
import org.springframework.boot.actuate.endpoint.mvc.ActuatorGetMapping;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Adapter to expose {@link ReactiveHealthEndpoint} as an {@link WebFluxEndpoint}.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "endpoints.health")
public class HealthWebFluxEndpoint
		extends AbstractEndpointMvcAdapter<ReactiveHealthEndpoint>
		implements WebFluxEndpoint {

	private Map<String, HttpStatus> statusMapping = new HashMap<>();

	public HealthWebFluxEndpoint(ReactiveHealthEndpoint delegate) {
		super(delegate);
		setupDefaultStatusMapping();
	}

	private void setupDefaultStatusMapping() {
		addStatusMapping(Status.DOWN, HttpStatus.SERVICE_UNAVAILABLE);
		addStatusMapping(Status.OUT_OF_SERVICE, HttpStatus.SERVICE_UNAVAILABLE);
	}

	/**
	 * Add a status mapping to the existing set.
	 * @param status the status to map
	 * @param httpStatus the http status
	 */
	public void addStatusMapping(Status status, HttpStatus httpStatus) {
		Assert.notNull(status, "Status must not be null");
		Assert.notNull(httpStatus, "HttpStatus must not be null");
		addStatusMapping(status.getCode(), httpStatus);
	}

	/**
	 * Add a status mapping to the existing set.
	 * @param statusCode the status code to map
	 * @param httpStatus the http status
	 */
	public void addStatusMapping(String statusCode, HttpStatus httpStatus) {
		Assert.notNull(statusCode, "StatusCode must not be null");
		Assert.notNull(httpStatus, "HttpStatus must not be null");
		this.statusMapping.put(statusCode, httpStatus);
	}


	@ActuatorGetMapping
	@ResponseBody
	public Mono<Health> invoke() {
		// TODO need to get `Mono<Health>` and map it to get the http status
		// TODO need to give actual type so that webflux knows how to handle it
		return (Mono<Health>) super.invoke();

	}

	private HttpStatus getStatus(Health health) {
		String code = getUniformValue(health.getStatus().getCode());
		if (code != null) {
			return this.statusMapping.keySet().stream()
					.filter((key) -> code.equals(getUniformValue(key)))
					.map(this.statusMapping::get).findFirst().orElse(null);
		}
		return null;
	}


	private String getUniformValue(String code) {
		if (code == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (char ch : code.toCharArray()) {
			if (Character.isAlphabetic(ch) || Character.isDigit(ch)) {
				builder.append(Character.toLowerCase(ch));
			}
		}
		return builder.toString();
	}

}
