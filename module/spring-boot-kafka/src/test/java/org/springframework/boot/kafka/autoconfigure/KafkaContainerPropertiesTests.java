/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import java.time.Duration;

import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Consumer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Listener;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link KafkaContainerProperties}.
 *
 * @author Stephane Nicoll
 */
class KafkaContainerPropertiesTests {

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	void getConsumerInheritsDefaultConsumerSettings() {
		this.environment.withProperty("spring.kafka.consumer.group-id", "default-group")
			.withProperty("spring.kafka.consumer.auto-offset-reset", "earliest")
			.withProperty("spring.kafka.consumer.value-deserializer", LongDeserializer.class.getName())
			.withProperty("spring.kafka.containers.test.consumer.group-id", "test-group");
		Consumer consumer = createContainerProperties().getConsumer("test");
		assertThat(consumer.getGroupId()).isEqualTo("test-group");
		assertThat(consumer.getAutoOffsetReset()).isEqualTo("earliest");
		assertThat(consumer.getValueDeserializer()).isEqualTo(LongDeserializer.class);
		assertThat(consumer.getKeyDeserializer()).isEqualTo(StringDeserializer.class);
	}

	@Test
	void getConsumerMergesAdditionalProperties() {
		this.environment.withProperty("spring.kafka.consumer.properties[default.one]", "1")
			.withProperty("spring.kafka.consumer.properties[default.two]", "2")
			.withProperty("spring.kafka.containers.test.consumer.properties[default.two]", "two")
			.withProperty("spring.kafka.containers.test.consumer.properties[test.three]", "3");
		Consumer consumer = createContainerProperties().getConsumer("test");
		assertThat(consumer.getProperties()).containsOnly(entry("default.one", "1"), entry("default.two", "two"),
				entry("test.three", "3"));
	}

	@Test
	void getConsumerReplacesBootstrapServers() {
		this.environment.withProperty("spring.kafka.consumer.bootstrap-servers", "a:9092,b:9092")
			.withProperty("spring.kafka.containers.test.consumer.bootstrap-servers", "other:9092");
		Consumer consumer = createContainerProperties().getConsumer("test");
		assertThat(consumer.getBootstrapServers()).containsExactly("other:9092");
	}

	@Test
	void getConsumerDoesNotAffectDefaultConsumer() {
		this.environment.withProperty("spring.kafka.consumer.group-id", "default-group")
			.withProperty("spring.kafka.containers.test.consumer.group-id", "test-group");
		KafkaProperties properties = bindKafkaProperties();
		new KafkaContainerProperties(properties, Binder.get(this.environment)).getConsumer("test");
		assertThat(properties.getConsumer().getGroupId()).isEqualTo("default-group");
	}

	@Test
	void getConsumerForContainerWithOnlyListenerSettingsUsesDefaultConsumer() {
		this.environment.withProperty("spring.kafka.consumer.group-id", "default-group")
			.withProperty("spring.kafka.containers.test.listener.concurrency", "3");
		Consumer consumer = createContainerProperties().getConsumer("test");
		assertThat(consumer.getGroupId()).isEqualTo("default-group");
	}

	@Test
	void getListenerInheritsDefaultListenerSettings() {
		this.environment.withProperty("spring.kafka.listener.concurrency", "2")
			.withProperty("spring.kafka.listener.observation-enabled", "true")
			.withProperty("spring.kafka.listener.poll-timeout", "10s")
			.withProperty("spring.kafka.containers.test.listener.concurrency", "5")
			.withProperty("spring.kafka.containers.test.listener.ack-mode", "manual");
		Listener listener = createContainerProperties().getListener("test");
		assertThat(listener.getConcurrency()).isEqualTo(5);
		assertThat(listener.getAckMode()).isEqualTo(AckMode.MANUAL);
		assertThat(listener.isObservationEnabled()).isTrue();
		assertThat(listener.getPollTimeout()).isEqualTo(Duration.ofSeconds(10));
	}

	@Test
	void getConsumerWithUnknownContainerThrowsException() {
		this.environment.withProperty("spring.kafka.containers.test.consumer.group-id", "test-group");
		assertThatIllegalArgumentException().isThrownBy(() -> createContainerProperties().getConsumer("tset"))
			.withMessage("No Kafka container named 'tset' has been configured using 'spring.kafka.containers'");
	}

	@Test
	void getListenerWithUnknownContainerThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> createContainerProperties().getListener("test"))
			.withMessage("No Kafka container named 'test' has been configured using 'spring.kafka.containers'");
	}

	@Test
	void getConsumerWithDashedContainerName() {
		this.environment.withProperty("spring.kafka.containers.stock-quotes.consumer.group-id", "quotes");
		assertThat(createContainerProperties().getConsumer("stock-quotes").getGroupId()).isEqualTo("quotes");
	}

	@Test
	void getConsumerWithCommonClientIdAndNoContainerClientIdThrowsException() {
		this.environment.withProperty("spring.kafka.client-id", "app")
			.withProperty("spring.kafka.containers.test.consumer.group-id", "test-group");
		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
			.isThrownBy(() -> createContainerProperties().getConsumer("test"))
			.satisfies((ex) -> assertThat(ex.getName()).isEqualTo("spring.kafka.containers.test.consumer.client-id"))
			.withMessageContaining("A client id is set using 'spring.kafka.client-id'");
	}

	@Test
	void getConsumerWithConsumerClientIdAndNoContainerClientIdThrowsException() {
		this.environment.withProperty("spring.kafka.client-id", "app")
			.withProperty("spring.kafka.consumer.client-id", "app-consumer")
			.withProperty("spring.kafka.containers.test.consumer.group-id", "test-group");
		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
			.isThrownBy(() -> createContainerProperties().getConsumer("test"))
			.satisfies((ex) -> assertThat(ex.getName()).isEqualTo("spring.kafka.containers.test.consumer.client-id"))
			.withMessageContaining("A client id is set using 'spring.kafka.consumer.client-id'");
	}

	@Test
	void getConsumerWithInheritedClientIdAndContainerClientId() {
		this.environment.withProperty("spring.kafka.client-id", "app")
			.withProperty("spring.kafka.consumer.client-id", "app-consumer")
			.withProperty("spring.kafka.containers.test.consumer.client-id", "test");
		assertThat(createContainerProperties().getConsumer("test").getClientId()).isEqualTo("test");
	}

	@Test
	void getConsumerWithListenerClientIdOnlyDoesNotRequireContainerConsumerClientId() {
		this.environment.withProperty("spring.kafka.listener.client-id", "app")
			.withProperty("spring.kafka.containers.test.consumer.group-id", "test-group");
		assertThat(createContainerProperties().getConsumer("test").getClientId()).isNull();
	}

	@Test
	void getListenerWithListenerClientIdAndNoContainerClientIdThrowsException() {
		this.environment.withProperty("spring.kafka.listener.client-id", "app")
			.withProperty("spring.kafka.containers.test.listener.concurrency", "2");
		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
			.isThrownBy(() -> createContainerProperties().getListener("test"))
			.satisfies((ex) -> assertThat(ex.getName()).isEqualTo("spring.kafka.containers.test.listener.client-id"))
			.withMessageContaining("A client id is set using 'spring.kafka.listener.client-id'");
	}

	@Test
	void getListenerWithListenerClientIdAndContainerClientId() {
		this.environment.withProperty("spring.kafka.listener.client-id", "app")
			.withProperty("spring.kafka.containers.test.listener.client-id", "test");
		assertThat(createContainerProperties().getListener("test").getClientId()).isEqualTo("test");
	}

	@Test
	void getListenerWithConsumerClientIdOnlyDoesNotRequireContainerListenerClientId() {
		this.environment.withProperty("spring.kafka.client-id", "app")
			.withProperty("spring.kafka.containers.test.listener.concurrency", "2");
		assertThat(createContainerProperties().getListener("test").getClientId()).isNull();
	}

	private KafkaContainerProperties createContainerProperties() {
		return new KafkaContainerProperties(bindKafkaProperties(), Binder.get(this.environment));
	}

	private KafkaProperties bindKafkaProperties() {
		return Binder.get(this.environment).bindOrCreate("spring.kafka", KafkaProperties.class);
	}

}
