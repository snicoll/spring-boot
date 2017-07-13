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

package org.springframework.boot.actuate.endpoint;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnvironmentEndpoint}.
 *
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Nicolas Lejeune
 * @author Stephane Nicoll
 * @author Madhura Bhave
 * @author Andy Wilkinson
 */
public class EnvironmentEndpointTests {

	@Test
	public void basicResponse() throws Exception {
		assertThat(new EnvironmentEndpoint(new StandardEnvironment()).environment())
				.isNotEmpty();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void compositeSourceIsHandledCorrectly() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		EnvironmentEndpoint endpoint = new EnvironmentEndpoint(environment);
		CompositePropertySource source = new CompositePropertySource("composite");
		source.addPropertySource(new MapPropertySource("one",
				Collections.singletonMap("foo", (Object) "bar")));
		source.addPropertySource(new MapPropertySource("two",
				Collections.singletonMap("foo", (Object) "spam")));
		environment.getPropertySources().addFirst(source);
		Map<String, Object> env = endpoint.environment();
		assertThat(((Map<String, Object>) env.get("composite:one")).get("foo"))
				.isEqualTo("bar");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void sensitiveKeysHaveTheirValuesSanitized() throws Exception {
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		System.setProperty("mySecret", "123456");
		System.setProperty("myCredentials", "123456");
		System.setProperty("VCAP_SERVICES", "123456");
		EnvironmentEndpoint endpoint = new EnvironmentEndpoint(new StandardEnvironment());
		Map<String, Object> env = endpoint.environment();
		Map<String, Object> systemProperties = (Map<String, Object>) env
				.get("systemProperties");
		assertThat(systemProperties.get("dbPassword")).isEqualTo("******");
		assertThat(systemProperties.get("apiKey")).isEqualTo("******");
		assertThat(systemProperties.get("mySecret")).isEqualTo("******");
		assertThat(systemProperties.get("myCredentials")).isEqualTo("******");
		assertThat(systemProperties.get("VCAP_SERVICES")).isEqualTo("******");
		clearSystemProperties("dbPassword", "apiKey", "mySecret", "myCredentials",
				"VCAP_SERVICES");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testKeySanitizationCredentialsPattern() throws Exception {
		System.setProperty("my.services.amqp-free.credentials.uri", "123456");
		System.setProperty("credentials.http_api_uri", "123456");
		System.setProperty("my.services.cleardb-free.credentials", "123456");
		System.setProperty("foo.mycredentials.uri", "123456");
		Map<String, Object> env = new EnvironmentEndpoint(new StandardEnvironment())
				.environment();
		Map<String, Object> systemProperties = (Map<String, Object>) env
				.get("systemProperties");
		assertThat(systemProperties.get("my.services.amqp-free.credentials.uri"))
				.isEqualTo("******");
		assertThat(systemProperties.get("credentials.http_api_uri")).isEqualTo("******");
		assertThat(systemProperties.get("my.services.cleardb-free.credentials"))
				.isEqualTo("******");
		assertThat(systemProperties.get("foo.mycredentials.uri")).isEqualTo("******");
		clearSystemProperties("my.services.amqp-free.credentials.uri",
				"credentials.http_api_uri", "my.services.cleardb-free.credentials",
				"foo.mycredentials.uri");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void keysMatchingCustomNameHaveTheirValuesSanitized() throws Exception {
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint endpoint = new EnvironmentEndpoint(new StandardEnvironment());
		endpoint.setKeysToSanitize("key");
		Map<String, Object> env = endpoint.environment();
		Map<String, Object> systemProperties = (Map<String, Object>) env
				.get("systemProperties");
		assertThat(systemProperties.get("dbPassword")).isEqualTo("123456");
		assertThat(systemProperties.get("apiKey")).isEqualTo("******");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void keysMatchingCustomPatternHaveTheirValuesSanitized() throws Exception {
		System.setProperty("dbPassword", "123456");
		System.setProperty("apiKey", "123456");
		EnvironmentEndpoint endpoint = new EnvironmentEndpoint(new StandardEnvironment());
		endpoint.setKeysToSanitize(".*pass.*");
		Map<String, Object> env = endpoint.environment();
		Map<String, Object> systemProperties = (Map<String, Object>) env
				.get("systemProperties");
		assertThat(systemProperties.get("dbPassword")).isEqualTo("******");
		assertThat(systemProperties.get("apiKey")).isEqualTo("123456");
		clearSystemProperties("dbPassword", "apiKey");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void propertyWithPlaceholderResolved() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("my.foo: ${bar.blah}", "bar.blah: hello")
				.applyTo(environment);
		Map<String, Object> env = new EnvironmentEndpoint(environment).environment();
		Map<String, Object> testProperties = (Map<String, Object>) env.get("test");
		assertThat(testProperties.get("my.foo")).isEqualTo("hello");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void propertyWithPlaceholderNotResolved() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("my.foo: ${bar.blah}").applyTo(environment);
		Map<String, Object> env = new EnvironmentEndpoint(environment).environment();
		Map<String, Object> testProperties = (Map<String, Object>) env.get("test");
		assertThat(testProperties.get("my.foo")).isEqualTo("${bar.blah}");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void propertyWithSensitivePlaceholderResolved() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertyValues
				.of("my.foo: http://${bar.password}://hello", "bar.password: hello")
				.applyTo(environment);
		Map<String, Object> env = new EnvironmentEndpoint(environment).environment();
		Map<String, Object> testProperties = (Map<String, Object>) env.get("test");
		assertThat(testProperties.get("my.foo")).isEqualTo("http://******://hello");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void propertyWithSensitivePlaceholderNotResolved() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		TestPropertyValues.of("my.foo: http://${bar.password}://hello")
				.applyTo(environment);
		Map<String, Object> env = new EnvironmentEndpoint(environment).environment();
		Map<String, Object> testProperties = (Map<String, Object>) env.get("test");
		assertThat(testProperties.get("my.foo"))
				.isEqualTo("http://${bar.password}://hello");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void propertyWithTypeOtherThanStringShouldNotFail() throws Exception {
		StandardEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		Map<String, Object> source = new HashMap<String, Object>();
		source.put("foo", Collections.singletonMap("bar", "baz"));
		propertySources.addFirst(new MapPropertySource("test", source));
		Map<String, Object> env = new EnvironmentEndpoint(environment).environment();
		Map<String, Object> testProperties = (Map<String, Object>) env.get("test");
		Map<String, String> foo = (Map<String, String>) testProperties.get("foo");
		assertThat(foo.get("bar")).isEqualTo("baz");
	}

	private void clearSystemProperties(String... properties) {
		for (String property : properties) {
			System.clearProperty(property);
		}
	}

}
