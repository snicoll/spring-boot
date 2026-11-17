/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

/**
 * Configure {@link DefaultKafkaConsumerFactory} with sensible defaults tuned using
 * configuration properties and {@link KafkaConnectionDetails}, and apply any
 * {@link DefaultKafkaConsumerFactoryCustomizer customizers}.
 * <p>
 * Can be injected into application code and used to define a custom
 * {@code DefaultKafkaConsumerFactory} whose configuration is based upon that produced by
 * auto-configuration.
 *
 * @author Stephane Nicoll
 * @since 4.3.0
 */
public class DefaultKafkaConsumerFactoryConfigurer {

	private final KafkaProperties properties;

	private final KafkaContainerProperties containerProperties;

	private final @Nullable KafkaConnectionDetails connectionDetails;

	private final List<DefaultKafkaConsumerFactoryCustomizer> customizers;

	DefaultKafkaConsumerFactoryConfigurer(KafkaProperties properties, KafkaContainerProperties containerProperties,
			@Nullable KafkaConnectionDetails connectionDetails,
			List<DefaultKafkaConsumerFactoryCustomizer> customizers) {
		this.properties = properties;
		this.containerProperties = containerProperties;
		this.connectionDetails = connectionDetails;
		this.customizers = customizers;
	}

	/**
	 * Configure the specified consumer factory using the {@code spring.kafka.consumer.*}
	 * settings. The factory can be further tuned and default settings can be overridden.
	 * @param consumerFactory the {@link DefaultKafkaConsumerFactory} instance to
	 * configure
	 */
	public void configure(DefaultKafkaConsumerFactory<?, ?> consumerFactory) {
		configure(consumerFactory, this.properties.getConsumer());
	}

	/**
	 * Configure the specified consumer factory using the consumer settings of the
	 * container with the specified name, as defined by
	 * {@code spring.kafka.containers.<name>.consumer.*}. Those settings are applied on
	 * top of the {@code spring.kafka.consumer.*} settings. The factory can be further
	 * tuned and default settings can be overridden.
	 * @param consumerFactory the {@link DefaultKafkaConsumerFactory} instance to
	 * configure
	 * @param containerName the name of the container
	 * @throws IllegalArgumentException if no container with that name has been configured
	 */
	public void configure(DefaultKafkaConsumerFactory<?, ?> consumerFactory, String containerName) {
		configure(consumerFactory, this.containerProperties.getConsumer(containerName));
	}

	private void configure(DefaultKafkaConsumerFactory<?, ?> consumerFactory,
			KafkaProperties.Consumer consumerProperties) {
		Map<String, Object> config = KafkaConfigBuilder.of(this.properties)
			.consumer(consumerProperties)
			.withConnectionDetails(this.connectionDetails)
			.build();
		consumerFactory.updateConfigs(config);
		this.customizers.forEach((customizer) -> customizer.customize(consumerFactory));
	}

}
