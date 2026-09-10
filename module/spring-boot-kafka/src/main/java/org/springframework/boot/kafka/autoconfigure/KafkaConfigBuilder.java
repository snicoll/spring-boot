/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.kafka.autoconfigure;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.kafka.autoconfigure.KafkaConnectionDetails.Configuration;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Admin;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Producer;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Security;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Ssl;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties.Streams;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * Builder for  Kafka components based on {@link KafkaProperties}.
 * @author Stephane Nicoll
 */
public class KafkaConfigBuilder {

	private final KafkaProperties kafkaProperties;

	public KafkaConfigBuilder(KafkaProperties kafkaProperties) {
		this.kafkaProperties = kafkaProperties;
	}

	public AdminConfigBuilder admin() {
		return new AdminConfigBuilder(initializeKafkaConfig(), this.kafkaProperties.getAdmin());
	}

	public ConsumerConfigBuilder consumer() {
		return new ConsumerConfigBuilder(initializeKafkaConfig(), this.kafkaProperties.getConsumer());
	}

	public ProducerConfigBuilder producer() {
		return new ProducerConfigBuilder(initializeKafkaConfig(), this.kafkaProperties.getProducer());
	}

	public StreamsConfigBuilder streams() {
		return new StreamsConfigBuilder(initializeKafkaConfig(), this.kafkaProperties.getStreams());
	}

	protected KafkaProperties kafkaProperties() {
		return this.kafkaProperties;
	}

	protected KafkaConfig initializeKafkaConfig() {
		KafkaConfig kafkaConfig = new KafkaConfig();
		kafkaConfig.putIfNonNull(kafkaProperties()::getBootstrapServers, CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG);
		kafkaConfig.putIfNonNull(kafkaProperties()::getClientId, CommonClientConfigs.CLIENT_ID_CONFIG);
		kafkaConfig.putAll(kafkaProperties().getSsl().buildProperties());
		kafkaConfig.putAll(kafkaProperties().getSecurity().buildProperties());
		kafkaConfig.putAll(kafkaProperties().getProperties());
		return kafkaConfig;
	}

	protected void applySecurityProtocol(Map<String, Object> properties, @Nullable String securityProtocol) {
		if (StringUtils.hasLength(securityProtocol)) {
			properties.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
		}
	}

	protected void applySslBundle(Map<String, Object> properties, @Nullable SslBundle sslBundle) {
		if (sslBundle != null) {
			properties.put(SslConfigs.SSL_ENGINE_FACTORY_CLASS_CONFIG, SslBundleSslEngineFactory.class);
			properties.put(SslBundle.class.getName(), sslBundle);
		}
	}

	public final class AdminConfigBuilder {

		private final KafkaConfig kafkaConfig;

		private final KafkaProperties.Admin admin;

		private AdminConfigBuilder(KafkaConfig kafkaConfig, Admin admin) {
			this.kafkaConfig = kafkaConfig;
			this.admin = admin;
		}

		public AdminConfigBuilder withConnectionDetails(KafkaConnectionDetails connectionDetails) {
			Configuration admin = connectionDetails.getAdmin();
			this.kafkaConfig.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, admin.getBootstrapServers());
			applySecurityProtocol(this.kafkaConfig, admin.getSecurityProtocol());
			applySslBundle(this.kafkaConfig, admin.getSslBundle());
			return this;
		}

		public Map<String, Object> build() {
			apply();
			return this.kafkaConfig;
		}

		private void apply() {
			PropertyMapper map = PropertyMapper.get();
			map.from(this.admin::getClientId).to(this.kafkaConfig.in(ProducerConfig.CLIENT_ID_CONFIG));
			this.kafkaConfig.applySsl(this.admin.getSsl());
			this.kafkaConfig.applySecurity(this.admin.getSecurity());
			this.kafkaConfig.putAll(this.admin.getProperties());
		}

	}

	public final class ConsumerConfigBuilder {

		private final KafkaConfig kafkaConfig;

		private final KafkaProperties.Consumer consumer;

		private ConsumerConfigBuilder(KafkaConfig kafkaConfig, KafkaProperties.Consumer consumer) {
			this.kafkaConfig = kafkaConfig;
			this.consumer = consumer;
		}

		public ConsumerConfigBuilder withConnectionDetails(KafkaConnectionDetails connectionDetails) {
			Configuration consumer = connectionDetails.getConsumer();
			this.kafkaConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, consumer.getBootstrapServers());
			applySecurityProtocol(kafkaConfig, consumer.getSecurityProtocol());
			applySslBundle(kafkaConfig, consumer.getSslBundle());
			return this;
		}

		public Map<String, Object> build() {
			apply(this.kafkaConfig);
			return this.kafkaConfig;
		}

		private void apply(KafkaConfig kafkaConfig) {
			PropertyMapper map = PropertyMapper.get();
			map.from(this.consumer::getAutoCommitInterval)
				.asInt(Duration::toMillis)
				.to(kafkaConfig.in(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
			map.from(this.consumer::getAutoOffsetReset).to(kafkaConfig.in(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
			map.from(this.consumer::getBootstrapServers).to(kafkaConfig.in(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
			map.from(this.consumer::getClientId).to(kafkaConfig.in(ConsumerConfig.CLIENT_ID_CONFIG));
			map.from(this.consumer::getEnableAutoCommit).to(kafkaConfig.in(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
			map.from(this.consumer::getFetchMaxWait)
				.asInt(Duration::toMillis)
				.to(kafkaConfig.in(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG));
			map.from(this.consumer::getFetchMinSize)
				.asInt(DataSize::toBytes)
				.to(kafkaConfig.in(ConsumerConfig.FETCH_MIN_BYTES_CONFIG));
			map.from(this.consumer::getGroupId).to(kafkaConfig.in(ConsumerConfig.GROUP_ID_CONFIG));
			map.from(this.consumer::getHeartbeatInterval)
				.asInt(Duration::toMillis)
				.to(kafkaConfig.in(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG));
			map.from(() -> this.consumer.getIsolationLevel().name().toLowerCase(Locale.ROOT))
				.to(kafkaConfig.in(ConsumerConfig.ISOLATION_LEVEL_CONFIG));
			map.from(this.consumer::getKeyDeserializer)
				.to(kafkaConfig.in(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
			map.from(this.consumer::getValueDeserializer)
				.to(kafkaConfig.in(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
			map.from(this.consumer::getMaxPollRecords).to(kafkaConfig.in(ConsumerConfig.MAX_POLL_RECORDS_CONFIG));
			map.from(this.consumer::getMaxPollInterval)
				.asInt(Duration::toMillis)
				.to(kafkaConfig.in(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG));
			kafkaConfig.applySsl(this.consumer.getSsl());
			kafkaConfig.applySecurity(this.consumer.getSecurity());
			kafkaConfig.putAll(this.consumer.getProperties());
		}

	}

	public final class ProducerConfigBuilder {

		private final KafkaConfig kafkaConfig;

		private final KafkaProperties.Producer producer;

		private ProducerConfigBuilder(KafkaConfig kafkaConfig, Producer producer) {
			this.producer = producer;
			this.kafkaConfig = kafkaConfig;
		}

		public ProducerConfigBuilder withConnectionDetails(KafkaConnectionDetails connectionDetails) {
			Configuration producer = connectionDetails.getProducer();
			this.kafkaConfig.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, producer.getBootstrapServers());
			applySecurityProtocol(this.kafkaConfig, producer.getSecurityProtocol());
			applySslBundle(this.kafkaConfig, producer.getSslBundle());
			return this;
		}

		public Map<String, Object> build() {
			apply(this.kafkaConfig);
			return this.kafkaConfig;
		}

		private void apply(KafkaConfig properties) {
			PropertyMapper map = PropertyMapper.get();
			map.from(this.producer::getAcks).to(properties.in(ProducerConfig.ACKS_CONFIG));
			map.from(this.producer::getBatchSize)
				.asInt(DataSize::toBytes)
				.to(properties.in(ProducerConfig.BATCH_SIZE_CONFIG));
			map.from(this.producer::getBootstrapServers).to(properties.in(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
			map.from(this.producer::getBufferMemory)
				.as(DataSize::toBytes)
				.to(properties.in(ProducerConfig.BUFFER_MEMORY_CONFIG));
			map.from(this.producer::getClientId).to(properties.in(ProducerConfig.CLIENT_ID_CONFIG));
			map.from(this.producer::getCompressionType).to(properties.in(ProducerConfig.COMPRESSION_TYPE_CONFIG));
			map.from(this.producer::getKeySerializer).to(properties.in(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
			map.from(this.producer::getRetries).to(properties.in(ProducerConfig.RETRIES_CONFIG));
			map.from(this.producer::getValueSerializer).to(properties.in(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
			kafkaConfig.applySsl(this.producer.getSsl());
			kafkaConfig.applySecurity(this.producer.getSecurity());
			kafkaConfig.putAll(this.producer.getProperties());
		}

	}

	public final class StreamsConfigBuilder {

		private final KafkaConfig kafkaConfig;

		private final Streams streams;

		private StreamsConfigBuilder(KafkaConfig kafkaConfig, Streams streams) {
			this.kafkaConfig = kafkaConfig;
			this.streams = streams;
		}

		public StreamsConfigBuilder withConnectionDetails(KafkaConnectionDetails connectionDetails) {
			Configuration streams = connectionDetails.getStreams();
			this.kafkaConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, streams.getBootstrapServers());
			applySecurityProtocol(this.kafkaConfig, streams.getSecurityProtocol());
			applySslBundle(this.kafkaConfig, streams.getSslBundle());
			return this;
		}

		public Map<String, Object> build() {
			KafkaConfig kafkaConfig = initializeKafkaConfig();
			apply(kafkaConfig);
			return kafkaConfig;
		}

		private void apply(KafkaConfig kafkaConfig) {
			PropertyMapper map = PropertyMapper.get();
			map.from(this.streams::getApplicationId).to(kafkaConfig.in("application.id"));
			map.from(this.streams::getBootstrapServers)
				.to(kafkaConfig.in(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG));
			map.from(this.streams::getStateStoreCacheMaxSize)
				.asInt(DataSize::toBytes)
				.to(kafkaConfig.in("statestore.cache.max.bytes"));
			map.from(this.streams::getClientId).to(kafkaConfig.in(CommonClientConfigs.CLIENT_ID_CONFIG));
			map.from(this.streams::getReplicationFactor).to(kafkaConfig.in("replication.factor"));
			map.from(this.streams::getStateDir).to(kafkaConfig.in("state.dir"));
			kafkaConfig.applySsl(this.streams.getSsl());
			kafkaConfig.applySecurity(this.streams.getSecurity());
			kafkaConfig.putAll(this.streams.getProperties());
		}

	}

	protected static class KafkaConfig extends LinkedHashMap<String, Object> {

		void applySsl(Ssl ssl) {
			putAll(ssl.buildProperties());
		}

		void applySecurity(Security security) {
			putAll(security.buildProperties());
		}

		void putIfNonNull(Supplier<@Nullable Object> value, String key) {
			Object toSet = value.get();
			if (toSet != null) {
				this.put(key, toSet);
			}
		}

		<V> java.util.function.Consumer<V> in(String key) {
			return (value) -> put(key, value);
		}

	}

}
