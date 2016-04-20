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

import org.junit.Test;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link ErrorPageCustomizer}.
 *
 * @author Stephane Nicoll
 */
public class ErrorPageCustomizerTests {

	private ConfigurableEmbeddedServletContainer container =
			mock(ConfigurableEmbeddedServletContainer.class);

	@Test
	public void templatesAreRegistered() {
		ErrorPageCustomizer customizer = customize(new String[] {"classpath:test/templates/one"},
				new String[] {});
		verifyZeroInteractions(this.container);
		assertThat(customizer.resolveErrorPath(HttpStatus.BAD_REQUEST)).isEqualTo("/error/400");
		assertThat(customizer.resolveErrorPath(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo("/error/500");
	}

	@Test
	public void staticResourcesAreRegistered() {
		ErrorPageCustomizer customizer = customize(new String[] {},
				new String[] {"classpath:test/static/one"});
		verifyErrorPage("/error/404.html", HttpStatus.NOT_FOUND);
		verifyErrorPage("/error/500.html", HttpStatus.INTERNAL_SERVER_ERROR);
		verifyNoMoreInteractions(this.container);
		assertThat(customizer.resolveErrorPath(HttpStatus.NOT_FOUND)).isNull();
		assertThat(customizer.resolveErrorPath(HttpStatus.INTERNAL_SERVER_ERROR)).isNull();
	}

	@Test
	public void staticResourcesAreRegisteredWithSeveralLocations() {
		ErrorPageCustomizer customizer = customize(new String[] {},
				new String[] {"classpath:test/static/one", "classpath:test/static/two"});
		verifyErrorPage("/error/400.html", HttpStatus.BAD_REQUEST);
		verifyErrorPage("/error/404.html", HttpStatus.NOT_FOUND);
		verifyErrorPage("/error/500.html", HttpStatus.INTERNAL_SERVER_ERROR);
		verifyNoMoreInteractions(this.container);
		assertThat(customizer.resolveErrorPath(HttpStatus.BAD_REQUEST)).isNull();
		assertThat(customizer.resolveErrorPath(HttpStatus.NOT_FOUND)).isNull();
		assertThat(customizer.resolveErrorPath(HttpStatus.INTERNAL_SERVER_ERROR)).isNull();
	}

	@Test
	public void templatesAreRegisteredWithSeveralLocations() {
		ErrorPageCustomizer customizer = customize(new String[] {"classpath:test/templates/one",
				"classpath:test/templates/two"}, new String[] {});
		verifyZeroInteractions(this.container);
		assertThat(customizer.resolveErrorPath(HttpStatus.BAD_REQUEST)).isEqualTo("/error/400");
		assertThat(customizer.resolveErrorPath(HttpStatus.NOT_FOUND)).isEqualTo("/error/404");
		assertThat(customizer.resolveErrorPath(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo("/error/500");
	}

	@Test
	public void resourceWithUnknownLocation() {
		customize(new String[] {}, new String[] {"classpath:test/static/one",
				"classpath:test/static/does_not_exist"});
		verifyErrorPage("/error/404.html", HttpStatus.NOT_FOUND);
		verifyErrorPage("/error/500.html", HttpStatus.INTERNAL_SERVER_ERROR);
		verifyNoMoreInteractions(this.container);
	}

	@Test
	public void resourceWithNoErrorSubPackage() {
		customize(new String[] {}, new String[] {"classpath:test/static/one",
				"classpath:test/static/none"});
		verifyErrorPage("/error/404.html", HttpStatus.NOT_FOUND);
		verifyErrorPage("/error/500.html", HttpStatus.INTERNAL_SERVER_ERROR);
		verifyNoMoreInteractions(this.container);
	}

	@Test
	public void templatesTakePrecedence() {
		ErrorPageCustomizer customizer = customize(new String[] {"classpath:test/templates/one"},
				new String[] {"classpath:test/static/one", "classpath:test/static/two"});
		verifyErrorPage("/error/404.html", HttpStatus.NOT_FOUND);
		verifyNoMoreInteractions(this.container);
		assertThat(customizer.resolveErrorPath(HttpStatus.BAD_REQUEST)).isEqualTo("/error/400");
		assertThat(customizer.resolveErrorPath(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo("/error/500");
	}

	@Test
	public void templatesWithCatchAll() {
		ErrorPageCustomizer customizer = customize(new String[] {"classpath:test/templates/three",
				"classpath:test/templates/one"},
				new String[] {});
		verifyZeroInteractions(this.container);
		assertThat(customizer.resolveErrorPath(HttpStatus.BAD_REQUEST)).isEqualTo("/error/400");
		assertThat(customizer.resolveErrorPath(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo("/error/500");
		assertThat(customizer.resolveErrorPath(HttpStatus.I_AM_A_TEAPOT)).isEqualTo("/error/4xx");
		assertThat(customizer.resolveErrorPath(HttpStatus.REQUEST_TIMEOUT)).isEqualTo("/error/4xx");
		assertThat(customizer.resolveErrorPath(HttpStatus.GONE)).isEqualTo("/error/4xx");
	}

	@Test
	public void templatesWithCatchAllHideStaticResources() {
		ErrorPageCustomizer customizer = customize(new String[] {"classpath:test/templates/three"},
				new String[]{"classpath:test/static/one"});
		verifyErrorPage("/error/500.html", HttpStatus.INTERNAL_SERVER_ERROR);
		verifyNoMoreInteractions(this.container);
		assertThat(customizer.resolveErrorPath(HttpStatus.NOT_FOUND)).isEqualTo("/error/4xx");
		assertThat(customizer.resolveErrorPath(HttpStatus.INTERNAL_SERVER_ERROR)).isNull();
	}

	@Test
	public void defaultErrorPage() {
		customize(new String[] {}, new String[] {}, "/context/error");
		verifyDefaultErrorPage("/context/error");
	}

	@Test
	public void locationsAreCleaned() {
		customize(new String[] {}, new String[] {"classpath:test/static/one/"});
		verifyErrorPage("/error/404.html", HttpStatus.NOT_FOUND);
		verifyErrorPage("/error/500.html", HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private ErrorPageCustomizer customize(String[] templatesLocations, String[] staticLocations) {
		return customize(templatesLocations, staticLocations, null);
	}

	private ErrorPageCustomizer customize(String[] templatesLocations, String[] staticLocations,
			String globalPath) {
		ErrorPageCustomizer customizer = new ErrorPageCustomizer(
				templatesLocations, staticLocations, globalPath);
		customizer.scan();
		customizer.customize(this.container);
		return customizer;
	}

	private void verifyDefaultErrorPage(String path) {
		verify(this.container).addErrorPages(new ErrorPage(path));
	}

	private void verifyErrorPage(String path, HttpStatus status) {
		verify(this.container).addErrorPages(new ErrorPage(status, path));
	}

}
