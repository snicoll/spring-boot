/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.kafka.receiving.custom

import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer
import org.springframework.boot.kafka.autoconfigure.DefaultKafkaConsumerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@Configuration(proxyBeanMethods = false)
class MyKafkaConfiguration {

	@Bean
	fun ordersFactory(configurer: ConcurrentKafkaListenerContainerFactoryConfigurer,
			consumerFactoryConfigurer: DefaultKafkaConsumerFactoryConfigurer):
			ConcurrentKafkaListenerContainerFactory<Any, Any> {
		val consumerFactory = DefaultKafkaConsumerFactory<Any, Any>(emptyMap())
		consumerFactoryConfigurer.configure(consumerFactory, "orders")
		val factory = ConcurrentKafkaListenerContainerFactory<Any, Any>()
		configurer.configure(factory, consumerFactory, "orders")
		return factory
	}

}
