/*
 * Copyright 2012-2014 the original author or authors.
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

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.jms.ConnectionFactory;

import org.hornetq.api.core.TransportConfiguration;
import org.hornetq.api.core.client.HornetQClient;
import org.hornetq.api.core.client.ServerLocator;
import org.hornetq.api.jms.HornetQJMSClient;
import org.hornetq.api.jms.JMSFactoryType;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.hornetq.core.remoting.impl.invm.InVMAcceptorFactory;
import org.hornetq.core.remoting.impl.invm.InVMConnectorFactory;
import org.hornetq.core.remoting.impl.netty.NettyConnectorFactory;
import org.hornetq.core.remoting.impl.netty.TransportConstants;
import org.hornetq.core.server.JournalType;
import org.hornetq.jms.client.HornetQConnectionFactory;
import org.hornetq.jms.server.config.JMSConfiguration;
import org.hornetq.jms.server.config.JMSQueueConfiguration;
import org.hornetq.jms.server.config.TopicConfiguration;
import org.hornetq.jms.server.config.impl.JMSConfigurationImpl;
import org.hornetq.jms.server.config.impl.JMSQueueConfigurationImpl;
import org.hornetq.jms.server.config.impl.TopicConfigurationImpl;
import org.hornetq.jms.server.embedded.EmbeddedJMS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jms.HornetQProperties.Embedded;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} to integrate with an HornetQ broker. Connect by default to a broker
 * available on the local machine with the default settings. If the necessary classes are
 * present, the broker can also be embedded in the application itself.
 * 
 * @author Stephane Nicoll
 * @since 1.1.0
 */
@Configuration
@AutoConfigureBefore(JmsTemplateAutoConfiguration.class)
@ConditionalOnClass({ ConnectionFactory.class, HornetQJMSClient.class })
@ConditionalOnMissingBean(ConnectionFactory.class)
@EnableConfigurationProperties(HornetQProperties.class)
public class HornetQAutoConfiguration {

	private static final String EMBEDDED_JMS_CLASS = "org.hornetq.jms.server.embedded.EmbeddedJMS";

	@Autowired
	private HornetQProperties properties;

	@Bean
	@ConditionalOnMissingBean
	public ConnectionFactory jmsConnectionFactory() {
		HornetQMode mode = this.properties.getMode();
		if (mode == null) {
			mode = deduceMode();
		}
		if (this.properties.getMode() == HornetQMode.embedded) {
			return createEmbeddedConnectionFactory();
		}
		return createNettyConnectionFactory();
	}

	private HornetQMode deduceMode() {
		if (this.properties.getEmbedded().isEnabled()
				&& ClassUtils.isPresent(EMBEDDED_JMS_CLASS, null)) {
			return HornetQMode.embedded;
		}
		return HornetQMode.netty;
	}

	private ConnectionFactory createEmbeddedConnectionFactory() {
		try {
			TransportConfiguration transportConfiguration = new TransportConfiguration(
					InVMConnectorFactory.class.getName());
			ServerLocator serviceLocator = HornetQClient
					.createServerLocatorWithoutHA(transportConfiguration);
			return new HornetQConnectionFactory(serviceLocator);
		}
		catch (NoClassDefFoundError ex) {
			throw new IllegalStateException("Unable to create emedded "
					+ "HornetQ connection, ensure that the hornet-jms-server.jar "
					+ "is the classpath", ex);
		}
	}

	private ConnectionFactory createNettyConnectionFactory() {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(TransportConstants.HOST_PROP_NAME, this.properties.getHost());
		params.put(TransportConstants.PORT_PROP_NAME, this.properties.getPort());
		TransportConfiguration transportConfiguration = new TransportConfiguration(
				NettyConnectorFactory.class.getName(), params);
		return HornetQJMSClient.createConnectionFactoryWithoutHA(JMSFactoryType.CF,
				transportConfiguration);
	}

	/**
	 * Configuration used to create the embedded hornet server.
	 */
	@Configuration
	@ConditionalOnClass(name = EMBEDDED_JMS_CLASS)
	@ConditionalOnExpression("${spring.hornetq.embedded.enabled:true}")
	static class EmbeddedServerConfiguration {

		@Autowired
		private HornetQProperties properties;

		@Autowired(required = false)
		private Collection<JMSQueueConfiguration> queuesConfiguration;

		@Autowired(required = false)
		private Collection<TopicConfiguration> topicsConfiguration;

		@Bean(initMethod = "start", destroyMethod = "stop")
		@ConditionalOnMissingBean
		public EmbeddedJMS hornetQServer(
				org.hornetq.core.config.Configuration hornetQConfiguration,
				JMSConfiguration hornetQJmsConfiguration) {
			EmbeddedJMS server = new EmbeddedJMS();
			server.setConfiguration(hornetQConfiguration);
			server.setJmsConfiguration(hornetQJmsConfiguration);
			server.setRegistry(new NoOpBindingRegistry());
			return server;
		}

		@Bean
		@ConditionalOnMissingBean
		public org.hornetq.core.config.Configuration hornetQConfiguration() {
			ConfigurationImpl configuration = new ConfigurationImpl();

			configuration.setSecurityEnabled(false);

			configure(this.properties.getEmbedded(), configuration);

			configuration.getAcceptorConfigurations().add(
					new TransportConfiguration(InVMAcceptorFactory.class.getName()));

			// https://issues.jboss.org/browse/HORNETQ-1143
			configuration.setClusterPassword("SpringBootRules");
			return configuration;
		}

		private void configure(Embedded embedded, ConfigurationImpl configuration) {
			// NOTE: Relies on the optional hornetq-jms-server.jar so cannot be moved to
			// the HornetQProperties
			configuration.setPersistenceEnabled(embedded.isPersistent());

			// https://issues.jboss.org/browse/HORNETQ-1302
			String rootDataDir = (embedded.getDataDirectory() != null ? embedded
					.getDataDirectory() : createDataDir());
			configuration.setJournalDirectory(rootDataDir + "/journal");

			if (embedded.isPersistent()) {
				// Force fallback to NIO
				configuration.setJournalType(JournalType.NIO);

				// Those directories should be configured separately.
				configuration.setLargeMessagesDirectory(rootDataDir + "/largemessages");
				configuration.setBindingsDirectory(rootDataDir + "/bindings");
				configuration.setPagingDirectory(rootDataDir + "/paging");
			}
		}

		private String createDataDir() {
			String tmpDir = System.getProperty("java.io.tmpdir");
			return new File(tmpDir, "hornetq-data").getAbsolutePath();
		}

		@Bean
		@ConditionalOnMissingBean
		public JMSConfiguration hornetQJmsConfiguration() {
			JMSConfiguration jmsConfig = new JMSConfigurationImpl();

			if (this.queuesConfiguration != null) {
				jmsConfig.getQueueConfigurations().addAll(this.queuesConfiguration);
			}
			if (this.topicsConfiguration != null) {
				jmsConfig.getTopicConfigurations().addAll(this.topicsConfiguration);
			}

			for (String queue : this.properties.getEmbedded().getQueues()) {
				JMSQueueConfiguration queueConfig = createSimpleQueueConfiguration(queue);
				jmsConfig.getQueueConfigurations().add(queueConfig);
			}
			for (String topic : this.properties.getEmbedded().getTopics()) {
				TopicConfiguration topicConfig = createSimpleTopicConfiguration(topic);
				jmsConfig.getTopicConfigurations().add(topicConfig);
			}

			return jmsConfig;
		}

		private JMSQueueConfiguration createSimpleQueueConfiguration(String name) {
			return new JMSQueueConfigurationImpl(name, null, this.properties
					.getEmbedded().isPersistent(), "/queue/" + name);
		}

		private TopicConfiguration createSimpleTopicConfiguration(String name) {
			return new TopicConfigurationImpl(name, "/topic/" + name);
		}

	}

}
