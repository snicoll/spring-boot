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

import java.util.Map;

import org.apache.kafka.common.config.SslConfigs;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.MutuallyExclusiveConfigurationPropertiesException;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link KafkaConfigBuilder}.
 *
 * @author Stephane Nicoll
 */
class KafkaConfigBuilderTests {

	interface CommonTests {

		Map<String, Object> build(KafkaProperties properties);

		@Test
		default void sslPemConfiguration() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setKeyStoreKey("-----BEGINkey");
			properties.getSsl().setTrustStoreCertificates("-----BEGINtrust");
			properties.getSsl().setKeyStoreCertificateChain("-----BEGINchain");
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, "-----BEGINkey");
			assertThat(config).containsEntry(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, "-----BEGINtrust");
			assertThat(config).containsEntry(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, "-----BEGINchain");
		}

		@Test
		default void sslPemConfigurationWithEmptyBundle() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setKeyStoreKey("-----BEGINkey");
			properties.getSsl().setTrustStoreCertificates("-----BEGINtrust");
			properties.getSsl().setKeyStoreCertificateChain("-----BEGINchain");
			properties.getSsl().setBundle("");
			Map<String, Object> config = build(properties);
			assertThat(config).containsEntry(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, "-----BEGINkey");
			assertThat(config).containsEntry(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, "-----BEGINtrust");
			assertThat(config).containsEntry(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, "-----BEGINchain");
		}

		@Test
		default void sslPropertiesWhenKeyStoreLocationAndKeySetShouldThrowException() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setKeyStoreKey("-----BEGIN");
			properties.getSsl().setKeyStoreLocation(new ClassPathResource("ksLoc"));
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> build(properties));
		}

		@Test
		default void sslPropertiesWhenTrustStoreLocationAndCertificatesSetShouldThrowException() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setTrustStoreLocation(new ClassPathResource("tsLoc"));
			properties.getSsl().setTrustStoreCertificates("-----BEGIN");
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> build(properties));
		}

		@Test
		default void sslPropertiesWhenKeyStoreLocationAndBundleSetShouldThrowException() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setBundle("myBundle");
			properties.getSsl().setKeyStoreLocation(new ClassPathResource("ksLoc"));
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> build(properties));
		}

		@Test
		default void sslPropertiesWhenKeyStoreKeyAndBundleSetShouldThrowException() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setBundle("myBundle");
			properties.getSsl().setKeyStoreKey("-----BEGIN");
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> build(properties));
		}

		@Test
		default void sslPropertiesWhenTrustStoreLocationAndBundleSetShouldThrowException() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setBundle("myBundle");
			properties.getSsl().setTrustStoreLocation(new ClassPathResource("tsLoc"));
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> build(properties));
		}

		@Test
		default void sslPropertiesWhenTrustStoreCertificatesAndBundleSetShouldThrowException() {
			KafkaProperties properties = new KafkaProperties();
			properties.getSsl().setBundle("myBundle");
			properties.getSsl().setTrustStoreCertificates("-----BEGIN");
			assertThatExceptionOfType(MutuallyExclusiveConfigurationPropertiesException.class)
				.isThrownBy(() -> build(properties));
		}

	}

	@Nested
	class AdminConfigTests implements CommonTests {

		@Override
		public Map<String, Object> build(KafkaProperties properties) {
			return KafkaConfigBuilder.of(properties).admin().build();
		}

	}

	@Nested
	class ConsumerConfigTests implements CommonTests {

		@Override
		public Map<String, Object> build(KafkaProperties properties) {
			return KafkaConfigBuilder.of(properties).consumer().build();
		}

	}

	@Nested
	class ProducerConfigTests implements CommonTests {

		@Override
		public Map<String, Object> build(KafkaProperties properties) {
			return KafkaConfigBuilder.of(properties).producer().build();
		}

	}

	@Nested
	class StreamsConfigTests implements CommonTests {

		@Override
		public Map<String, Object> build(KafkaProperties properties) {
			return KafkaConfigBuilder.of(properties).streams().build();
		}

	}

}
