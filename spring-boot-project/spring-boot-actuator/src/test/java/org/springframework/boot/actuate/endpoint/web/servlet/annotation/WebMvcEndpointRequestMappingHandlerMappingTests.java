/*
 * Copyright 2012-2018 the original author or authors.
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

import java.util.Arrays;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.annotation.AnnotatedEndpointInfo;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcEndpointRequestMappingHandlerMapping}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class WebMvcEndpointRequestMappingHandlerMappingTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final StaticApplicationContext context = new StaticApplicationContext();

	@Test
	public void mappingWithNoPrefix() throws Exception {
		AnnotatedEndpointInfo<WebOperation> first = firstEndpointInfo();
		AnnotatedEndpointInfo<WebOperation> second = secondEndpointInfo();
		WebMvcEndpointRequestMappingHandlerMapping mapping = createMapping("",
				first, second);
		assertThat(mapping.getHandler(request("GET", "/first")).getHandler())
				.isEqualTo(handlerOf(first.getSource(), "get"));
		assertThat(mapping.getHandler(request("POST", "/second")).getHandler())
				.isEqualTo(handlerOf(second.getSource(), "save"));
		assertThat(mapping.getHandler(request("GET", "/third"))).isNull();
	}

	@Test
	public void mappingWithPrefix() throws Exception {
		AnnotatedEndpointInfo<WebOperation> first = firstEndpointInfo();
		AnnotatedEndpointInfo<WebOperation> second = secondEndpointInfo();
		WebMvcEndpointRequestMappingHandlerMapping mapping = createMapping("actuator",
				first, second);
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		assertThat(mapping.getHandler(request("GET", "/actuator/first")).getHandler())
				.isEqualTo(handlerOf(first.getSource(), "get"));
		assertThat(mapping.getHandler(request("POST", "/actuator/second")).getHandler())
				.isEqualTo(handlerOf(second.getSource(), "save"));
		assertThat(mapping.getHandler(request("GET", "/first"))).isNull();
		assertThat(mapping.getHandler(request("GET", "/second"))).isNull();
	}


	@Test
	public void mappingNarrowedToMethod() throws Exception {
		AnnotatedEndpointInfo<WebOperation> first = firstEndpointInfo();
		WebMvcEndpointRequestMappingHandlerMapping mapping = createMapping("actuator",
				first);
		this.thrown.expect(HttpRequestMethodNotSupportedException.class);
		mapping.getHandler(request("POST", "/actuator/first"));

	}

	private WebMvcEndpointRequestMappingHandlerMapping createMapping(String prefix,
			AnnotatedEndpointInfo<WebOperation>... endpoints) {
		WebMvcEndpointRequestMappingHandlerMapping mapping = new WebMvcEndpointRequestMappingHandlerMapping(
				new EndpointMapping(prefix), EndpointPathResolver.useEndpointId(),
				Arrays.asList(endpoints));
		mapping.setApplicationContext(this.context);
		mapping.afterPropertiesSet();
		return mapping;
	}


	private HandlerMethod handlerOf(Object source, String methodName) {
		return new HandlerMethod(source,
				ReflectionUtils.findMethod(source.getClass(), methodName));
	}

	private MockHttpServletRequest request(String method, String requestURI) {
		return new MockHttpServletRequest(method, requestURI);
	}

	private AnnotatedEndpointInfo<WebOperation> firstEndpointInfo() {
		return new AnnotatedEndpointInfo<>(new FirstTestMvcEndpoint(), "first", true,
				Collections.EMPTY_LIST);
	}

	private AnnotatedEndpointInfo<WebOperation> secondEndpointInfo() {
		return new AnnotatedEndpointInfo<>(new SecondTestMvcEndpoint(), "second", true,
				Collections.EMPTY_LIST);
	}

	@WebMvcEndpoint(id = "first")
	private static class FirstTestMvcEndpoint {

		@GetMapping("/")
		public String get() {
			return "test";
		}

	}

	@WebMvcEndpoint(id = "second")
	private static class SecondTestMvcEndpoint {

		@PostMapping("/")
		public void save() {

		}

	}

}
