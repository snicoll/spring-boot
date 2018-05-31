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
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

import javax.xml.transform.TransformerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.web.client.ClientHttpRequestFactorySupplier;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.ClientHttpRequestMessageSender;

/**
 * Builder that can be used to configure and create a {@link WebServiceTemplate}. Provides
 * convenience methods to register {@link #messageSenders(WebServiceMessageSender...)
 * message senders}, {@link #interceptors(ClientInterceptor...) client interceptors} and
 * {@link #customizers(WebServiceTemplateCustomizer...) customizers}.
 * <p>
 * By default the built {@link WebServiceTemplate} uses the most suitable HTTP-based
 * {@link WebServiceMessageSender}, call {@link #detectHttpMessageSender(boolean)
 * detectHttpMessageSender(false)} if you prefer to keep the default. In a typical
 * auto-configured Spring Boot application this builder is available as a bean and can be
 * injected whenever a {@link WebServiceTemplate} is needed.
 *
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class WebServiceTemplateBuilder {

	private final Supplier<WebServiceMessageSender> httpMessageSenderSupplier;

	private final Set<ClientInterceptor> interceptors;

	private final Set<WebServiceTemplateCustomizer> internalCustomizers;

	private final Set<WebServiceTemplateCustomizer> customizers;

	private final Set<WebServiceMessageSender> messageSenders;

	private final Marshaller marshaller;

	private final Unmarshaller unmarshaller;

	private final DestinationProvider destinationProvider;

	private final Class<? extends TransformerFactory> transformerFactoryClass;

	private final WebServiceMessageFactory messageFactory;

	public WebServiceTemplateBuilder(WebServiceTemplateCustomizer... customizers) {
		this(new HttpMessageSenderSupplier(), Collections.emptySet(),
				Collections.emptySet(),
				append(Collections.<WebServiceTemplateCustomizer>emptySet(), customizers),
				Collections.emptySet(), null, null, null, null, null);
	}

	private WebServiceTemplateBuilder(
			Supplier<WebServiceMessageSender> httpMessageSenderSupplier,
			Set<ClientInterceptor> interceptors,
			Set<WebServiceTemplateCustomizer> internalCustomizers,
			Set<WebServiceTemplateCustomizer> customizers,
			Set<WebServiceMessageSender> messageSenders, Marshaller marshaller,
			Unmarshaller unmarshaller, DestinationProvider destinationProvider,
			Class<? extends TransformerFactory> transformerFactoryClass,
			WebServiceMessageFactory messageFactory) {
		this.interceptors = interceptors;
		this.internalCustomizers = internalCustomizers;
		this.customizers = customizers;
		this.messageSenders = messageSenders;
		this.httpMessageSenderSupplier = httpMessageSenderSupplier;
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
		this.destinationProvider = destinationProvider;
		this.transformerFactoryClass = transformerFactoryClass;
		this.messageFactory = messageFactory;
	}

	/**
	 * Set if a suitable HTTP-based {@link WebServiceMessageSender} should be detected
	 * based on the classpath and added to the list of configured message senders. Default
	 * is {@code true}.
	 * @param detectHttpMessageSender if a HTTP-based {@link WebServiceMessageSender}
	 * should be detected
	 * @return a new builder instance
	 * @see #detectHttpMessageSender(Duration, Duration)
	 */
	public WebServiceTemplateBuilder detectHttpMessageSender(
			boolean detectHttpMessageSender) {
		Supplier<WebServiceMessageSender> supplier = (detectHttpMessageSender
				? new HttpMessageSenderSupplier() : null);
		return new WebServiceTemplateBuilder(supplier, this.interceptors,
				this.internalCustomizers, this.customizers, this.messageSenders,
				this.marshaller, this.unmarshaller, this.destinationProvider,
				this.transformerFactoryClass, this.messageFactory);
	}

	/**
	 * Detect a suitable HTTP-based {@link WebServiceMessageSender} based on the classpath
	 * with the specified configuration and add it to the list of configured message
	 * senders.
	 * @param connectionTimeout the connection timeout or {@code null} to use the default
	 * @param readTimeout the read timeout or {@code null} to use the default
	 * @return a new builder instance
	 */
	public WebServiceTemplateBuilder detectHttpMessageSender(Duration connectionTimeout,
			Duration readTimeout) {
		return new WebServiceTemplateBuilder(
				new HttpMessageSenderSupplier(connectionTimeout, readTimeout),
				this.interceptors, this.internalCustomizers, this.customizers,
				this.messageSenders, this.marshaller, this.unmarshaller,
				this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory);
	}

	/**
	 * Sets the {@link WebServiceMessageSender WebServiceMessageSenders} that should be
	 * used with the {@link WebServiceTemplate}. Setting this value will replace any
	 * previously defined message senders.
	 * @param messageSenders the message senders to set
	 * @return a new builder instance.
	 * @see #additionalMessageSenders(WebServiceMessageSender...)
	 * @see #detectHttpMessageSender(boolean)
	 */
	public WebServiceTemplateBuilder messageSenders(
			WebServiceMessageSender... messageSenders) {
		Assert.notNull(messageSenders, "MessageSenders must not be null");
		return messageSenders(Arrays.asList(messageSenders));
	}

	/**
	 * Sets the {@link WebServiceMessageSender WebServiceMessageSenders} that should be
	 * used with the {@link WebServiceTemplate}. Setting this value will replace any
	 * previously defined message senders.
	 * @param messageSenders the message senders to set
	 * @return a new builder instance.
	 * @see #additionalMessageSenders(Collection)
	 * @see #detectHttpMessageSender(boolean)
	 */
	public WebServiceTemplateBuilder messageSenders(
			Collection<? extends WebServiceMessageSender> messageSenders) {
		Assert.notNull(messageSenders, "MessageSenders must not be null");
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				this.interceptors, this.internalCustomizers, this.customizers,
				append(Collections.<WebServiceMessageSender>emptySet(), messageSenders),
				this.marshaller, this.unmarshaller, this.destinationProvider,
				this.transformerFactoryClass, this.messageFactory);
	}

	/**
	 * Add additional {@link WebServiceMessageSender WebServiceMessageSenders} that should
	 * be used with the {@link WebServiceTemplate}.
	 * @param messageSenders the message senders to add
	 * @return a new builder instance.
	 * @see #messageSenders(WebServiceMessageSender...)
	 */
	public WebServiceTemplateBuilder additionalMessageSenders(
			WebServiceMessageSender... messageSenders) {
		Assert.notNull(messageSenders, "MessageSenders must not be null");
		return additionalMessageSenders(Arrays.asList(messageSenders));
	}

	/**
	 * Add additional {@link WebServiceMessageSender WebServiceMessageSenders} that should
	 * be used with the {@link WebServiceTemplate}.
	 * @param messageSenders the message senders to add
	 * @return a new builder instance.
	 * @see #messageSenders(Collection)
	 */
	public WebServiceTemplateBuilder additionalMessageSenders(
			Collection<? extends WebServiceMessageSender> messageSenders) {
		Assert.notNull(messageSenders, "MessageSenders must not be null");
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				this.interceptors, this.internalCustomizers, this.customizers,
				append(this.messageSenders, messageSenders), this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory);
	}

	/**
	 * Set the {@link ClientInterceptor ClientInterceptors} that should be used with the
	 * {@link WebServiceTemplate}. Setting this value will replace any previously defined
	 * interceptors.
	 * @param interceptors the interceptors to set
	 * @return a new builder instance
	 * @see #additionalInterceptors(ClientInterceptor...)
	 */
	public WebServiceTemplateBuilder interceptors(ClientInterceptor... interceptors) {
		Assert.notNull(interceptors, "Interceptors must not be null");
		return interceptors(Arrays.asList(interceptors));
	}

	/**
	 * Set the {@link ClientInterceptor ClientInterceptors} that should be used with the
	 * {@link WebServiceTemplate}. Setting this value will replace any previously defined
	 * interceptors.
	 * @param interceptors the interceptors to set
	 * @return a new builder instance
	 * @see #additionalInterceptors(Collection)
	 */
	public WebServiceTemplateBuilder interceptors(
			Collection<? extends ClientInterceptor> interceptors) {
		Assert.notNull(interceptors, "Interceptors must not be null");
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				append(Collections.<ClientInterceptor>emptySet(), interceptors),
				this.internalCustomizers, this.customizers, this.messageSenders,
				this.marshaller, this.unmarshaller, this.destinationProvider,
				this.transformerFactoryClass, this.messageFactory);
	}

	/**
	 * Add additional {@link ClientInterceptor ClientInterceptors} that should be used
	 * with the {@link WebServiceTemplate}.
	 * @param interceptors the interceptors to add
	 * @return a new builder instance
	 * @see #interceptors(ClientInterceptor...)
	 */
	public WebServiceTemplateBuilder additionalInterceptors(
			ClientInterceptor... interceptors) {
		Assert.notNull(interceptors, "Interceptors must not be null");
		return additionalInterceptors(Arrays.asList(interceptors));
	}

	/**
	 * Add additional {@link ClientInterceptor ClientInterceptors} that should be used
	 * with the {@link WebServiceTemplate}.
	 * @param interceptors the interceptors to add
	 * @return a new builder instance
	 * @see #interceptors(Collection)
	 */
	public WebServiceTemplateBuilder additionalInterceptors(
			Collection<? extends ClientInterceptor> interceptors) {
		Assert.notNull(interceptors, "Interceptors must not be null");
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				append(this.interceptors, interceptors), this.internalCustomizers,
				this.customizers, this.messageSenders, this.marshaller, this.unmarshaller,
				this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory);
	}

	/**
	 * Set {@link WebServiceTemplateCustomizer WebServiceTemplateCustomizers} that should
	 * be applied to the {@link WebServiceTemplate}. Customizers are applied in the order
	 * that they were added after builder configuration has been applied. Setting this
	 * value will replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(WebServiceTemplateCustomizer...)
	 */
	public WebServiceTemplateBuilder customizers(
			WebServiceTemplateCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return customizers(Arrays.asList(customizers));
	}

	/**
	 * Set {@link WebServiceTemplateCustomizer WebServiceTemplateCustomizers} that should
	 * be applied to the {@link WebServiceTemplate}. Customizers are applied in the order
	 * that they were added after builder configuration has been applied. Setting this
	 * value will replace any previously configured customizers.
	 * @param customizers the customizers to set
	 * @return a new builder instance
	 * @see #additionalCustomizers(Collection)
	 */
	public WebServiceTemplateBuilder customizers(
			Collection<? extends WebServiceTemplateCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				this.interceptors, this.internalCustomizers,
				append(Collections.<WebServiceTemplateCustomizer>emptySet(), customizers),
				this.messageSenders, this.marshaller, this.unmarshaller,
				this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory);
	}

	/**
	 * Add additional {@link WebServiceTemplateCustomizer WebServiceTemplateCustomizers}
	 * that should be applied to the {@link WebServiceTemplate}. Customizers are applied
	 * in the order that they were added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(WebServiceTemplateCustomizer...)
	 */
	public WebServiceTemplateBuilder additionalCustomizers(
			WebServiceTemplateCustomizer... customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return additionalCustomizers(Arrays.asList(customizers));
	}

	/**
	 * Add additional {@link WebServiceTemplateCustomizer WebServiceTemplateCustomizers}
	 * that should be applied to the {@link WebServiceTemplate}. Customizers are applied
	 * in the order that they were added after builder configuration has been applied.
	 * @param customizers the customizers to add
	 * @return a new builder instance
	 * @see #customizers(Collection)
	 */
	public WebServiceTemplateBuilder additionalCustomizers(
			Collection<? extends WebServiceTemplateCustomizer> customizers) {
		Assert.notNull(customizers, "Customizers must not be null");
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				this.interceptors, this.internalCustomizers,
				append(this.customizers, customizers), this.messageSenders,
				this.marshaller, this.unmarshaller, this.destinationProvider,
				this.transformerFactoryClass, this.messageFactory);
	}

	/**
	 * Indicates whether the connection should be checked for fault indicators
	 * ({@code true}), or whether we should rely on the message only ({@code false}).
	 * @param checkConnectionForFault whether to check for fault indicators
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setCheckConnectionForFault(boolean)
	 */
	public WebServiceTemplateBuilder setCheckConnectionForFault(
			boolean checkConnectionForFault) {
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				this.interceptors,
				append(this.internalCustomizers,
						new CheckConnectionFaultCustomizer(checkConnectionForFault)),
				this.customizers, this.messageSenders, this.marshaller, this.unmarshaller,
				this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory);
	}

	/**
	 * Indicates whether the connection should be checked for error indicators
	 * ({@code true}), or whether these should be ignored ({@code false}).
	 * @param checkConnectionForError whether to check for error indicators
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setCheckConnectionForError(boolean)
	 */
	public WebServiceTemplateBuilder setCheckConnectionForError(
			boolean checkConnectionForError) {
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				this.interceptors,
				append(this.internalCustomizers,
						new CheckConnectionForErrorCustomizer(checkConnectionForError)),
				this.customizers, this.messageSenders, this.marshaller, this.unmarshaller,
				this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory);
	}

	/**
	 * Sets the {@link WebServiceMessageFactory} to use for creating messages.
	 * @param messageFactory the message factory to use for creating messages
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setMessageFactory(WebServiceMessageFactory)
	 **/
	public WebServiceTemplateBuilder setWebServiceMessageFactory(
			WebServiceMessageFactory messageFactory) {
		Assert.notNull(messageFactory, "messageFactory must not be null");
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				this.interceptors, this.internalCustomizers, this.customizers,
				this.messageSenders, this.marshaller, this.unmarshaller,
				this.destinationProvider, this.transformerFactoryClass, messageFactory);
	}

	/**
	 * Set the {@link Unmarshaller} to use to deserialize messages.
	 * @param unmarshaller the message unmarshaller
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setUnmarshaller(Unmarshaller)
	 **/
	public WebServiceTemplateBuilder setUnmarshaller(Unmarshaller unmarshaller) {
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				this.interceptors, this.internalCustomizers, this.customizers,
				this.messageSenders, this.marshaller, unmarshaller,
				this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory);
	}

	/**
	 * Set the {@link Marshaller} to use to serialize messages.
	 * @param marshaller the message marshaller
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setMarshaller(Marshaller)
	 **/
	public WebServiceTemplateBuilder setMarshaller(Marshaller marshaller) {
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				this.interceptors, this.internalCustomizers, this.customizers,
				this.messageSenders, marshaller, this.unmarshaller,
				this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory);
	}

	/**
	 * Set the {@link FaultMessageResolver} to use.
	 * @param faultMessageResolver the fault message resolver to use
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setFaultMessageResolver(FaultMessageResolver)
	 */
	public WebServiceTemplateBuilder setFaultMessageResolver(
			FaultMessageResolver faultMessageResolver) {
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				this.interceptors,
				append(this.internalCustomizers,
						new FaultMessageResolverCustomizer(faultMessageResolver)),
				this.customizers, this.messageSenders, this.marshaller, this.unmarshaller,
				this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory);
	}

	/**
	 * Set the {@link TransformerFactory} implementation to use.
	 * @param transformerFactoryClass the transformer factory implementation to use
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setTransformerFactoryClass(Class)
	 */
	public WebServiceTemplateBuilder setTransformerFactoryClass(
			Class<? extends TransformerFactory> transformerFactoryClass) {
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				this.interceptors, this.internalCustomizers, this.customizers,
				this.messageSenders, this.marshaller, this.unmarshaller,
				this.destinationProvider, transformerFactoryClass, this.messageFactory);
	}

	/**
	 * Set the default URI to be used on operations that do not have a URI parameter.
	 * <p>
	 * Typically, either this property is set, or
	 * {@link #setDestinationProvider(DestinationProvider)}, but not both.
	 * @param defaultUri the destination provider URI to be used on operations that do not
	 * have a URI parameter.
	 * @return a new builder instance.
	 * @see #setDestinationProvider(DestinationProvider)
	 */
	public WebServiceTemplateBuilder setDefaultUri(String defaultUri) {
		Assert.hasText(defaultUri, "DefaultUri must not be empty");
		return setDestinationProvider(() -> URI.create(defaultUri));
	}

	/**
	 * Set the {@link DestinationProvider} to use
	 * <p>
	 * Typically, either this property is set, or {@link #setDefaultUri(String)}, but not
	 * both.
	 * @param destinationProvider the destination provider to be used on operations that
	 * do not have a URI parameter.
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setDestinationProvider(DestinationProvider)
	 */
	public WebServiceTemplateBuilder setDestinationProvider(
			DestinationProvider destinationProvider) {
		Assert.notNull(destinationProvider, "destinationProvider must not be null");
		return new WebServiceTemplateBuilder(this.httpMessageSenderSupplier,
				this.interceptors, this.internalCustomizers, this.customizers,
				this.messageSenders, this.marshaller, this.unmarshaller,
				destinationProvider, this.transformerFactoryClass, this.messageFactory);
	}

	/**
	 * Build a new {@link WebServiceTemplate} instance and configure it using this
	 * builder.
	 * @return a configured {@link WebServiceTemplate} instance.
	 * @see #build(Class)
	 * @see #configure(WebServiceTemplate)
	 */
	public WebServiceTemplate build() {
		return build(WebServiceTemplate.class);
	}

	/**
	 * Build a new {@link WebServiceTemplate} instance of the specified type and configure
	 * it using this builder.
	 * @param <T> the type of web service template
	 * @param webServiceTemplateClass the template type to create
	 * @return a configured {@link WebServiceTemplate} instance.
	 * @see WebServiceTemplateBuilder#build()
	 * @see #configure(WebServiceTemplate)
	 */
	public <T extends WebServiceTemplate> T build(Class<T> webServiceTemplateClass) {
		Assert.notNull(webServiceTemplateClass,
				"WebServiceTemplateClass must not be null");
		return configure(BeanUtils.instantiateClass(webServiceTemplateClass));
	}

	/**
	 * Configure the provided {@link WebServiceTemplate} instance using this builder.
	 * @param <T> the type of web service template
	 * @param webServiceTemplate the {@link WebServiceTemplate} to configure
	 * @return the web service template instance
	 * @see #build()
	 * @see #build(Class)
	 */
	public <T extends WebServiceTemplate> T configure(T webServiceTemplate) {
		Assert.notNull(webServiceTemplate, "webServiceTemplate must not be null");
		configureSenders(webServiceTemplate);
		if (!CollectionUtils.isEmpty(this.internalCustomizers)) {
			for (WebServiceTemplateCustomizer internalCustomizer : this.internalCustomizers) {
				internalCustomizer.customize(webServiceTemplate);
			}
		}
		if (this.marshaller != null) {
			webServiceTemplate.setMarshaller(this.marshaller);
		}
		if (this.unmarshaller != null) {
			webServiceTemplate.setUnmarshaller(this.unmarshaller);
		}
		if (this.destinationProvider != null) {
			webServiceTemplate.setDestinationProvider(this.destinationProvider);
		}
		if (this.transformerFactoryClass != null) {
			webServiceTemplate.setTransformerFactoryClass(this.transformerFactoryClass);
		}
		if (this.messageFactory != null) {
			webServiceTemplate.setMessageFactory(this.messageFactory);
		}
		if (!CollectionUtils.isEmpty(this.interceptors)) {
			webServiceTemplate.setInterceptors(
					append(this.interceptors, webServiceTemplate.getInterceptors())
							.toArray(new ClientInterceptor[0]));
		}
		if (!CollectionUtils.isEmpty(this.customizers)) {
			for (WebServiceTemplateCustomizer customizer : this.customizers) {
				customizer.customize(webServiceTemplate);
			}
		}
		return webServiceTemplate;
	}

	private <T extends WebServiceTemplate> void configureSenders(T webServiceTemplate) {
		if (!CollectionUtils.isEmpty(this.messageSenders)) {
			Set<WebServiceMessageSender> webServiceMessageSenders = new LinkedHashSet<>(
					this.messageSenders);
			webServiceTemplate.setMessageSenders(
					webServiceMessageSenders.toArray(new WebServiceMessageSender[0]));
		}
		else if (this.httpMessageSenderSupplier != null) {
			webServiceTemplate.setMessageSenders(new WebServiceMessageSender[] {
					this.httpMessageSenderSupplier.get() });
		}

	}

	private static <T> Set<T> append(Set<T> set, T[] additions) {
		return append(set, additions != null
				? new LinkedHashSet<>(Arrays.asList(additions)) : Collections.emptySet());
	}

	private static <T> Set<T> append(Set<T> set, T addition) {
		Set<T> result = new LinkedHashSet<>(set != null ? set : Collections.emptySet());
		result.add(addition);
		return Collections.unmodifiableSet(result);
	}

	private static <T> Set<T> append(Set<T> set, Collection<? extends T> additions) {
		Set<T> result = new LinkedHashSet<>(set != null ? set : Collections.emptySet());
		result.addAll(additions != null ? additions : Collections.emptyList());
		return Collections.unmodifiableSet(result);
	}

	/**
	 * {@link WebServiceTemplateCustomizer} to set
	 * {@link WebServiceTemplate#checkConnectionForFault checkConnectionForFault }.
	 */
	private static final class CheckConnectionFaultCustomizer
			implements WebServiceTemplateCustomizer {

		private final boolean checkConnectionFault;

		private CheckConnectionFaultCustomizer(boolean checkConnectionFault) {
			this.checkConnectionFault = checkConnectionFault;
		}

		@Override
		public void customize(WebServiceTemplate webServiceTemplate) {
			webServiceTemplate.setCheckConnectionForFault(this.checkConnectionFault);
		}

	}

	/**
	 * {@link WebServiceTemplateCustomizer} to set
	 * {@link WebServiceTemplate#checkConnectionForError checkConnectionForError }.
	 */
	private static final class CheckConnectionForErrorCustomizer
			implements WebServiceTemplateCustomizer {

		private final boolean checkConnectionForError;

		private CheckConnectionForErrorCustomizer(boolean checkConnectionForError) {
			this.checkConnectionForError = checkConnectionForError;
		}

		@Override
		public void customize(WebServiceTemplate webServiceTemplate) {
			webServiceTemplate.setCheckConnectionForError(this.checkConnectionForError);
		}

	}

	/**
	 * {@link WebServiceTemplateCustomizer} to set
	 * {@link WebServiceTemplate#faultMessageResolver faultMessageResolver }.
	 */
	private static final class FaultMessageResolverCustomizer
			implements WebServiceTemplateCustomizer {

		private final FaultMessageResolver faultMessageResolver;

		private FaultMessageResolverCustomizer(
				FaultMessageResolver faultMessageResolver) {
			this.faultMessageResolver = faultMessageResolver;
		}

		@Override
		public void customize(WebServiceTemplate webServiceTemplate) {
			webServiceTemplate.setFaultMessageResolver(this.faultMessageResolver);
		}

	}

	private static class HttpMessageSenderSupplier
			implements Supplier<WebServiceMessageSender> {

		private final Duration connectionTimeout;

		private final Duration readTimeout;

		HttpMessageSenderSupplier(Duration connectionTimeout, Duration readTimeout) {
			this.connectionTimeout = connectionTimeout;
			this.readTimeout = readTimeout;
		}

		HttpMessageSenderSupplier() {
			this(null, null);
		}

		@Override
		public WebServiceMessageSender get() {
			ClientHttpRequestFactory requestFactory = new ClientHttpRequestFactorySupplier()
					.get();
			if (this.connectionTimeout != null) {
				new TimeoutRequestFactoryCustomizer(this.connectionTimeout.toMillis(),
						"setConnectTimeout").customize(requestFactory);
			}
			if (this.readTimeout != null) {
				new TimeoutRequestFactoryCustomizer(this.readTimeout.toMillis(),
						"setReadTimeout").customize(requestFactory);
			}
			return new ClientHttpRequestMessageSender(requestFactory);
		}

		/**
		 * {@link ClientHttpRequestFactory} customizer to call a "set timeout" method.
		 */
		private static class TimeoutRequestFactoryCustomizer {

			private final long timeout;

			private final String methodName;

			TimeoutRequestFactoryCustomizer(long timeout, String methodName) {
				this.timeout = timeout;
				this.methodName = methodName;
			}

			public void customize(ClientHttpRequestFactory factory) {
				ReflectionUtils.invokeMethod(findMethod(factory), factory,
						(int) this.timeout);
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

}
