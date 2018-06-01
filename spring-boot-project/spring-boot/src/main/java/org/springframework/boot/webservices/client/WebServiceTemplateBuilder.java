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

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.xml.transform.TransformerFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.ws.WebServiceMessageFactory;
import org.springframework.ws.client.core.FaultMessageResolver;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.support.destination.DestinationProvider;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.transport.WebServiceMessageSender;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

/**
 * Builder that can be used to configure and create a {@link WebServiceTemplate}. Provides
 * convenience methods to register {@link #messageSenders(WebServiceMessageSender...)
 * message senders}, {@link #interceptors(ClientInterceptor...) client interceptors} and
 * {@link #customizers(Collection) customizers}.
 * <p>
 * By default the built {@link WebServiceTemplate} will attempt to use the most suitable
 * {@link WebServiceMessageSender}, call {@link #detectWebServiceMessageSender(boolean)
 * detectWebServiceMessageSender(false)} if you prefer to keep the default. In a typical
 * auto-configured Spring Boot application this builder is available as a bean and can be
 * injected whenever a {@link WebServiceTemplate} is needed.
 *
 * @author Dmytro Nosan
 * @author Stephane Nicoll
 * @since 2.1.0
 */
public class WebServiceTemplateBuilder {

	private final Set<ClientInterceptor> interceptors;

	private final Set<WebServiceTemplateCustomizer> internalCustomizers;

	private final Set<WebServiceTemplateCustomizer> customizers;

	private final Set<WebServiceMessageSender> webServiceMessageSenders;

	private final Set<WebServiceMessageSenderCustomizer> webServiceMessageSenderCustomizers;

	private final Marshaller marshaller;

	private final Unmarshaller unmarshaller;

	private final DestinationProvider destinationProvider;

	private final Class<? extends TransformerFactory> transformerFactoryClass;

	private final WebServiceMessageFactory messageFactory;

	private final boolean detectWebServiceMessageSender;

	public WebServiceTemplateBuilder(WebServiceTemplateCustomizer... customizers) {
		this(Collections.emptySet(), Collections.emptySet(),
				append(Collections.<WebServiceTemplateCustomizer>emptySet(), customizers),
				Collections.emptySet(), Collections.emptySet(), null, null, null, null,
				null, true);
	}

	private WebServiceTemplateBuilder(Set<ClientInterceptor> interceptors,
			Set<WebServiceTemplateCustomizer> internalCustomizers,
			Set<WebServiceTemplateCustomizer> customizers,
			Set<WebServiceMessageSender> webServiceMessageSenders,
			Set<WebServiceMessageSenderCustomizer> webServiceMessageSenderCustomizers,
			Marshaller marshaller, Unmarshaller unmarshaller,
			DestinationProvider destinationProvider,
			Class<? extends TransformerFactory> transformerFactoryClass,
			WebServiceMessageFactory messageFactory,
			boolean detectWebServiceMessageSender) {
		this.interceptors = interceptors;
		this.internalCustomizers = internalCustomizers;
		this.customizers = customizers;
		this.webServiceMessageSenders = webServiceMessageSenders;
		this.webServiceMessageSenderCustomizers = webServiceMessageSenderCustomizers;
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
		this.destinationProvider = destinationProvider;
		this.transformerFactoryClass = transformerFactoryClass;
		this.messageFactory = messageFactory;
		this.detectWebServiceMessageSender = detectWebServiceMessageSender;
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
		return new WebServiceTemplateBuilder(
				append(Collections.<ClientInterceptor>emptySet(), interceptors),
				this.internalCustomizers, this.customizers, this.webServiceMessageSenders,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
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
		return new WebServiceTemplateBuilder(append(this.interceptors, interceptors),
				this.internalCustomizers, this.customizers, this.webServiceMessageSenders,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
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
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				append(Collections.<WebServiceTemplateCustomizer>emptySet(), customizers),
				this.webServiceMessageSenders, this.webServiceMessageSenderCustomizers,
				this.marshaller, this.unmarshaller, this.destinationProvider,
				this.transformerFactoryClass, this.messageFactory,
				this.detectWebServiceMessageSender);
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
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				append(this.customizers, customizers), this.webServiceMessageSenders,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Sets the {@link WebServiceMessageSender WebServiceMessageSenders} that should be
	 * used with the {@link WebServiceTemplate}. Setting this value will replace any
	 * previously defined message senders.
	 * @param messageSenders the message senders to set
	 * @return a new builder instance.
	 * @see #additionalMessageSenders(WebServiceMessageSender...)
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
	 */
	public WebServiceTemplateBuilder messageSenders(
			Collection<? extends WebServiceMessageSender> messageSenders) {
		Assert.notNull(messageSenders, "MessageSenders must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers,
				append(Collections.<WebServiceMessageSender>emptySet(), messageSenders),
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
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
		Assert.notNull(messageSenders,
				"webServiceMessageSenderSuppliers must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, append(this.webServiceMessageSenders, messageSenders),
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplate#setCheckConnectionForFault(boolean)
	 * setCheckConnectionForFault} on the underlying.
	 * @param checkConnectionForFault Specify whether checkConnectionForFault should be
	 * enabled or not.
	 * @return a new builder instance.
	 */
	public WebServiceTemplateBuilder setCheckConnectionForFault(
			boolean checkConnectionForFault) {
		return new WebServiceTemplateBuilder(this.interceptors,
				append(this.internalCustomizers,
						new CheckConnectionFaultCustomizer(checkConnectionForFault)),
				this.customizers, this.webServiceMessageSenders,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplate#setCheckConnectionForError(boolean)
	 * setCheckConnectionForError} on the underlying.
	 * @param checkConnectionForError Specify whether checkConnectionForError should be
	 * enabled or not.
	 * @return a new builder instance.
	 */
	public WebServiceTemplateBuilder setCheckConnectionForError(
			boolean checkConnectionForError) {
		return new WebServiceTemplateBuilder(this.interceptors,
				append(this.internalCustomizers,
						new CheckConnectionForErrorCustomizer(checkConnectionForError)),
				this.customizers, this.webServiceMessageSenders,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Sets the message factory used for creating messages.
	 * @param messageFactory instance of WebServiceMessageFactory
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setMessageFactory(WebServiceMessageFactory)
	 **/
	public WebServiceTemplateBuilder setWebServiceMessageFactory(
			WebServiceMessageFactory messageFactory) {
		Assert.notNull(messageFactory, "messageFactory must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenders,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplate#setUnmarshaller(Unmarshaller) unmarshaller} on the
	 * underlying.
	 * @param unmarshaller message unmarshaller
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setUnmarshaller(Unmarshaller)
	 **/
	public WebServiceTemplateBuilder setUnmarshaller(Unmarshaller unmarshaller) {
		Assert.notNull(unmarshaller, "unmarshaller must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenders,
				this.webServiceMessageSenderCustomizers, this.marshaller, unmarshaller,
				this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplate#setMarshaller(Marshaller) marshaller} on the
	 * underlying.
	 * @param marshaller message marshaller
	 * @return a new builder instance.
	 * @see WebServiceTemplate#setMarshaller(Marshaller)
	 **/
	public WebServiceTemplateBuilder setMarshaller(Marshaller marshaller) {
		Assert.notNull(marshaller, "marshaller must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenders,
				this.webServiceMessageSenderCustomizers, marshaller, this.unmarshaller,
				this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Sets the connection timeout on the underlying.
	 * @param connectionTimeout the connection timeout.
	 * @return a new builder instance.
	 * @throws java.lang.IllegalStateException if the underlying source doesn't support a
	 * connection timeout.
	 */
	public WebServiceTemplateBuilder setConnectionTimeout(Duration connectionTimeout) {
		Assert.notNull(connectionTimeout, "connectionTimeout must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenders,
				append(this.webServiceMessageSenderCustomizers,
						new ConnectionTimeoutWebServiceMessageSenderCustomizer(
								connectionTimeout)),
				this.marshaller, this.unmarshaller, this.destinationProvider,
				this.transformerFactoryClass, this.messageFactory,
				this.detectWebServiceMessageSender);
	}

	/**
	 * Sets the read timeout on the underlying.
	 * @param readTimeout the read timeout
	 * @return a new builder instance.
	 * @throws java.lang.IllegalStateException if the underlying source doesn't support a
	 * read timeout.
	 */
	public WebServiceTemplateBuilder setReadTimeout(Duration readTimeout) {
		Assert.notNull(readTimeout, "readTimeout must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenders,
				append(this.webServiceMessageSenderCustomizers,
						new ReadTimeoutWebServiceMessageSenderCustomizer(readTimeout)),
				this.marshaller, this.unmarshaller, this.destinationProvider,
				this.transformerFactoryClass, this.messageFactory,
				this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplate#setFaultMessageResolver(FaultMessageResolver)
	 * faultMessageResolver} on the underlying.
	 * @param faultMessageResolver faultMessageResolver may be set to null to disable
	 * fault handling.
	 * @return a new builder instance.
	 **/
	public WebServiceTemplateBuilder setFaultMessageResolver(
			FaultMessageResolver faultMessageResolver) {
		return new WebServiceTemplateBuilder(this.interceptors,
				append(this.internalCustomizers,
						new FaultMessageResolverCustomizer(faultMessageResolver)),
				this.customizers, this.webServiceMessageSenders,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set {@link WebServiceTemplate#setTransformerFactoryClass(Class)
	 * setTransformerFactoryClass} on the underlying.
	 * @param transformerFactoryClass boolean value
	 * @return a new builder instance.
	 **/

	public WebServiceTemplateBuilder setTransformerFactoryClass(
			Class<? extends TransformerFactory> transformerFactoryClass) {
		Assert.notNull(transformerFactoryClass,
				"transformerFactoryClass must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenders,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set the default URI to be used on operations that do not have a URI parameter.
	 *
	 * <b>Note!</b>Typically, either this property is set, or
	 * {@link #setDestinationProvider(DestinationProvider)}, but not both.
	 * @param defaultUri the destination provider URI to be used on operations that do not
	 * have a URI parameter.
	 * @return a new builder instance.
	 */
	public WebServiceTemplateBuilder setDefaultUri(String defaultUri) {
		Assert.hasText(defaultUri, "defaultUri must not be empty");
		return setDestinationProvider(() -> URI.create(defaultUri));
	}

	/**
	 * Set {@link WebServiceTemplate#setDestinationProvider(DestinationProvider)
	 * destinationProvider} on the underlying.
	 *
	 * <b>Note!</b>Typically, either this property is set, or
	 * {@link #setDefaultUri(String)}, but not both.
	 * @param destinationProvider the destination provider URI to be used on operations
	 * that do not have a URI parameter.
	 * @return a new builder instance.
	 */
	public WebServiceTemplateBuilder setDestinationProvider(
			DestinationProvider destinationProvider) {
		Assert.notNull(destinationProvider, "destinationProvider must not be null");
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenders,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, destinationProvider, this.transformerFactoryClass,
				this.messageFactory, this.detectWebServiceMessageSender);
	}

	/**
	 * Set if the {@link WebServiceMessageSender} should be detected based on the
	 * classpath. Default is {@code true}.
	 * @param detectWebServiceMessageSender if the {@link WebServiceMessageSender} should
	 * be detected
	 * @return a new builder instance
	 */
	public WebServiceTemplateBuilder detectWebServiceMessageSender(
			boolean detectWebServiceMessageSender) {
		return new WebServiceTemplateBuilder(this.interceptors, this.internalCustomizers,
				this.customizers, this.webServiceMessageSenders,
				this.webServiceMessageSenderCustomizers, this.marshaller,
				this.unmarshaller, this.destinationProvider, this.transformerFactoryClass,
				this.messageFactory, detectWebServiceMessageSender);
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
				"webServiceTemplateClass must not be null");
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
		if (!CollectionUtils.isEmpty(this.webServiceMessageSenders)) {
			Set<WebServiceMessageSender> webServiceMessageSenders = new LinkedHashSet<>(
					this.webServiceMessageSenders);
			webServiceTemplate.setMessageSenders(
					webServiceMessageSenders.toArray(new WebServiceMessageSender[0]));
		}
		else if (this.detectWebServiceMessageSender) {
			webServiceTemplate.setMessageSenders(
					new WebServiceMessageSender[] { detectMessageSender() });
		}

		if (!CollectionUtils.isEmpty(this.webServiceMessageSenderCustomizers)) {
			if (!ObjectUtils.isEmpty(webServiceTemplate.getMessageSenders())) {
				for (WebServiceMessageSender webServiceMessageSender : webServiceTemplate
						.getMessageSenders()) {
					for (WebServiceMessageSenderCustomizer webServiceMessageSenderCustomizer : this.webServiceMessageSenderCustomizers) {
						webServiceMessageSenderCustomizer
								.customize(webServiceMessageSender);
					}
				}
			}
		}
	}

	private WebServiceMessageSender detectMessageSender() {
		ClassLoader classLoader = getClass().getClassLoader();
		if (ClassUtils.isPresent("org.apache.http.client.HttpClient", classLoader)) {
			return new HttpComponentsMessageSender();
		}
		return new HttpUrlConnectionMessageSender();
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

	private interface WebServiceMessageSenderCustomizer {

		void customize(WebServiceMessageSender webServiceMessageSender);

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

	/**
	 * {@link WebServiceMessageSenderCustomizer} to set connection timeout.
	 */
	private static final class ConnectionTimeoutWebServiceMessageSenderCustomizer
			implements WebServiceMessageSenderCustomizer {

		private final Duration timeout;

		private ConnectionTimeoutWebServiceMessageSenderCustomizer(Duration timeout) {
			this.timeout = timeout;
		}

		@Override
		public void customize(WebServiceMessageSender webServiceMessageSender) {
			if (webServiceMessageSender instanceof HttpComponentsMessageSender) {
				((HttpComponentsMessageSender) webServiceMessageSender)
						.setConnectionTimeout(Math.toIntExact(this.timeout.toMillis()));

			}
			else if (webServiceMessageSender instanceof HttpUrlConnectionMessageSender) {
				((HttpUrlConnectionMessageSender) webServiceMessageSender)
						.setConnectionTimeout(this.timeout);
			}
			else {
				throw new IllegalStateException("There is no way to customize '"
						+ webServiceMessageSender.getClass() + "' with '"
						+ "connectionTimeout'. Please use a custom " + "customizer.");
			}
		}

	}

	/**
	 * {@link WebServiceMessageSenderCustomizer} to set read timeout.
	 */
	private static final class ReadTimeoutWebServiceMessageSenderCustomizer
			implements WebServiceMessageSenderCustomizer {

		private final Duration timeout;

		private ReadTimeoutWebServiceMessageSenderCustomizer(Duration timeout) {
			this.timeout = timeout;
		}

		@Override
		public void customize(WebServiceMessageSender webServiceMessageSender) {
			if (webServiceMessageSender instanceof HttpComponentsMessageSender) {
				((HttpComponentsMessageSender) webServiceMessageSender)
						.setReadTimeout(Math.toIntExact(this.timeout.toMillis()));

			}
			else if (webServiceMessageSender instanceof HttpUrlConnectionMessageSender) {
				((HttpUrlConnectionMessageSender) webServiceMessageSender)
						.setReadTimeout(this.timeout);
			}
			else {
				throw new IllegalStateException("There is no way to customize '"
						+ webServiceMessageSender.getClass() + "' with '"
						+ "readTimeout'. Please use a custom " + "customizer.");
			}

		}

	}

}
