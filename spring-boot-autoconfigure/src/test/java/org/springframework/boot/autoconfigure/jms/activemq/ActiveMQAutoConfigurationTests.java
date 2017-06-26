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

package org.springframework.boot.autoconfigure.jms.activemq;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.test.rule.ContextLoader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;

/**
 * Tests for {@link ActiveMQAutoConfiguration}
 *
 * @author Andy Wilkinson
 * @author Aurélien Leboulanger
 * @author Stephane Nicoll
 */
public class ActiveMQAutoConfigurationTests {

	@Rule
	public ContextLoader contextLoader = new ContextLoader().autoConfig(
			ActiveMQAutoConfiguration.class, JmsAutoConfiguration.class);

	@Test
	public void brokerIsEmbeddedByDefault() {
		ConfigurableApplicationContext context = this.contextLoader
				.config(EmptyConfiguration.class).load();
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		assertThat(connectionFactory).isInstanceOf(ActiveMQConnectionFactory.class);
		String brokerUrl = ((ActiveMQConnectionFactory) connectionFactory).getBrokerURL();
		assertThat(brokerUrl).isEqualTo("vm://localhost?broker.persistent=false");
	}

	@Test
	public void configurationBacksOffWhenCustomConnectionFactoryExists() {
		ConfigurableApplicationContext context = this.contextLoader
				.config(CustomConnectionFactoryConfiguration.class).load();
		assertThat(mockingDetails(context.getBean(ConnectionFactory.class)).isMock())
				.isTrue();
	}

	@Test
	public void customPooledConnectionFactoryConfiguration() {
		ConfigurableApplicationContext context = this.contextLoader
				.config(EmptyConfiguration.class)
				.env("spring.activemq.pool.enabled:true",
						"spring.activemq.pool.maxConnections:256",
						"spring.activemq.pool.idleTimeout:512",
						"spring.activemq.pool.expiryTimeout:4096",
						"spring.activemq.pool.configuration.maximumActiveSessionPerConnection:1024",
						"spring.activemq.pool.configuration.timeBetweenExpirationCheckMillis:2048")
				.load();
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		assertThat(connectionFactory).isInstanceOf(PooledConnectionFactory.class);
		PooledConnectionFactory pooledConnectionFactory = (PooledConnectionFactory) connectionFactory;
		assertThat(pooledConnectionFactory.getMaxConnections()).isEqualTo(256);
		assertThat(pooledConnectionFactory.getIdleTimeout()).isEqualTo(512);
		assertThat(pooledConnectionFactory.getMaximumActiveSessionPerConnection())
				.isEqualTo(1024);
		assertThat(pooledConnectionFactory.getTimeBetweenExpirationCheckMillis())
				.isEqualTo(2048);
		assertThat(pooledConnectionFactory.getExpiryTimeout()).isEqualTo(4096);
	}

	@Test
	public void pooledConnectionFactoryConfiguration() throws JMSException {
		ConfigurableApplicationContext context = this.contextLoader
				.config(EmptyConfiguration.class).env("spring.activemq.pool.enabled:true")
				.load();
		ConnectionFactory connectionFactory = context.getBean(ConnectionFactory.class);
		assertThat(connectionFactory).isInstanceOf(PooledConnectionFactory.class);
		context.close();
		assertThat(connectionFactory.createConnection()).isNull();
	}


	@Configuration
	static class EmptyConfiguration {

	}

	@Configuration
	static class CustomConnectionFactoryConfiguration {

		@Bean
		public ConnectionFactory connectionFactory() {
			return mock(ConnectionFactory.class);
		}

	}

}
