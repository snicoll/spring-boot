/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.http.client.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientHttpServiceGroupConfigurer;

/**
 * A {@link RestClientHttpServiceGroupConfigurer} to apply auto-configured
 * {@link RestClientCustomizer} beans to the group's {@link RestClient}.
 *
 * @author Olga Maciaszek-Sharma
 * @author Phillip Webb
 */
class RestClientCustomizerHttpServiceGroupConfigurer implements RestClientHttpServiceGroupConfigurer {

	/**
	 * Allow user defined configurers to apply before / after ours.
	 */
	private static final int ORDER = 0;

	private final ObjectProvider<RestClientCustomizer> customizers;

	RestClientCustomizerHttpServiceGroupConfigurer(ObjectProvider<RestClientCustomizer> customizers) {
		this.customizers = customizers;
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	public void configureGroups(Groups<RestClient.Builder> groups) {
		groups.configureClient(this::configureClient);
	}

	private void configureClient(RestClient.Builder builder) {
		this.customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
	}

}
