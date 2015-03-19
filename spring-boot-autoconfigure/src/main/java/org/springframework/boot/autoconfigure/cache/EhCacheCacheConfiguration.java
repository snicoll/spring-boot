/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.cache;

import net.sf.ehcache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * EhCache configuration.
 *
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass({CacheManager.class, EhCacheCacheManager.class})
@ConditionalOnMissingBean(org.springframework.cache.CacheManager.class)
@Conditional(EhCacheCacheConfiguration.ConfigAvailableCondition.class)
@ConditionalOnProperty(prefix = "spring.cache", value = "mode", havingValue = "ehcache", matchIfMissing = true)
class EhCacheCacheConfiguration {

	@Autowired
	private CacheProperties properties;

	@Bean
	public EhCacheCacheManager cacheManager() {
		Resource location = this.properties.resolveLocation();
		if (location != null) {
			return new EhCacheCacheManager(
				EhCacheManagerUtils.buildCacheManager(location));
		}
		return new EhCacheCacheManager(
			EhCacheManagerUtils.buildCacheManager());
	}

	/**
	 * Determines if the EhCache configuration is available. This either kick in if a default
	 * configuration has been found or if property referring to the file to use has been set.
	 */
	static class ConfigAvailableCondition extends AnyNestedCondition {

		public ConfigAvailableCondition() {
			super(ConfigurationCondition.ConfigurationPhase.PARSE_CONFIGURATION);
		}

		@ConditionalOnProperty(prefix = "spring.cache", name = "location")
		static class CacheLocationProperty {
		}

		@Conditional(BootstrapConfigurationAvailableCondition.class)
		static class DefaultConfigurationAvailable {
		}

	}

	static class BootstrapConfigurationAvailableCondition extends SpringBootCondition {

		private final ResourceLoader resourceLoader = new DefaultResourceLoader();

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			if (resourceLoader.getResource("file:./ehcache.xml").exists()) {
				return ConditionOutcome.match("ehcache.xml found in the working directory.");
			}
			if (resourceLoader.getResource("classpath:/ehcache.xml").exists()) {
				return ConditionOutcome.match("ehcache.xml found in the classpath.");
			}
			return ConditionOutcome.noMatch("No ehcache.xml file found.");
		}
	}

}
