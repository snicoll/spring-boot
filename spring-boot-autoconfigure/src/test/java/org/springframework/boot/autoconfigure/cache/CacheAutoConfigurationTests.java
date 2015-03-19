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

import javax.cache.configuration.CompleteConfiguration;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.cache.support.MockCachingProvider;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.jcache.JCacheCacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CacheAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class CacheAutoConfigurationTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void noEnableCaching() {
		load(EmptyConfiguration.class);

		thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(CacheManager.class);
	}

	@Test
	public void cacheManagerBackOff() {
		load(CustomCacheManagerConfiguration.class);
		ConcurrentMapCacheManager cacheManager = validateCacheManager(ConcurrentMapCacheManager.class);
		assertThat(cacheManager.getCacheNames(), contains("custom1"));
		assertThat(cacheManager.getCacheNames(), hasSize(1));
	}

	@Test
	public void simpleCacheExplicit() {
		load(DefaultCacheConfiguration.class, "spring.cache.mode=simple");
		ConcurrentMapCacheManager cacheManager = validateCacheManager(ConcurrentMapCacheManager.class);
		assertThat(cacheManager.getCacheNames(), is(empty()));
	}

	@Test
	public void simpleCacheExplicitWithCacheNames() {
		load(DefaultCacheConfiguration.class,
				"spring.cache.mode=simple",
				"spring.cache.cacheNames[0]=foo",
				"spring.cache.cacheNames[1]=bar");
		ConcurrentMapCacheManager cacheManager = validateCacheManager(ConcurrentMapCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "bar"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void genericCacheWithCaches() {
		load(GenericCacheConfiguration.class);

		SimpleCacheManager cacheManager = validateCacheManager(SimpleCacheManager.class);
		assertThat(cacheManager.getCache("first"), equalTo(this.context.getBean("firstCache")));
		assertThat(cacheManager.getCache("second"), equalTo(this.context.getBean("secondCache")));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void genericCacheExplicit() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("CacheManager found");  // no cache manager found
		load(DefaultCacheConfiguration.class,
				"spring.cache.mode=generic");
	}

	@Test
	public void genericCacheExplicitWithCaches() {
		load(GenericCacheConfiguration.class,
				"spring.cache.mode=generic");

		SimpleCacheManager cacheManager = validateCacheManager(SimpleCacheManager.class);
		assertThat(cacheManager.getCache("first"), equalTo(this.context.getBean("firstCache")));
		assertThat(cacheManager.getCache("second"), equalTo(this.context.getBean("secondCache")));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void redisCache() {
		load(RedisCacheConfiguration.class);
		RedisCacheManager cacheManager = validateCacheManager(RedisCacheManager.class);
		assertThat(cacheManager.getCacheNames(), is(empty()));
	}

	@Test
	public void redisCacheWithCaches() {
		load(RedisCacheConfiguration.class,
				"spring.cache.cacheNames[0]=foo",
				"spring.cache.cacheNames[1]=bar");
		RedisCacheManager cacheManager = validateCacheManager(RedisCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "bar"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void noOpCacheExplicit() {
		load(DefaultCacheConfiguration.class, "spring.cache.mode=none");
		NoOpCacheManager cacheManager = validateCacheManager(NoOpCacheManager.class);
		assertThat(cacheManager.getCacheNames(), is(empty()));
	}

	@Test // This test won't last for long once we add another JSR-107 providers on the classpath
	public void jCacheCacheNoProviderExplicit() {
		load(DefaultCacheConfiguration.class, "spring.cache.mode=jcache");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames(), is(empty()));
	}

	@Test
	public void jCacheCacheWithProvider() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		load(DefaultCacheConfiguration.class,
				"spring.cache.mode=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn);
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames(), is(empty()));
	}

	@Test
	public void jCacheCacheWithCaches() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		load(DefaultCacheConfiguration.class,
				"spring.cache.mode=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.cacheNames[0]=foo",
				"spring.cache.cacheNames[1]=bar");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("foo", "bar"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));
	}

	@Test
	public void jCacheCacheWithCachesAndCustomConfig() {
		String cachingProviderFqn = MockCachingProvider.class.getName();
		load(CustomJCacheConfiguration.class,
				"spring.cache.mode=jcache",
				"spring.cache.jcache.provider=" + cachingProviderFqn,
				"spring.cache.cacheNames[0]=one",
				"spring.cache.cacheNames[1]=two");
		JCacheCacheManager cacheManager = validateCacheManager(JCacheCacheManager.class);
		assertThat(cacheManager.getCacheNames(), containsInAnyOrder("one", "two"));
		assertThat(cacheManager.getCacheNames(), hasSize(2));

		CompleteConfiguration<?,?> defaultCacheConfiguration = this.context.getBean(CompleteConfiguration.class);
		verify(cacheManager.getCacheManager()).createCache("one", defaultCacheConfiguration);
		verify(cacheManager.getCacheManager()).createCache("two", defaultCacheConfiguration);
	}

	@Test
	public void jCacheCacheWithUnknownProvider() {
		String wrongCachingProviderFqn = "org.acme.FooBar";

		thrown.expect(BeanCreationException.class);
		thrown.expectMessage(wrongCachingProviderFqn);
		load(DefaultCacheConfiguration.class,
				"spring.cache.mode=jcache",
				"spring.cache.jcache.provider=" + wrongCachingProviderFqn);
	}

	private <T extends CacheManager> T validateCacheManager(Class<T> type) {
		CacheManager cacheManager = this.context.getBean(CacheManager.class);
		assertThat("Wrong cache manager type", cacheManager, is(instanceOf(type)));
		return type.cast(cacheManager);
	}

	private void load(Class<?> config, String... environment) {
		this.context = doLoad(config, environment);
	}

	private AnnotationConfigApplicationContext doLoad(Class<?> config,
			String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.register(config);
		applicationContext.register(CacheAutoConfiguration.class);
		applicationContext.refresh();
		return applicationContext;
	}


	@Configuration
	static class EmptyConfiguration {

	}

	@Configuration
	@EnableCaching
	static class DefaultCacheConfiguration {

	}

	@Configuration
	@EnableCaching
	static class GenericCacheConfiguration {

		@Bean
		public Cache firstCache() {
			return new ConcurrentMapCache("first");
		}

		@Bean
		public Cache secondCache() {
			return new ConcurrentMapCache("second");
		}

	}

	@Configuration
	@EnableCaching
	static class RedisCacheConfiguration {

		@Bean
		public RedisTemplate<?, ?> redisTemplate() {
			return mock(RedisTemplate.class);
		}

	}

	@Configuration
	@EnableCaching
	static class CustomJCacheConfiguration {

		@Bean
		public CompleteConfiguration<?,?> defaultCacheConfiguration() {
			return mock(CompleteConfiguration.class);
		}

	}

	@Configuration
	@Import({GenericCacheConfiguration.class, RedisCacheConfiguration.class})
	static class CustomCacheManagerConfiguration {

		@Bean
		public CacheManager cacheManager() {
			return new ConcurrentMapCacheManager("custom1");
		}
	}

}
