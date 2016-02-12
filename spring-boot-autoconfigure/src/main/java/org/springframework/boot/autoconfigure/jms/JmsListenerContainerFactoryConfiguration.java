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

package org.springframework.boot.autoconfigure.jms;

import javax.jms.ConnectionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.util.Assert;

/**
 *
 * @author Stephane Nicoll
 */
@Configuration
public abstract class JmsListenerContainerFactoryConfiguration {

	@Autowired(required = false)
	private DestinationResolver destinationResolver;

	@Autowired(required = false)
	private JtaTransactionManager transactionManager;

	@Autowired
	private JmsProperties properties;

	/**
	 * Create a new and pre-configured {@link DefaultJmsListenerContainerFactory} instance
	 * for the specified {@link ConnectionFactory}.
	 * @param connectionFactory the {@link ConnectionFactory} to use.
	 * @return a pre-configured {@link DefaultJmsListenerContainerFactory}
	 */
	public DefaultJmsListenerContainerFactory createJmsListenerContainerFactory(ConnectionFactory connectionFactory) {
		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
		applySettings(factory, connectionFactory);
		return factory;
	}

	/**
	 * Apply the default settings for the specified jms listener container factory. The
	 * factory can be further tuned and default settings can be overridden.
	 * @param factory the {@link DefaultJmsListenerContainerFactory} instance to configure
	 * @param connectionFactory the {@link ConnectionFactory} to use
	 */
	public void applySettings(DefaultJmsListenerContainerFactory factory, ConnectionFactory connectionFactory) {
		Assert.notNull(factory, "Factory must not be null");
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		factory.setConnectionFactory(connectionFactory);
		factory.setPubSubDomain(this.properties.isPubSubDomain());
		if (this.transactionManager != null) {
			factory.setTransactionManager(this.transactionManager);
		}
		else {
			factory.setSessionTransacted(true);
		}
		if (this.destinationResolver != null) {
			factory.setDestinationResolver(this.destinationResolver);
		}
		JmsProperties.Listener listener = this.properties.getListener();
		factory.setAutoStartup(listener.isAutoStartup());
		if (listener.getAcknowledgeMode() != null) {
			factory.setSessionAcknowledgeMode(listener.getAcknowledgeMode().getMode());
		}
		String concurrency = listener.formatConcurrency();
		if (concurrency != null) {
			factory.setConcurrency(concurrency);
		}
	}

}
