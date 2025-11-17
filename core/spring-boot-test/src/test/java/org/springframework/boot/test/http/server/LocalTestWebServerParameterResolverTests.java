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

package org.springframework.boot.test.http.server;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.http.server.LocalTestWebServer.Scheme;
import org.springframework.boot.testsupport.classpath.resources.WithResource;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LocalTestWebServerParameterResolver}.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(SpringExtension.class)
@WithResource(name = "META-INF/spring.factories", content = """
		org.springframework.boot.test.http.server.LocalTestWebServer$Provider=\
		org.springframework.boot.test.http.server.LocalTestWebServerParameterResolverTests.TestProvider
		""")
class LocalTestWebServerParameterResolverTests {

	@Test
	void parameterIsResolved(LocalTestWebServer webServer) {
		assertThat(webServer).isNotNull();
		assertThat(webServer.scheme()).isEqualTo(Scheme.HTTPS);
		assertThat(webServer.uri()).isEqualTo("https://localhost:7070/test");
	}

	@SuppressWarnings("unused")
	static class TestProvider implements LocalTestWebServer.Provider {

		@Override
		public @Nullable LocalTestWebServer getLocalTestWebServer() {
			return LocalTestWebServer.of(Scheme.HTTPS, 7070, "/test");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
