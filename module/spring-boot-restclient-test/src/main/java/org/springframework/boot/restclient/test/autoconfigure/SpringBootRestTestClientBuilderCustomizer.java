/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.restclient.test.autoconfigure;

import java.util.Collection;

import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.test.web.servlet.client.RestTestClientBuilderCustomizer;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * {@link RestTestClientBuilderCustomizer} for a typical Spring Boot application.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class SpringBootRestTestClientBuilderCustomizer implements RestTestClientBuilderCustomizer {

	private final Collection<ClientHttpMessageConvertersCustomizer> messageConvertersCustomizers;

	/**
	 * Create a new {@code SpringBootWebTestClientBuilderCustomizer} that will configure
	 * the builder's codecs using the given {@code codecCustomizers}.
	 * @param messageConvertersCustomizers the codec customizers
	 */
	public SpringBootRestTestClientBuilderCustomizer(
			Collection<ClientHttpMessageConvertersCustomizer> messageConvertersCustomizers) {
		this.messageConvertersCustomizers = messageConvertersCustomizers;
	}

	@Override
	public void customize(RestTestClient.Builder<?> webClientBuilder) {
		webClientBuilder.configureMessageConverters(
				(builder) -> this.messageConvertersCustomizers.forEach((customizer) -> customizer.customize(builder)));
	}

}
