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

package org.springframework.boot.webservices.client;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.boot.web.client.ClientHttpRequestFactorySupplier;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

/**
 * {@link WebServiceMessageSender} builder that can detect a suitable HTTP library based
 * on the classpath.
 *
 * @author Stephane Nicoll
 * @author Dmytro Nosan
 * @since 2.1.0
 */
public class HttpWebServiceMessageSenderBuilder {

	private final Duration connectionTimeout;

	private final Duration readTimeout;

	private final Supplier<ClientHttpRequestFactory> requestFactorySupplier;

	private HttpWebServiceMessageSenderBuilder(Duration connectionTimeout,
			Duration readTimeout,
			Supplier<ClientHttpRequestFactory> requestFactorySupplier) {
		this.connectionTimeout = connectionTimeout;
		this.readTimeout = readTimeout;
		this.requestFactorySupplier = requestFactorySupplier;
	}

	public HttpWebServiceMessageSenderBuilder() {
		this(null, null, new ClientHttpRequestFactorySupplier());
	}

	/**
	 * Set the connection timeout.
	 * @param connectionTimeout the connection timeout.
	 * @return a new builder instance
	 */
	public HttpWebServiceMessageSenderBuilder setConnectionTimeout(
			Duration connectionTimeout) {
		return new HttpWebServiceMessageSenderBuilder(connectionTimeout, this.readTimeout,
				this.requestFactorySupplier);
	}

	/**
	 * Set the read timeout.
	 * @param readTimeout the read timeout.
	 * @return a new builder instance
	 */
	public HttpWebServiceMessageSenderBuilder setReadTimeout(Duration readTimeout) {
		return new HttpWebServiceMessageSenderBuilder(this.connectionTimeout, readTimeout,
				this.requestFactorySupplier);
	}

	/**
	 * Set the {@code Supplier} of {@link ClientHttpRequestFactory} that should be called
	 * to create the HTTP-based {@link WebServiceMessageSender}.
	 * @param requestFactorySupplier the supplier for the request factory
	 * @return a new builder instance
	 */
	public HttpWebServiceMessageSenderBuilder requestFactory(
			Supplier<ClientHttpRequestFactory> requestFactorySupplier) {
		Assert.notNull(requestFactorySupplier,
				"RequestFactory Supplier must not be null");
		return new HttpWebServiceMessageSenderBuilder(this.connectionTimeout,
				this.readTimeout, requestFactorySupplier);
	}

	/**
	 * Set if a suitable HTTP-based {@link WebServiceMessageSender} should be detected
	 * based on the classpath. Default is {@code true}.
	 * @return a new builder instance
	 * @see HttpWebServiceMessageSenderBuilder
	 */
	public WebServiceMessageSender build() {
		ClientHttpRequestFactory requestFactory = this.requestFactorySupplier.get();
		Assert.notNull(requestFactory, "requestFactory must not be null");
		if (this.connectionTimeout != null) {
			TimeoutRequestFactoryCustomizer customizer = new TimeoutRequestFactoryCustomizer(
					this.connectionTimeout, "setConnectTimeout");
			customizer.customize(requestFactory);
		}
		if (this.readTimeout != null) {
			TimeoutRequestFactoryCustomizer customizer = new TimeoutRequestFactoryCustomizer(
					this.readTimeout, "setReadTimeout");
			customizer.customize(requestFactory);
		}
		return new ClientHttpRequestMessageSender(requestFactory);
	}

	/**
	 * {@link ClientHttpRequestFactory} customizer to call a "set timeout" method.
	 */
	private static class TimeoutRequestFactoryCustomizer {

		private final Duration timeout;

		private final String methodName;

		TimeoutRequestFactoryCustomizer(Duration timeout, String methodName) {
			this.timeout = timeout;
			this.methodName = methodName;
		}

		public void customize(ClientHttpRequestFactory factory) {
			ReflectionUtils.invokeMethod(findMethod(factory), factory,
					Math.toIntExact(this.timeout.toMillis()));
		}

		private Method findMethod(ClientHttpRequestFactory factory) {
			Method method = ReflectionUtils.findMethod(factory.getClass(),
					this.methodName, int.class);
			if (method != null) {
				return method;
			}
			throw new IllegalStateException("Request factory " + factory.getClass()
					+ " does not have a " + this.methodName + "(int) method");
		}

	}

}
