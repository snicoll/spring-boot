/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.hazelcast;

import java.io.IOException;
import java.io.InputStream;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientConfigRecognizer;
import com.hazelcast.config.ConfigStream;
import com.hazelcast.core.HazelcastInstance;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Builder;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Configuration for Hazelcast client.
 *
 * @author Vedran Pavic
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(HazelcastClient.class)
@ConditionalOnMissingBean(HazelcastInstance.class)
class HazelcastClientConfiguration {

	static final String CONFIG_SYSTEM_PROPERTY = "hazelcast.client.config";

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(ClientConfig.class)
	@Conditional(ConfigAvailableCondition.class)
	static class HazelcastClientConfigFileConfiguration {

		@Bean
		HazelcastInstance hazelcastInstance(HazelcastProperties properties) throws IOException {
			Resource config = properties.resolveConfigLocation();
			if (config != null) {
				return new HazelcastClientFactory(config).getHazelcastInstance();
			}
			return HazelcastClient.newHazelcastClient();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(ClientConfig.class)
	static class HazelcastClientConfigConfiguration {

		@Bean
		HazelcastInstance hazelcastInstance(ClientConfig config) {
			return new HazelcastClientFactory(config).getHazelcastInstance();
		}

	}

	/**
	 * {@link HazelcastConfigResourceCondition} that checks if the
	 * {@code spring.hazelcast.config} configuration key is defined.
	 */
	static class ConfigAvailableCondition extends HazelcastConfigResourceCondition {

		ConfigAvailableCondition() {
			super(CONFIG_SYSTEM_PROPERTY, "file:./hazelcast-client.xml", "classpath:/hazelcast-client.xml",
					"file:./hazelcast-client.yaml", "classpath:/hazelcast-client.yaml");
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			if (context.getEnvironment().containsProperty(HAZELCAST_CONFIG_PROPERTY)) {
				ConditionOutcome configValidationOutcome = Hazelcast4ClientValidation.clientConfigOutcome(context,
						HAZELCAST_CONFIG_PROPERTY, startConditionMessage());
				return (configValidationOutcome != null) ? configValidationOutcome : ConditionOutcome
						.match(startConditionMessage().foundExactly("property " + HAZELCAST_CONFIG_PROPERTY));
			}
			return getResourceOutcome(context, metadata);
		}

	}

	static class Hazelcast4ClientValidation {

		static ConditionOutcome clientConfigOutcome(ConditionContext context, String propertyName, Builder builder) {
			String resourcePath = context.getEnvironment().getProperty(propertyName);
			Resource resource = context.getResourceLoader().getResource(resourcePath);
			if (!resource.exists()) {
				return ConditionOutcome.noMatch(
						builder.because("property '" + propertyName + "' points to a resource that does not exist"));
			}
			try (InputStream in = resource.getInputStream()) {
				boolean clientConfig = new ClientConfigRecognizer().isRecognized(new ConfigStream(in));
				return (clientConfig)
						? ConditionOutcome.match(builder.because("Hazelcast client configuration detected"))
						: ConditionOutcome.noMatch(builder.because("Hazelcast server configuration detected"));
			}
			catch (Throwable ex) { // Hazelcast 4 specific API
				return null;
			}

		}

	}

}
