/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.kafka.receiving.custom;

import java.util.Collections;

import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

@Configuration(proxyBeanMethods = false)
public class MyKafkaConfiguration {

	@Bean
	public ConcurrentKafkaListenerContainerFactory<Object, Object> ordersFactory(
			ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
			DefaultKafkaConsumerFactoryConfigurer consumerFactoryConfigurer) {
		DefaultKafkaConsumerFactory<Object, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(
				Collections.emptyMap());
		consumerFactoryConfigurer.configure(consumerFactory, "orders");
		ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		configurer.configure(factory, consumerFactory, "orders");
		return factory;
	}

}
