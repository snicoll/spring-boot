/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.util.List;

import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * Configure {@link RestTemplate} with sensible defaults.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class RestTemplateConfigurer {

	private final HttpMessageConverters httpMessageConverters;

	public RestTemplateConfigurer(HttpMessageConverters httpMessageConverters) {
		this.httpMessageConverters = httpMessageConverters;
	}

	/**
	 * Configures the specified {@link RestTemplate} with the state of this instance.
	 * @param restTemplate the rest template to configure
	 * @see RestTemplate#setMessageConverters(List)
	 */
	public void configure(RestTemplate restTemplate) {
		Assert.notNull(restTemplate, "RestOperations must not be null");
		restTemplate.setMessageConverters(this.httpMessageConverters.getConverters());
	}

}
