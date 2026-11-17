/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link DefaultKafkaConsumerFactoryConfigurer}.
 *
 * @author Stephane Nicoll
 */
class DefaultKafkaConsumerFactoryConfigurerTests {

	private final MockEnvironment environment = new MockEnvironment();

	@Test
	void configureUsesConsumerProperties() {
		this.environment.withProperty("spring.kafka.bootstrap-servers", "base:9092")
			.withProperty("spring.kafka.consumer.group-id", "default-group");
		DefaultKafkaConsumerFactory<Object, Object> consumerFactory = createConsumerFactory();
		createConfigurer(null).configure(consumerFactory);
		assertThat(consumerFactory.getConfigurationProperties())
			.containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("base:9092"))
			.containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "default-group");
	}

	@Test
	void configureAppliesConnectionDetails() {
		this.environment.withProperty("spring.kafka.bootstrap-servers", "base:9092");
		DefaultKafkaConsumerFactory<Object, Object> consumerFactory = createConsumerFactory();
		createConfigurer(() -> List.of("service-connection:9092")).configure(consumerFactory);
		assertThat(consumerFactory.getConfigurationProperties()).containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
				List.of("service-connection:9092"));
	}

	@Test
	void configureWithContainerNameAppliesContainerSettingsOnTopOfConsumerProperties() {
		this.environment.withProperty("spring.kafka.bootstrap-servers", "base:9092")
			.withProperty("spring.kafka.consumer.group-id", "default-group")
			.withProperty("spring.kafka.consumer.auto-offset-reset", "earliest")
			.withProperty("spring.kafka.containers.test.consumer.group-id", "test-group")
			.withProperty("spring.kafka.containers.test.consumer.value-deserializer", LongDeserializer.class.getName())
			.withProperty("spring.kafka.containers.test.consumer.properties[test.custom]", "value");
		DefaultKafkaConsumerFactory<Object, Object> consumerFactory = createConsumerFactory();
		createConfigurer(null).configure(consumerFactory, "test");
		assertThat(consumerFactory.getConfigurationProperties())
			.containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("base:9092"))
			.containsEntry(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
			.containsEntry(ConsumerConfig.GROUP_ID_CONFIG, "test-group")
			.containsEntry(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class)
			.containsEntry("test.custom", "value");
	}

	@Test
	void configureWithContainerNameAndPropertiesConnectionDetailsUsesContainerConnectionSettings() {
		this.environment.withProperty("spring.kafka.bootstrap-servers", "base:9092")
			.withProperty("spring.kafka.security.protocol", "SSL")
			.withProperty("spring.kafka.containers.test.consumer.bootstrap-servers", "other-cluster:9092")
			.withProperty("spring.kafka.containers.test.consumer.security.protocol", "SASL_SSL");
		KafkaProperties properties = bindKafkaProperties();
		DefaultKafkaConsumerFactory<Object, Object> consumerFactory = createConsumerFactory();
		createConfigurer(properties, new PropertiesKafkaConnectionDetails(properties, null)).configure(consumerFactory,
				"test");
		assertThat(consumerFactory.getConfigurationProperties())
			.containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, List.of("other-cluster:9092"))
			.containsEntry(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
	}

	@Test
	void configureWithContainerNameAndPropertiesConnectionDetailsFallsBackToCommonConnectionSettings() {
		this.environment.withProperty("spring.kafka.bootstrap-servers", "base:9092")
			.withProperty("spring.kafka.containers.test.consumer.group-id", "test-group");
		KafkaProperties properties = bindKafkaProperties();
		DefaultKafkaConsumerFactory<Object, Object> consumerFactory = createConsumerFactory();
		createConfigurer(properties, new PropertiesKafkaConnectionDetails(properties, null)).configure(consumerFactory,
				"test");
		assertThat(consumerFactory.getConfigurationProperties()).containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
				List.of("base:9092"));
	}

	@Test
	void configureWithContainerNameAndServiceConnectionUsesServiceConnection() {
		this.environment.withProperty("spring.kafka.containers.test.consumer.bootstrap-servers", "other-cluster:9092");
		DefaultKafkaConsumerFactory<Object, Object> consumerFactory = createConsumerFactory();
		createConfigurer(() -> List.of("service-connection:9092")).configure(consumerFactory, "test");
		assertThat(consumerFactory.getConfigurationProperties()).containsEntry(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
				List.of("service-connection:9092"));
	}

	@Test
	void configureWithUnknownContainerNameThrowsException() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> createConfigurer(null).configure(createConsumerFactory(), "test"))
			.withMessageContaining("No Kafka container named 'test'");
	}

	@Test
	void configureDoesNotAffectSubsequentConfiguration() {
		this.environment.withProperty("spring.kafka.containers.test.consumer.group-id", "test-group");
		DefaultKafkaConsumerFactoryConfigurer configurer = createConfigurer(null);
		configurer.configure(createConsumerFactory(), "test");
		DefaultKafkaConsumerFactory<Object, Object> consumerFactory = createConsumerFactory();
		configurer.configure(consumerFactory);
		assertThat(consumerFactory.getConfigurationProperties()).doesNotContainKey(ConsumerConfig.GROUP_ID_CONFIG);
	}

	@Test
	void configureKeepsExistingConfigurationThatIsNotMapped() {
		DefaultKafkaConsumerFactory<Object, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(
				Map.of("custom", "value"));
		createConfigurer(null).configure(consumerFactory);
		assertThat(consumerFactory.getConfigurationProperties()).containsEntry("custom", "value");
	}

	@Test
	void configureAppliesCustomizersInOrder() {
		this.environment.withProperty("spring.kafka.containers.test.consumer.group-id", "test-group");
		DefaultKafkaConsumerFactoryCustomizer first = (factory) -> factory
			.updateConfigs(Map.of(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class));
		DefaultKafkaConsumerFactoryCustomizer second = (factory) -> factory
			.updateConfigs(Map.of(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class));
		DefaultKafkaConsumerFactory<Object, Object> consumerFactory = createConsumerFactory();
		KafkaProperties properties = bindKafkaProperties();
		new DefaultKafkaConsumerFactoryConfigurer(properties, containerProperties(properties), null,
				List.of(first, second))
			.configure(consumerFactory, "test");
		assertThat(consumerFactory.getConfigurationProperties())
			.containsEntry(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
	}

	private DefaultKafkaConsumerFactory<Object, Object> createConsumerFactory() {
		return new DefaultKafkaConsumerFactory<>(Collections.emptyMap());
	}

	private DefaultKafkaConsumerFactoryConfigurer createConfigurer(@Nullable KafkaConnectionDetails connectionDetails) {
		return createConfigurer(bindKafkaProperties(), connectionDetails);
	}

	private DefaultKafkaConsumerFactoryConfigurer createConfigurer(KafkaProperties properties,
			@Nullable KafkaConnectionDetails connectionDetails) {
		return new DefaultKafkaConsumerFactoryConfigurer(properties, containerProperties(properties), connectionDetails,
				Collections.emptyList());
	}

	private KafkaContainerProperties containerProperties(KafkaProperties properties) {
		return new KafkaContainerProperties(properties, Binder.get(this.environment));
	}

	private KafkaProperties bindKafkaProperties() {
		return Binder.get(this.environment).bindOrCreate("spring.kafka", KafkaProperties.class);
	}

}
