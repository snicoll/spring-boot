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

package org.springframework.boot.config.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;

import org.springframework.boot.config.ConfigMetadataGroup;
import org.springframework.boot.config.ConfigMetadataItem;
import org.springframework.boot.config.SimpleConfigMetadataRepository;
import org.springframework.boot.config.processor.util.ModelUtils;

/**
 * Harvest the condition defined on an {@link Element} and register
 * the corresponding configuration item(s) if necessary.
 *
 * <p>Conditions can be defined in a different notation and could
 * conflict with existing keys. If the conditions are defined using
 * the hyphen notation (that is lower case with dash to separate
 * words), it is transformed to the camel case counter part automatically.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class ConditionHarvester {

	/**
	 * The prefix value
	 */
	static final String PREFIX_ATTRIBUTE = "prefix";

	/**
	 * The actual value. Can be a single value or a list of values.
	 */
	static final String VALUE_ATTRIBUTE = "value";

	private final SimpleConfigMetadataRepository repository;

	public ConditionHarvester(SimpleConfigMetadataRepository repository) {
		this.repository = repository;
	}

	/**
	 * Harvest the condition on the specified {@link Element} and and register
	 * the corresponding configuration item(s) if necessary.
	 */
	public void harvest(Element element) {
		AnnotationMirror annotation = ModelUtils.getConditionalOnPropertyAnnotation(element);
		if (annotation != null) {
			Map<String, Object> parameters = ModelUtils.parseAnnotationProperties(annotation);
			harvest(parameters);
		}
	}

	/**
	 * Harvest using the specified annotation properties.
	 * @see #PREFIX_ATTRIBUTE
	 * @see #VALUE_ATTRIBUTE
	 */
	void harvest(Map<String, Object> properties) {
		String prefix = getPrefix(properties);
		List<String> values = getValues(properties);
		for (String value : values) {
			if (prefix != null) {
				register(prefix, value);
			}
			else {
				String prefixFromProperty = extractPrefix(value);
				if (prefixFromProperty != null) {
					String actualProperty = value.substring(prefixFromProperty.length() + 1, value.length());
					register(prefixFromProperty, actualProperty);
				}
				else {
					register(null, value);
				}
			}
		}
	}

	private String extractPrefix(String property) {
		int i = property.lastIndexOf('.');
		if (i == -1) {
			return null;
		}
		return property.substring(0, i);
	}

	private void register(String prefix, String property) {
		if (prefix == null || !repository.getAllGroups().containsKey(prefix)) {
			ConfigMetadataGroup root = ConfigMetadataGroup.root(prefix);
			root.registerItem(new ConfigMetadataItem(property));
			repository.registerRootGroup(root);
		}
		else {
			ConfigMetadataGroup group = repository.getAllGroups().get(prefix);
			group.registerItem(new ConfigMetadataItem(property));
		}
	}

	@SuppressWarnings("unchecked")
	private static List<String> getValues(Map<String, Object> properties) {
		List<String> result = new ArrayList<String>();
		Object o = properties.get(VALUE_ATTRIBUTE);
		if (o instanceof String) {
			String item = (String) o;
			if (ModelUtils.hasText(item)) {
				result.add(hyphenToCamelCase(item));
			}
		}
		else if (o instanceof List) {
			for (Object rawItem : (List<Object>) o) {
				if (rawItem instanceof String) {
					String item = (String) rawItem;
					if (ModelUtils.hasText(item)) {
						result.add(hyphenToCamelCase(item));
					}
				}
			}
		}
		return result;
	}

	private static String getPrefix(Map<String, Object> properties) {
		String prefix = (String) properties.get(PREFIX_ATTRIBUTE);
		if (prefix == null || prefix.trim().isEmpty()) {
			return null;
		}
		if (prefix.endsWith(".")) {
			return prefix.substring(0, prefix.length() - 1);
		}
		return hyphenToCamelCase(prefix);
	}

	private static String hyphenToCamelCase(String value) {
		StringBuilder builder = new StringBuilder();
		for (String field : value.split("[\\-]")) {
			builder.append(builder.length() == 0 ? field : ModelUtils
					.capitalize(field));
		}

		if (value.endsWith("-")) {
			builder.append("-");
		}
		return builder.toString();
	}

}
