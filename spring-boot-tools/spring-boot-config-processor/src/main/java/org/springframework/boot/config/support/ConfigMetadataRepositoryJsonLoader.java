/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.config.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.json.JSONException;

import org.springframework.boot.config.ConfigMetadataRepository;
import org.springframework.boot.config.SimpleConfigMetadataRepository;
import org.springframework.boot.config.processor.mapper.ConfigMetadataRepositoryJsonMapper;
import org.springframework.boot.config.processor.util.Assert;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Load a {@link ConfigMetadataRepository} either from the content of the
 * current classpath or based on a number or arbitrary resource(s).
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ConfigMetadataRepositoryJsonLoader {

	/**
	 * The default classpath location for config metadata.
	 */
	public static final String DEFAULT_LOCATION_PATTERN =
			"classpath*:META-INF/boot/config-metadata.json";

	/**
	 * The default classpath location for manual config metadata.
	 */
	public static final String DEFAULT_MANUAL_LOCATION_PATTERN =
			"classpath*:META-INF/boot/config-manual-metadata.json";

	private ConfigMetadataRepositoryJsonMapper mapper = new ConfigMetadataRepositoryJsonMapper();

	/**
	 * Load the {@link ConfigMetadataRepository} with the metadata of the current
	 * classpath using the {@link #DEFAULT_LOCATION_PATTERN}. If the same config
	 * metadata items is held within different resources, the first that is
	 * loaded is kept which means the result is not deterministic.
	 */
	public ConfigMetadataRepository loadFromClasspath() throws IOException {
		Resource[] manualResources = new PathMatchingResourcePatternResolver()
				.getResources(DEFAULT_MANUAL_LOCATION_PATTERN);
		Resource[] resources = new PathMatchingResourcePatternResolver()
				.getResources(DEFAULT_LOCATION_PATTERN);
		Collection<Resource> allResources = new ArrayList<Resource>();
		allResources.addAll(Arrays.asList(manualResources));
		allResources.addAll(Arrays.asList(resources));
		return load(allResources);
	}

	/**
	 * Load the {@link ConfigMetadataRepository} with the metadata defined by
	 * the specified {@code resources}. If the same config metadata items is
	 * held within different resources, the first that is loaded is kept.
	 */
	public ConfigMetadataRepository load(Collection<Resource> resources) throws IOException {
		Assert.notNull(resources, "Resources must not be null");
		if (resources.size() == 1) {
			return load(resources.iterator().next());
		}

		SimpleConfigMetadataRepository repository = new SimpleConfigMetadataRepository();
		for (Resource resource : resources) {
			ConfigMetadataRepository repo = load(resource);
			repository.include(repo);
		}
		return repository;
	}

	private ConfigMetadataRepository load(Resource resource) throws IOException {
		InputStream in = resource.getInputStream();
		try {
			return mapper.readRepository(in);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to read config metadata from '" + resource + "'", e);
		}
		catch (JSONException e) {
			throw new IllegalStateException("Invalid config metadata document defined at '" + resource + "'", e);
		}

		finally {
			in.close();
		}
	}

}
