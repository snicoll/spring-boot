/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import java.time.Duration;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.micrometer.KafkaListenerObservation.DefaultKafkaListenerObservationConvention;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

/**
 * Tests for {@link ConcurrentKafkaListenerContainerFactoryConfigurer}.
 *
 * @author Moritz Halbritter
 */
class ConcurrentKafkaListenerContainerFactoryConfigurerTests {

	private ConcurrentKafkaListenerContainerFactoryConfigurer configurer;

	private ConcurrentKafkaListenerContainerFactory<Object, Object> factory;

	private ConsumerFactory<Object, Object> consumerFactory;

	private KafkaProperties properties;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		this.configurer = new ConcurrentKafkaListenerContainerFactoryConfigurer();
		this.properties = new KafkaProperties();
		this.configurer.setKafkaProperties(this.properties);
		this.factory = spy(new ConcurrentKafkaListenerContainerFactory<>());
		this.consumerFactory = mock(ConsumerFactory.class);
	}

	@Test
	void shouldApplyKafkaAdmin() {
		KafkaAdmin kafkaAdmin = mock();
		this.configurer.setKafkaAdmin(kafkaAdmin);
		this.configurer.configure(this.factory, this.consumerFactory);
		ConcurrentMessageListenerContainer<Object, Object> container = this.factory.createContainer("test");
		assertThat(container.getKafkaAdmin()).isSameAs(kafkaAdmin);
	}

	@Test
	void shouldApplyThreadNameSupplier() {
		Function<MessageListenerContainer, String> function = (container) -> "thread-1";
		this.configurer.setThreadNameSupplier(function);
		this.configurer.configure(this.factory, this.consumerFactory);
		then(this.factory).should().setThreadNameSupplier(function);
	}

	@Test
	void shouldApplyChangeConsumerThreadName() {
		this.properties.getListener().setChangeConsumerThreadName(true);
		this.configurer.configure(this.factory, this.consumerFactory);
		then(this.factory).should().setChangeConsumerThreadName(true);
	}

	@Test
	void shouldApplyListenerTaskExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		this.configurer.setListenerTaskExecutor(executor);
		this.configurer.configure(this.factory, this.consumerFactory);
		assertThat(this.factory.getContainerProperties().getListenerTaskExecutor()).isEqualTo(executor);
	}

	@Test
	void shouldApplyAuthExceptionRetryInterval() {
		this.properties.getListener().setAuthExceptionRetryInterval(Duration.ofSeconds(10));
		this.configurer.configure(this.factory, this.consumerFactory);
		assertThat(this.factory.getContainerProperties().getAuthExceptionRetryInterval())
			.isEqualTo(Duration.ofSeconds(10));
	}

	@Test
	void shouldApplyObservationConvention() {
		DefaultKafkaListenerObservationConvention convention = new DefaultKafkaListenerObservationConvention();
		this.configurer.setObservationConvention(convention);
		this.configurer.configure(this.factory, this.consumerFactory);
		assertThat(this.factory.getContainerProperties().getObservationConvention()).isSameAs(convention);
	}

	@Test
	void shouldApplyContainerListenerSettingsOnTopOfListenerProperties() {
		MockEnvironment environment = new MockEnvironment().withProperty("spring.kafka.listener.concurrency", "2")
			.withProperty("spring.kafka.listener.ack-mode", "batch")
			.withProperty("spring.kafka.listener.observation-enabled", "true")
			.withProperty("spring.kafka.containers.test.listener.concurrency", "5")
			.withProperty("spring.kafka.containers.test.listener.ack-mode", "manual");
		useEnvironment(environment);
		this.configurer.configure(this.factory, this.consumerFactory, "test");
		ConcurrentMessageListenerContainer<Object, Object> container = this.factory.createContainer("test");
		assertThat(container.getConcurrency()).isEqualTo(5);
		assertThat(container.getContainerProperties().getAckMode()).isEqualTo(AckMode.MANUAL);
		assertThat(container.getContainerProperties().isObservationEnabled()).isTrue();
	}

	@Test
	void shouldNotAffectDefaultListenerSettingsWhenUsingContainer() {
		MockEnvironment environment = new MockEnvironment().withProperty("spring.kafka.listener.concurrency", "2")
			.withProperty("spring.kafka.containers.test.listener.concurrency", "5");
		useEnvironment(environment);
		this.configurer.configure(new ConcurrentKafkaListenerContainerFactory<>(), this.consumerFactory, "test");
		this.configurer.configure(this.factory, this.consumerFactory);
		assertThat(this.factory.createContainer("test").getConcurrency()).isEqualTo(2);
	}

	@Test
	void shouldFailWithUnknownContainer() {
		useEnvironment(new MockEnvironment());
		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.configurer.configure(this.factory, this.consumerFactory, "test"))
			.withMessageContaining("No Kafka container named 'test'");
	}

	private void useEnvironment(MockEnvironment environment) {
		Binder binder = Binder.get(environment);
		this.properties = binder.bindOrCreate("spring.kafka", KafkaProperties.class);
		this.configurer.setKafkaProperties(this.properties);
		this.configurer.setContainerProperties(new KafkaContainerProperties(this.properties, binder));
	}

}
