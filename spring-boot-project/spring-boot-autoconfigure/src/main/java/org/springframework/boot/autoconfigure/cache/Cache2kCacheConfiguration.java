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

package org.springframework.boot.autoconfigure.cache;

import java.util.Collection;

import org.cache2k.Cache2kBuilder;
import org.cache2k.extra.spring.SpringCache2kCacheManager;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;

/**
 * Support for cache2k auto configuration.
 *
 * @author Jens Wilke
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Cache2kBuilder.class, SpringCache2kCacheManager.class })
@ConditionalOnMissingBean(CacheManager.class)
@Conditional({ CacheCondition.class })
class Cache2kCacheConfiguration {

	@Bean
	SpringCache2kCacheManager cacheManager(CacheProperties cacheProperties, CacheManagerCustomizers customizers) {
		String managerName = cacheProperties.getCache2k().getManagerName();
		SpringCache2kCacheManager cacheManager = (managerName != null) ? new SpringCache2kCacheManager(managerName)
				: new SpringCache2kCacheManager();
		customizers.customize(cacheManager);
		addListedButUnknownCaches(cacheManager, cacheProperties.getCacheNames());
		return cacheManager;
	}

	/**
	 * Add caches yet unknown that are configured via the {@code CacheProperties}. Don't
	 * add those before customization to be able the add the cache with programmatic
	 * configuration via customization. These caches are created with default parameters
	 * provided either via the customizer or the XML configuration
	 * @param cacheManager the cache manger to add the caches
	 * @param cacheNames the cache names
	 */
	private void addListedButUnknownCaches(SpringCache2kCacheManager cacheManager, Collection<String> cacheNames) {
		Collection<String> knownCaches = cacheManager.getCacheNames();
		if (!CollectionUtils.isEmpty(cacheNames)) {
			cacheNames.stream().filter((n) -> !knownCaches.contains(n))
					.forEach((name) -> cacheManager.addCaches((builder) -> builder.name(name)));
		}
	}

}
