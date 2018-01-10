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

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebMvcEndpointFilter}.
 *
 * @author Phillip Webb
 */
public class WebMvcEndpointFilterTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private WebMvcEndpointFilter filter = new WebMvcEndpointFilter();

	@Test
	public void matchWhenDiscovererIsWebAnnotationEndpointDiscovererShouldReturnTrue() {
		EndpointInfo<WebOperation> info = new EndpointInfo<>("foo", true,
				Collections.emptyList());
		WebAnnotationEndpointDiscoverer discoverer = mock(
				WebAnnotationEndpointDiscoverer.class);
		assertThat(this.filter.match(info, discoverer)).isTrue();
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void matchWhenDiscovererIsNotWebAnnotationEndpointDiscovererShouldReturnFalse() {
		EndpointInfo<WebOperation> info = new EndpointInfo<>("foo", true,
				Collections.emptyList());
		EndpointDiscoverer discoverer = mock(EndpointDiscoverer.class);
		assertThat(this.filter.match(info, discoverer)).isFalse();
	}

	@Test
	public void matchWhenHasWebOperationsShouldThrowException() {
		WebOperation operation = mock(WebOperation.class);
		EndpointInfo<WebOperation> info = new EndpointInfo<>("foo", true,
				Collections.singleton(operation));
		WebAnnotationEndpointDiscoverer discoverer = mock(
				WebAnnotationEndpointDiscoverer.class);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Invalid @WebMvcEndpoint: operations are not supported"
				+ ", use only standard Spring MVC annotations (e.g. @RequestMapping");
		this.filter.match(info, discoverer);
	}

}
