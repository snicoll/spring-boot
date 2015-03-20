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

package org.springframework.boot.actuate.cache;

import java.io.IOException;
import java.util.Arrays;

import com.google.common.cache.CacheBuilder;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import org.junit.After;
import org.junit.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.cache.ehcache.EhCacheManagerUtils;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Stephane Nicoll
 */
public class CacheStatisticsProviderTests {

	private AnnotationConfigApplicationContext context;

	private CacheManager cacheManager;

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void basicEhCacheCacheStatistics() {
		load(EhCacheConfig.class);
		CacheStatisticsProvider provider = this.context
				.getBean("ehCacheCacheStatisticsProvider", CacheStatisticsProvider.class);
		doTestCoreStatistics(provider);
	}

	@Test // TODO cache miss
	public void basicHazelcastCacheStatistics() {
		load(HazelcastConfig.class);
		CacheStatisticsProvider provider = this.context
				.getBean("hazelcastCacheStatisticsProvider", CacheStatisticsProvider.class);
		Cache books = getCache("books");
		CacheStatistics cacheStatistics = provider.getCacheStatistics(books, this.cacheManager);
		assertCoreStatistics(cacheStatistics, 0L, 0L, null);
		getOrCreate(books, "a", "b", "b", "a", "a");
		CacheStatistics updatedCacheStatistics = provider.getCacheStatistics(books, this.cacheManager);
		assertCoreStatistics(updatedCacheStatistics, 2L, 3L, null);
	}

	@Test
	public void basicGuavaCacheStatistics() {
		load(GuavaConfig.class);
		CacheStatisticsProvider provider = this.context
				.getBean("guavaCacheStatisticsProvider", CacheStatisticsProvider.class);
		doTestCoreStatistics(provider);
	}

	@Test
	public void concurrentMapCacheStatistics() {
		load(ConcurrentMapConfig.class);
		CacheStatisticsProvider provider = this.context
				.getBean("concurrentMapCacheStatisticsProvider", CacheStatisticsProvider.class);
		Cache books = getCache("books");
		CacheStatistics cacheStatistics = provider.getCacheStatistics(books, this.cacheManager);
		assertCoreStatistics(cacheStatistics, 0L, null, null);
		getOrCreate(books, "a", "b", "b", "a", "a");
		CacheStatistics updatedCacheStatistics = provider.getCacheStatistics(books, this.cacheManager);
		assertCoreStatistics(updatedCacheStatistics, 2L, null, null);
	}

	@Test
	public void noOpCacheStatistics() {
		load(NoOpCacheConfig.class);
		CacheStatisticsProvider provider = this.context
				.getBean("noOpCacheStatisticsProvider", CacheStatisticsProvider.class);
		Cache books = getCache("books");
		CacheStatistics cacheStatistics = provider.getCacheStatistics(books, this.cacheManager);
		assertCoreStatistics(cacheStatistics, null, null, null);
		getOrCreate(books, "a", "b", "b", "a", "a");
		CacheStatistics updatedCacheStatistics = provider.getCacheStatistics(books, this.cacheManager);
		assertCoreStatistics(updatedCacheStatistics, null, null, null);
	}

	private void doTestCoreStatistics(CacheStatisticsProvider provider) {
		Cache books = getCache("books");
		CacheStatistics cacheStatistics = provider.getCacheStatistics(books, this.cacheManager);
		assertCoreStatistics(cacheStatistics, 0L, 0L, 0L);
		getOrCreate(books, "a", "b", "b", "a", "a");
		CacheStatistics updatedCacheStatistics = provider.getCacheStatistics(books, this.cacheManager);
		assertCoreStatistics(updatedCacheStatistics, 2L, 3L, 2L);
	}

	private void assertCoreStatistics(CacheStatistics metrics, Long size, Long hitCount, Long missCount) {
		assertNotNull("Cache metrics must not be null", metrics);
		assertEquals("Wrong size for metrics " + metrics, size, metrics.getSize());
		assertEquals("Wrong hit count for metrics " + metrics, hitCount, metrics.getHitCount());
		assertEquals("Wrong miss count for metrics " + metrics, missCount, metrics.getMissCount());
	}

	private void getOrCreate(Cache cache, String... ids) {
		for (String id : ids) {
			Cache.ValueWrapper wrapper = cache.get(id);
			if (wrapper == null) {
				cache.put(id, id);
			}
		}
	}

	private Cache getCache(String cacheName) {
		Cache cache = this.cacheManager.getCache(cacheName);
		Assert.notNull("No cache with name '" + cacheName + "' found.");
		return cache;
	}

	private void load(Class<?>... config) {
		this.context = new AnnotationConfigApplicationContext();
		if (config.length > 0) {
			this.context.register(config);
		}
		this.context.register(CacheStatisticsProvidersConfiguration.class);
		this.context.refresh();
		this.cacheManager = this.context.getBean(CacheManager.class);
	}


	@Configuration
	static class EhCacheConfig {

		@Bean
		public EhCacheCacheManager cacheManager() {
			return new EhCacheCacheManager(EhCacheManagerUtils
					.buildCacheManager(new ClassPathResource("cache/test-ehcache.xml")));
		}
	}

	@Configuration
	static class HazelcastConfig {

		@Bean
		public HazelcastCacheManager cacheManager() throws IOException {
			Resource resource = new ClassPathResource("cache/test-hazelcast.xml");
			Config cfg = new XmlConfigBuilder(resource.getURL()).build();
			return new HazelcastCacheManager(Hazelcast.newHazelcastInstance(cfg));
		}
	}

	@Configuration
	static class GuavaConfig {

		@Bean
		public GuavaCacheManager cacheManager() throws IOException {
			GuavaCacheManager cacheManager = new GuavaCacheManager();
			cacheManager.setCacheBuilder(CacheBuilder.newBuilder().recordStats());
			cacheManager.setCacheNames(Arrays.asList("books", "speakers"));
			return cacheManager;
		}
	}

	@Configuration
	static class ConcurrentMapConfig {

		@Bean
		public ConcurrentMapCacheManager cacheManager() {
			return new ConcurrentMapCacheManager("books", "speakers");
		}
	}

	@Configuration
	static class NoOpCacheConfig {

		@Bean
		public NoOpCacheManager cacheManager() {
			return new NoOpCacheManager();
		}
	}

}
