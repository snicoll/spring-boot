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

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Test;

import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.convert.ConversionServiceParameterMapper;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMapper;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.EndpointPathResolver;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.annotation.WebAnnotationEndpointDiscoverer;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.endpoint.web.EndpointMapping;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * Integration tests for {@link WebMvcEndpointRequestMappingHandlerMapping}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class WebMvcEndpointRequestMappingHandlerMappingIntegrationTests {

	public WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
			AnnotationConfigServletWebServerApplicationContext::new)
			.withUserConfiguration(EndpointConfiguration.class,
					ExampleMvcEndpoint.class);

	@Test
	public void get() {
		this.contextRunner.run(withWebTestClient(webTestClient -> {
			webTestClient.get().uri("/actuator/example/one").accept(MediaType.TEXT_PLAIN)
					.exchange()
					.expectStatus().isOk()
					.expectHeader().contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
					.expectBody(String.class).isEqualTo("One");
		}));
	}

	@Test
	public void getWithUnacceptableContentType() {
		this.contextRunner.run(withWebTestClient(webTestClient -> {
			webTestClient.get().uri("/actuator/example/one")
					.accept(MediaType.APPLICATION_JSON)
					.exchange()
					.expectStatus().isEqualTo(HttpStatus.NOT_ACCEPTABLE);
		}));
	}

	@Test
	public void post() {
		this.contextRunner.run(withWebTestClient(webTestClient -> {
			webTestClient.post().uri("/actuator/example/two")
					.syncBody(Collections.singletonMap("id", "test"))
					.exchange()
					.expectStatus().isCreated()
					.expectHeader().valueEquals(HttpHeaders.LOCATION, "/example/test");
		}));
	}

	private ContextConsumer<AssertableWebApplicationContext> withWebTestClient(
			Consumer<WebTestClient> webClient) {
		return (context) -> {
			int port = ((AnnotationConfigServletWebServerApplicationContext) context.getSourceApplicationContext())
					.getWebServer().getPort();
			WebTestClient webTestClient = createWebTestClient(port);
			webClient.accept(webTestClient);
		};
	}

	private WebTestClient createWebTestClient(int port) {
		DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(
				"http://localhost:" + port);
		uriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);
		return WebTestClient.bindToServer().uriBuilderFactory(uriBuilderFactory)
				.responseTimeout(Duration.ofMinutes(2)).build();
	}

	@Configuration
	@ImportAutoConfiguration({ JacksonAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, WebMvcAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class })
	static class EndpointConfiguration {

		@Bean
		public TomcatServletWebServerFactory tomcat() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		public WebAnnotationEndpointDiscoverer webEndpointDiscoverer(
				ApplicationContext applicationContext) {
			ParameterMapper parameterMapper = new ConversionServiceParameterMapper(
					DefaultConversionService.getSharedInstance());
			List<String> mediaTypes = Arrays.asList(MediaType.APPLICATION_JSON_VALUE,
					ActuatorMediaType.V2_JSON);
			EndpointMediaTypes endpointMediaTypes = new EndpointMediaTypes(mediaTypes,
					mediaTypes);
			return new WebAnnotationEndpointDiscoverer(applicationContext,
					parameterMapper, endpointMediaTypes,
					EndpointPathResolver.useEndpointId(), null, null);
		}

		@Bean
		public WebMvcEndpointRequestMappingHandlerMapping webEndpointHandlerMapping(
				EndpointDiscoverer<WebOperation> webEndpointDiscoverer) {
			return new WebMvcEndpointRequestMappingHandlerMapping(
					new EndpointMapping("actuator"), EndpointPathResolver.useEndpointId(),
					webEndpointDiscoverer.discoverEndpoints());
		}

	}

	@WebMvcEndpoint(id = "example")
	@ResponseBody
	public static class ExampleMvcEndpoint {

		@GetMapping(path = "one", produces = MediaType.TEXT_PLAIN_VALUE)
		public String one() {
			return "One";
		}

		@PostMapping("/two")
		public ResponseEntity<String> two(@RequestBody Map<String, Object> content) {
			return ResponseEntity.created(
					URI.create("/example/" + content.get("id"))).build();
		}

	}

}
