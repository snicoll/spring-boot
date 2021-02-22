/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.influx;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectAssert;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.LogLevel;
import org.influxdb.impl.BatchProcessor;
import org.junit.jupiter.api.Test;
import retrofit2.Retrofit;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfluxDbAutoConfiguration}.
 *
 * @author Sergey Kuptsov
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class InfluxDbAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(InfluxDbAutoConfiguration.class));

	@Test
	void influxDbRequiresUrl() {
		this.contextRunner.run((context) -> assertThat(context.getBeansOfType(InfluxDB.class)).isEmpty());
	}

	@Test
	void influxDbCanBeCustomized() {
		this.contextRunner.withPropertyValues("spring.influx.url=http://localhost", "spring.influx.password=password",
				"spring.influx.user=user", "spring.influx.database=sample-db",
				"spring.influx.retention-policy=two_hours", "spring.influx.consistency-level=all",
				"spring.influx.log=basic", "spring.influx.gzip-enabled=true").run((context) -> {
					assertThat(context).hasSingleBean(InfluxDB.class);
					InfluxDB influxDb = context.getBean(InfluxDB.class);
					assertThat(influxDb).hasFieldOrPropertyWithValue("database", "sample-db");
					assertThat(influxDb).hasFieldOrPropertyWithValue("retentionPolicy", "two_hours");
					assertThat(influxDb).hasFieldOrPropertyWithValue("consistency", InfluxDB.ConsistencyLevel.ALL);
					assertThat(influxDb).hasFieldOrPropertyWithValue("logLevel", LogLevel.BASIC);
					assertThat(influxDb).extracting("gzipRequestInterceptor").hasFieldOrPropertyWithValue("enabled",
							true);
				});
	}

	@Test
	void influxDbDoesNotUseBatchByDefault() {
		this.contextRunner.withPropertyValues("spring.influx.url=http://localhost").run((context) -> {
			assertThat(context).hasSingleBean(InfluxDB.class);
			InfluxDB influxDb = context.getBean(InfluxDB.class);
			assertThat(influxDb.isBatchEnabled()).isFalse();
		});
	}

	@Test
	void influxDbCanCustomizeBatchOptions() {
		this.contextRunner
				.withPropertyValues("spring.influx.url=http://localhost", "spring.influx.consistency-level=all",
						"spring.influx.batch.enabled=true", "spring.influx.batch.actions=50",
						"spring.influx.batch.flush-duration=5s", "spring.influx.batch.jitter-duration=500ms")
				.run((context) -> {
					assertThat(context).hasSingleBean(InfluxDB.class);
					InfluxDB influxDb = context.getBean(InfluxDB.class);
					ObjectAssert<BatchProcessor> batchProcessor = assertThat(influxDb).extracting("batchProcessor",
							InstanceOfAssertFactories.type(BatchProcessor.class));
					batchProcessor.hasFieldOrPropertyWithValue("actions", 50);
					batchProcessor.hasFieldOrPropertyWithValue("flushInterval", (int) Duration.ofSeconds(5).toMillis());
					batchProcessor.hasFieldOrPropertyWithValue("jitterInterval", 500);
					batchProcessor.hasFieldOrPropertyWithValue("consistencyLevel", InfluxDB.ConsistencyLevel.ALL);

				});
	}

	@Test
	void influxDbCanBeCreatedWithoutCredentials() {
		this.contextRunner.withPropertyValues("spring.influx.url=http://localhost").run((context) -> {
			assertThat(context).hasSingleBean(InfluxDB.class);
			int readTimeout = getReadTimeoutProperty(context);
			assertThat(readTimeout).isEqualTo(10_000);
		});
	}

	@Test
	void influxDbWithOkHttpClientBuilderProvider() {
		this.contextRunner.withUserConfiguration(CustomOkHttpClientBuilderProviderConfig.class)
				.withPropertyValues("spring.influx.url=http://localhost").run((context) -> {
					assertThat(context).hasSingleBean(InfluxDB.class);
					int readTimeout = getReadTimeoutProperty(context);
					assertThat(readTimeout).isEqualTo(40_000);
				});
	}

	@Test
	void influxDbWithCustomizer() {
		this.contextRunner.withBean(InfluxDbCustomizer.class, () -> (influxDb) -> influxDb.setDatabase("test"))
				.withPropertyValues("spring.influx.url=http://localhost", "spring.influx.database=sample-db")
				.run((context) -> {
					assertThat(context).hasSingleBean(InfluxDB.class);
					InfluxDB influxDb = context.getBean(InfluxDB.class);
					assertThat(influxDb).hasFieldOrPropertyWithValue("database", "test");
				});
	}

	private int getReadTimeoutProperty(AssertableApplicationContext context) {
		InfluxDB influxDB = context.getBean(InfluxDB.class);
		Retrofit retrofit = (Retrofit) ReflectionTestUtils.getField(influxDB, "retrofit");
		OkHttpClient callFactory = (OkHttpClient) retrofit.callFactory();
		return callFactory.readTimeoutMillis();
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomOkHttpClientBuilderProviderConfig {

		@Bean
		InfluxDbOkHttpClientBuilderProvider influxDbOkHttpClientBuilderProvider() {
			return () -> new OkHttpClient.Builder().readTimeout(40, TimeUnit.SECONDS);
		}

	}

}
