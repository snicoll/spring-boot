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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;

/**
 * Configuration properties for the cache abstraction.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.cache")
public class CacheProperties {

	/**
	 * Cache mode (can be "ehcache", "generic", "hazelcast", "jcache", "redis", "simple" or
	 * "none"). Auto-detected according to the environment.
	 */
	private String mode = "simple";

	/**
	 * The location of the configuration file to use to initialize the cache
	 * library.
	 */
	private Resource location;

	/**
	 * Comma-separated list of cache names to create if supported by the
	 * underlying cache manager. Usually, this disables the ability to
	 * create caches on-the-fly.
	 */
	private final List<String> cacheNames = new ArrayList<String>();

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public Resource getLocation() {
		return location;
	}

	public void setLocation(Resource location) {
		this.location = location;
	}

	public List<String> getCacheNames() {
		return cacheNames;
	}

	/**
	 * Resolve the location attribute.
	 * @return the location or {@code null} if it is not set
	 * @throws IllegalArgumentException if the location is set to a unknown location
	 */
	public Resource resolveLocation() {
		if (this.location != null) {
			if (this.location.exists()) {
				return this.location;
			}
			else {
				throw new IllegalArgumentException("Cache configuration field defined by 'spring.config.location' does not exist " + this.location);
			}
		}
		return null;
	}

}
