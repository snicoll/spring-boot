/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Consumer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Listener;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Provide the settings of the named containers defined using
 * {@code spring.kafka.containers.*}. The settings of a named container are bound on top
 * of the {@code spring.kafka.consumer.*} and {@code spring.kafka.listener.*} settings, so
 * that only the settings that are explicitly set for the container are overridden.
 *
 * @author Stephane Nicoll
 */
final class KafkaContainerProperties {

	private static final ConfigurationPropertyName CONTAINERS = ConfigurationPropertyName.of("spring.kafka.containers");

	private static final ConfigurationPropertyName CONSUMER = ConfigurationPropertyName.of("spring.kafka.consumer");

	private static final ConfigurationPropertyName LISTENER = ConfigurationPropertyName.of("spring.kafka.listener");

	private static final ConfigurationPropertyName COMMON_CLIENT_ID = ConfigurationPropertyName
		.of("spring.kafka.client-id");

	private final KafkaProperties properties;

	private final Binder binder;

	KafkaContainerProperties(KafkaProperties properties, Binder binder) {
		this.properties = properties;
		this.binder = binder;
	}

	/**
	 * Return the consumer settings of the container with the specified name.
	 * @param name the name of the container
	 * @return the consumer settings
	 * @throws IllegalArgumentException if no such container exists
	 * @throws InvalidConfigurationPropertyValueException if the container inherits a
	 * client id rather than defining its own
	 */
	Consumer getConsumer(String name) {
		Consumer consumer = bind(name, CONSUMER, "consumer", Consumer::new);
		assertClientIdIsNotInherited(name, "consumer", inheritedConsumerClientId());
		return consumer;
	}

	/**
	 * Return the listener settings of the container with the specified name.
	 * @param name the name of the container
	 * @return the listener settings
	 * @throws IllegalArgumentException if no such container exists
	 * @throws InvalidConfigurationPropertyValueException if the container inherits a
	 * client id rather than defining its own
	 */
	Listener getListener(String name) {
		Listener listener = bind(name, LISTENER, "listener", Listener::new);
		assertClientIdIsNotInherited(name, "listener",
				StringUtils.hasText(this.properties.getListener().getClientId()) ? LISTENER.append("client-id") : null);
		return listener;
	}

	private @Nullable ConfigurationPropertyName inheritedConsumerClientId() {
		if (StringUtils.hasText(this.properties.getConsumer().getClientId())) {
			return CONSUMER.append("client-id");
		}
		if (StringUtils.hasText(this.properties.getClientId())) {
			return COMMON_CLIENT_ID;
		}
		return null;
	}

	/**
	 * Consumers of a container use the client id as a prefix. A client id that is
	 * inherited by several containers leads to clients with the same id, so a container
	 * must define its own client id if one is set at a higher level.
	 * @param name the name of the container
	 * @param element the element of the container that is checked
	 * @param inheritedClientId the property that defines the client id the container
	 * would inherit, or {@code null}
	 */
	private void assertClientIdIsNotInherited(String name, String element,
			@Nullable ConfigurationPropertyName inheritedClientId) {
		if (inheritedClientId == null) {
			return;
		}
		ConfigurationPropertyName clientId = CONTAINERS.append(name).append(element).append("client-id");
		if (!this.binder.bind(clientId, Bindable.of(String.class)).isBound()) {
			throw new InvalidConfigurationPropertyValueException(clientId.toString(), null,
					"A client id is set using '%s'. Each container must define its own client id so that its clients can be identified."
						.formatted(inheritedClientId));
		}
	}

	private <T> T bind(String name, ConfigurationPropertyName defaults, String element, Supplier<T> factory) {
		Assert.isTrue(this.properties.getContainers().containsKey(name),
				() -> "No Kafka container named '%s' has been configured using '%s'".formatted(name, CONTAINERS));
		T target = factory.get();
		Bindable<T> bindable = Bindable.ofInstance(target);
		this.binder.bind(defaults, bindable);
		this.binder.bind(CONTAINERS.append(name).append(element), bindable);
		return target;
	}

}
