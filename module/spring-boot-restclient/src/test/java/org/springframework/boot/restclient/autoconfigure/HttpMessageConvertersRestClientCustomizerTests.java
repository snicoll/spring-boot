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

package org.springframework.boot.restclient.autoconfigure;

import java.util.List;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;

import org.springframework.boot.http.converter.autoconfigure.HttpMessageConverters;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpMessageConvertersRestClientCustomizer}
 *
 * @author Phillip Webb
 */
class HttpMessageConvertersRestClientCustomizerTests {

	@Test
	void createWhenNullMessageConvertersArrayThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new HttpMessageConvertersRestClientCustomizer((HttpMessageConverter<?>[]) null))
			.withMessage("'messageConverters' must not be null");
	}

	@Test
	void createWhenNullMessageConvertersDoesNotCustomize() {
		assertThatCustomizedConverters(new HttpMessageConvertersRestClientCustomizer((HttpMessageConverters) null))
			.containsExactlyElementsOf(defaultConverters());
	}

	@Test
	void customizeConfiguresMessageConverters() {
		HttpMessageConverter<?> c1 = mock();
		HttpMessageConverter<?> c2 = mock();
		assertThatCustomizedConverters(new HttpMessageConvertersRestClientCustomizer(c1, c2)).containsExactly(c1, c2);
	}

	@SuppressWarnings("rawtypes")
	private ListAssert<HttpMessageConverter> assertThatCustomizedConverters(
			HttpMessageConvertersRestClientCustomizer customizer) {
		Builder restClientBuilder = createRestClientBuilder(customizer);
		return assertThat(restClientBuilder).extracting("messageConverters")
			.asInstanceOf(InstanceOfAssertFactories.list(HttpMessageConverter.class));
	}

	@SuppressWarnings("unchecked")
	private List<HttpMessageConverter<?>> defaultConverters() {
		return (List<HttpMessageConverter<?>>) ReflectionTestUtils.getField(RestClient.builder().build(),
				"messageConverters");
	}

	private RestClient.Builder createRestClientBuilder(HttpMessageConvertersRestClientCustomizer customizer) {
		RestClient.Builder restClientBuilder = RestClient.builder();
		customizer.customize(restClientBuilder);
		return restClientBuilder;
	}

}
