package org.springframework.boot.testsupport.web.servlet;

import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockSessionCookieConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MockServletWebServer}.
 *
 * @author Stephane Nicoll
 */
class MockServletWebServerTests {

	@Test
	void servletContextIsConfigured() {
		MockServletWebServer server = TestMockServletWebServer.create();
		assertThat(server.getServletContext()).isNotNull();
	}

	@Test
	void servletContextHasSessionCookieConfigConfigured() {
		MockServletWebServer server = TestMockServletWebServer.create();
		assertThat(server.getServletContext().getSessionCookieConfig()).isNotNull()
				.isInstanceOf(MockSessionCookieConfig.class);
	}

	private static final class TestMockServletWebServer extends MockServletWebServer {

		private TestMockServletWebServer(Initializer[] initializers, int port) {
			super(initializers, port);
		}

		public static MockServletWebServer create(Initializer... initializers) {
			return new TestMockServletWebServer(initializers, 8080);
		}

	}

}
