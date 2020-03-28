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
import java.net.URL;
import java.util.Arrays;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.config.YamlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.context.SpringManagedContext;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

/**
 * Factory that can be used to create a {@link HazelcastInstance}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.3.0
 */
public class HazelcastInstanceFactory {

	private final Config config;

	/**
	 * Create a {@link HazelcastInstanceFactory} for the specified configuration location.
	 * @param configLocation the location of the configuration file
	 * @param configCustomizers the {@linkplain HazelcastConfigCustomizer customizers} to
	 * apply to the configuration
	 * @throws IOException if the configuration location could not be read
	 */
	public HazelcastInstanceFactory(Resource configLocation, HazelcastConfigCustomizer... configCustomizers)
			throws IOException {
		this.config = getConfig(configLocation, configCustomizers);
	}

	/**
	 * Create a {@link HazelcastInstanceFactory} for the specified configuration.
	 * @param config the configuration
	 */
	public HazelcastInstanceFactory(Config config) {
		Assert.notNull(config, "Config must not be null");
		this.config = config;
	}

	private static Config getConfig(Resource configLocation, HazelcastConfigCustomizer... configCustomizers)
			throws IOException {
		Assert.notNull(configLocation, "ConfigLocation must not be null");
		URL configUrl = configLocation.getURL();
		Config config = createConfig(configUrl);
		if (ResourceUtils.isFileURL(configUrl)) {
			config.setConfigurationFile(configLocation.getFile());
		}
		else {
			config.setConfigurationUrl(configUrl);
		}
		if (!ObjectUtils.isEmpty(configCustomizers)) {
			Arrays.stream(configCustomizers).forEach((customizer) -> customizer.customize(config));
		}
		return config;
	}

	private static Config createConfig(URL configUrl) throws IOException {
		String configFileName = configUrl.getPath();
		if (configFileName.endsWith(".yaml")) {
			return new YamlConfigBuilder(configUrl).build();
		}
		return new XmlConfigBuilder(configUrl).build();
	}


	@Bean
	public Config hazelcastConfig(ApplicationContext applicationContext) throws IOException {
		URL configLocation = new ClassPathResource("my-hazelcast-config.xml").getURL();
		Config config = new XmlConfigBuilder(configLocation).build();
		config.setConfigurationUrl(configLocation);
		SpringManagedContext managedContext = new SpringManagedContext();
		managedContext.setApplicationContext(applicationContext);
		config.setManagedContext(managedContext);
		return config;
	}

	/**
	 * Return the {@link Config} managed by this instance.
	 * @return the hazelcast config
	 */
	protected Config getConfig() {
		return this.config;
	}

	/**
	 * Get the {@link HazelcastInstance}.
	 * @return the {@link HazelcastInstance}
	 */
	public HazelcastInstance getHazelcastInstance() {
		if (StringUtils.hasText(this.config.getInstanceName())) {
			return Hazelcast.getOrCreateHazelcastInstance(this.config);
		}
		return Hazelcast.newHazelcastInstance(this.config);
	}

}
