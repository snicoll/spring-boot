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

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.influxdb.client.InfluxDBClientOptions.Builder;
import okhttp3.OkHttpClient;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for InfluxDB 2.x.
 *
 * @author Stephane Nicoll
 * @since 2.5.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(InfluxDBClient.class)
@EnableConfigurationProperties(InfluxDbProperties.class)
public class InfluxDbClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("spring.influx.url")
	public InfluxDBClient influxDbClient(InfluxDbProperties properties,
			ObjectProvider<InfluxDbOkHttpClientBuilderProvider> httpClientBuilder,
			ObjectProvider<InfluxDbClientOptionsCustomizer> customizers) {
		Builder builder = InfluxDBClientOptions.builder().url(properties.getUrl())
				.authenticate(properties.getUrl(), properties.getPassword().toCharArray())
				.okHttpClient(determineBuilder(httpClientBuilder.getIfAvailable()));
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return InfluxDBClientFactory.create(builder.build());
	}

	private static OkHttpClient.Builder determineBuilder(InfluxDbOkHttpClientBuilderProvider builder) {
		if (builder != null) {
			return builder.get();
		}
		return new OkHttpClient.Builder();
	}

}
