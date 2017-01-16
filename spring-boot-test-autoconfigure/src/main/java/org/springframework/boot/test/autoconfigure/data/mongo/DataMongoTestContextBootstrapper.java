/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.mongo;

import java.util.List;
import java.util.Map;

import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.support.TestPropertySourceUtils;

/**
 * {@link TestContextBootstrapper} for {@link DataMongoTest @DataMongoTest} support.
 *
 * @author Stephane Nicoll
 */
class DataMongoTestContextBootstrapper extends SpringBootTestContextBootstrapper {

	@Override
	protected void processPropertySourceProperties(
			MergedContextConfiguration mergedConfig,
			List<String> propertySourceProperties) {
		if (!hasCustomPort(propertySourceProperties)) {
			propertySourceProperties.add("spring.data.mongodb.port=0");
		}
		super.processPropertySourceProperties(mergedConfig, propertySourceProperties);
	}

	private boolean hasCustomPort(List<String> properties) {
		Map<String, Object> props = TestPropertySourceUtils.convertInlinedPropertiesToMap(
				properties.toArray(new String[properties.size()]));
		MutablePropertySources sources = new MutablePropertySources();
		sources.addFirst(new MapPropertySource("inline", props));
		RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(
				new PropertySourcesPropertyResolver(sources), "spring.data.mongodb.");
		return resolver.containsProperty("port");
	}

}
