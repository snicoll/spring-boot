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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JCache (JSR-107) specific cache properties.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.cache.jcache")
public class JCacheCacheProperties {

	/**
	 * Fully qualified name of the CachingProvider implementation to use to
	 * retrieve the JSR-107 compliant cache manager. Only needed if more than
	 * one JSR-107 implementation is available on the classpath.
	 */
	private String provider;

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

}
