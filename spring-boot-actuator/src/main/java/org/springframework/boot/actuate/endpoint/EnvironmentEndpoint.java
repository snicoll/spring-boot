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

package org.springframework.boot.actuate.endpoint;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.ReadOperation;
import org.springframework.boot.endpoint.Selector;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * {@link Endpoint} to expose {@link ConfigurableEnvironment environment} information.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Madhura Bhave
 */
@Endpoint(id = "env")
public class EnvironmentEndpoint {

	private final Sanitizer sanitizer = new Sanitizer();

	private final Environment environment;

	public EnvironmentEndpoint(Environment environment) {
		this.environment = environment;
	}

	public void setKeysToSanitize(String... keysToSanitize) {
		this.sanitizer.setKeysToSanitize(keysToSanitize);
	}

	@ReadOperation
	public Map<String, Object> getEnvironment() {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("profiles", this.environment.getActiveProfiles());
		PropertyResolver resolver = getResolver();
		for (Entry<String, PropertySource<?>> entry : getPropertySourcesAsMap()
				.entrySet()) {
			PropertySource<?> source = entry.getValue();
			String sourceName = entry.getKey();
			if (source instanceof EnumerablePropertySource) {
				EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;
				Map<String, Object> properties = new LinkedHashMap<>();
				for (String name : enumerable.getPropertyNames()) {
					Object resolved = resolver.getProperty(name, Object.class);
					properties.put(name, sanitize(name, resolved));
				}
				properties = postProcessSourceProperties(sourceName, properties);
				if (properties != null) {
					result.put(sourceName, properties);
				}
			}
		}
		return result;
	}

	public Object getEnvironmentEntry(@Selector String name) {
		return new NamePatternEnvironmentFilter(this.environment).getResults(name);
	}

	public PropertyResolver getResolver() {
		PlaceholderSanitizingPropertyResolver resolver = new PlaceholderSanitizingPropertyResolver(
				getPropertySources(), this.sanitizer);
		resolver.setIgnoreUnresolvableNestedPlaceholders(true);
		return resolver;
	}

	private Map<String, PropertySource<?>> getPropertySourcesAsMap() {
		Map<String, PropertySource<?>> map = new LinkedHashMap<String, PropertySource<?>>();
		for (PropertySource<?> source : getPropertySources()) {
			extract("", map, source);
		}
		return map;
	}

	private MutablePropertySources getPropertySources() {
		MutablePropertySources sources;
		if (this.environment instanceof ConfigurableEnvironment) {
			sources = ((ConfigurableEnvironment) this.environment).getPropertySources();
		}
		else {
			sources = new StandardEnvironment().getPropertySources();
		}
		return sources;
	}

	private void extract(String root, Map<String, PropertySource<?>> map,
			PropertySource<?> source) {
		if (source instanceof CompositePropertySource) {
			for (PropertySource<?> nest : ((CompositePropertySource) source)
					.getPropertySources()) {
				extract(source.getName() + ":", map, nest);
			}
		}
		else {
			map.put(root + source.getName(), source);
		}
	}

	public Object sanitize(String name, Object object) {
		return this.sanitizer.sanitize(name, object);
	}

	/**
	 * Apply any post processing to source data before it is added.
	 * @param sourceName the source name
	 * @param properties the properties
	 * @return the post-processed properties or {@code null} if the source should not be
	 * added
	 * @since 1.4.0
	 */
	protected Map<String, Object> postProcessSourceProperties(String sourceName,
			Map<String, Object> properties) {
		return properties;
	}

	/**
	 * {@link PropertySourcesPropertyResolver} that sanitizes sensitive placeholders if
	 * present.
	 */
	private class PlaceholderSanitizingPropertyResolver
			extends PropertySourcesPropertyResolver {

		private final Sanitizer sanitizer;

		/**
		 * Create a new resolver against the given property sources.
		 * @param propertySources the set of {@link PropertySource} objects to use
		 * @param sanitizer the sanitizer used to sanitize sensitive values
		 */
		PlaceholderSanitizingPropertyResolver(PropertySources propertySources,
				Sanitizer sanitizer) {
			super(propertySources);
			this.sanitizer = sanitizer;
		}

		@Override
		protected String getPropertyAsRawString(String key) {
			String value = super.getPropertyAsRawString(key);
			return (String) this.sanitizer.sanitize(key, value);
		}

	}

	/**
	 * {@link NamePatternFilter} for the Environment source.
	 */
	private class NamePatternEnvironmentFilter extends NamePatternFilter<Environment> {

		NamePatternEnvironmentFilter(Environment source) {
			super(source);
		}

		@Override
		protected void getNames(Environment source, NameCallback callback) {
			if (source instanceof ConfigurableEnvironment) {
				getNames(((ConfigurableEnvironment) source).getPropertySources(),
						callback);
			}
		}

		private void getNames(PropertySources propertySources, NameCallback callback) {
			for (PropertySource<?> propertySource : propertySources) {
				if (propertySource instanceof EnumerablePropertySource) {
					EnumerablePropertySource<?> source = (EnumerablePropertySource<?>) propertySource;
					for (String name : source.getPropertyNames()) {
						callback.addName(name);
					}
				}
			}
		}

		@Override
		protected Object getOptionalValue(Environment source, String name) {
			Object result = EnvironmentEndpoint.this.environment.getProperty(name,
					Object.class);
			if (result != null) {
				result = sanitize(name, result);
			}
			return result;
		}

		@Override
		protected Object getValue(Environment source, String name) {
			Object result = source.getProperty(name, Object.class);
			if (result == null) {
				throw new NoSuchPropertyException("No such property: " + name);
			}
			return sanitize(name, result);
		}

	}

	/**
	 * Exception thrown when the specified property cannot be found.
	 */
	@SuppressWarnings("serial")
	@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "No such property")
	public static class NoSuchPropertyException extends RuntimeException {

		public NoSuchPropertyException(String string) {
			super(string);
		}

	}

}
