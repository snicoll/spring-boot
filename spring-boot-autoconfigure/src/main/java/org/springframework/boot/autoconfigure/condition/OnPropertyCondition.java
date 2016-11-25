/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.condition;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.boot.bind.PropertySourcesPropertyValues;
import org.springframework.boot.bind.RelaxedDataBinder;
import org.springframework.boot.bind.RelaxedNames;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

/**
 * {@link Condition} that checks if properties are defined in environment.
 *
 * @author Maciej Walkowiak
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 1.1.0
 * @see ConditionalOnProperty
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
class OnPropertyCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		List<AnnotationAttributes> allAnnotationAttributes = annotationAttributesFromMultiValueMap(
				metadata.getAllAnnotationAttributes(
						ConditionalOnProperty.class.getName()));
		List<ConditionMessage> noMatch = new ArrayList<ConditionMessage>();
		List<ConditionMessage> match = new ArrayList<ConditionMessage>();
		for (AnnotationAttributes annotationAttributes : allAnnotationAttributes) {
			ConditionOutcome outcome = determineOutcome(annotationAttributes,
					(ConfigurableEnvironment) context.getEnvironment());
			(outcome.isMatch() ? match : noMatch).add(outcome.getConditionMessage());
		}
		if (!noMatch.isEmpty()) {
			return ConditionOutcome.noMatch(ConditionMessage.of(noMatch));
		}
		return ConditionOutcome.match(ConditionMessage.of(match));
	}

	private List<AnnotationAttributes> annotationAttributesFromMultiValueMap(
			MultiValueMap<String, Object> multiValueMap) {
		List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
		for (Entry<String, List<Object>> entry : multiValueMap.entrySet()) {
			for (int i = 0; i < entry.getValue().size(); i++) {
				Map<String, Object> map;
				if (i < maps.size()) {
					map = maps.get(i);
				}
				else {
					map = new HashMap<String, Object>();
					maps.add(map);
				}
				map.put(entry.getKey(), entry.getValue().get(i));
			}
		}
		List<AnnotationAttributes> annotationAttributes = new ArrayList<AnnotationAttributes>(
				maps.size());
		for (Map<String, Object> map : maps) {
			annotationAttributes.add(AnnotationAttributes.fromMap(map));
		}
		return annotationAttributes;
	}

	private ConditionOutcome determineOutcome(AnnotationAttributes annotationAttributes,
			ConfigurableEnvironment environment) {
		Spec spec = new Spec(annotationAttributes);
		List<String> missingProperties = new ArrayList<String>();
		List<String> nonMatchingProperties = new ArrayList<String>();
		spec.collectProperties(environment, missingProperties, nonMatchingProperties);
		if (!missingProperties.isEmpty()) {
			return ConditionOutcome.noMatch(
					ConditionMessage.forCondition(ConditionalOnProperty.class, spec)
							.didNotFind("property", "properties")
							.items(Style.QUOTE, missingProperties));
		}
		if (!nonMatchingProperties.isEmpty()) {
			return ConditionOutcome.noMatch(
					ConditionMessage.forCondition(ConditionalOnProperty.class, spec)
							.found("different value in property",
									"different value in properties")
					.items(Style.QUOTE, nonMatchingProperties));
		}
		return ConditionOutcome.match(ConditionMessage
				.forCondition(ConditionalOnProperty.class, spec).because("matched"));
	}

	private static class Spec {

		private final String prefix;

		private final String havingValue;

		private final String[] names;

		private final boolean relaxedNames;

		private final boolean matchIfMissing;

		Spec(AnnotationAttributes annotationAttributes) {
			String prefix = annotationAttributes.getString("prefix").trim();
			if (StringUtils.hasText(prefix) && !prefix.endsWith(".")) {
				prefix = prefix + ".";
			}
			this.prefix = prefix;
			this.havingValue = annotationAttributes.getString("havingValue");
			this.names = getNames(annotationAttributes);
			this.relaxedNames = annotationAttributes.getBoolean("relaxedNames");
			this.matchIfMissing = annotationAttributes.getBoolean("matchIfMissing");
		}

		private String[] getNames(Map<String, Object> annotationAttributes) {
			String[] value = (String[]) annotationAttributes.get("value");
			String[] name = (String[]) annotationAttributes.get("name");
			Assert.state(value.length > 0 || name.length > 0,
					"The name or value attribute of @ConditionalOnProperty must be specified");
			Assert.state(value.length == 0 || name.length == 0,
					"The name and value attributes of @ConditionalOnProperty are exclusive");
			return (value.length > 0 ? value : name);
		}

		private void collectProperties(ConfigurableEnvironment environment, List<String> missing,
				List<String> nonMatching) {
			PropertyResolver resolver = new PropertyResolver(environment, this.relaxedNames, this.prefix);
			for (String name : this.names) {
				Entry<String, Object> entry = resolver.resolveProperty(name);
				if (entry != null) {
					if (!isMatch(ObjectUtils.nullSafeToString(entry.getValue()),
							this.havingValue)) {
						nonMatching.add(name);
					}
				}
				else {
					if (!this.matchIfMissing) {
						missing.add(name);
					}
				}
			}
		}

		private boolean isMatch(String value, String requiredValue) {
			if (StringUtils.hasLength(requiredValue)) {
				return requiredValue.equalsIgnoreCase(value);
			}
			return !"false".equalsIgnoreCase(value);
		}

		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append("(");
			result.append(this.prefix);
			if (this.names.length == 1) {
				result.append(this.names[0]);
			}
			else {
				result.append("[");
				result.append(StringUtils.arrayToCommaDelimitedString(this.names));
				result.append("]");
			}
			if (StringUtils.hasLength(this.havingValue)) {
				result.append("=").append(this.havingValue);
			}
			result.append(")");
			return result.toString();
		}


		private static class PropertyResolver {

			private final boolean relaxedNames;

			private final String prefix;

			private final Map<String, Object> content;

			PropertyResolver(ConfigurableEnvironment environment,
					boolean relaxedNames, String prefix) {
				this.relaxedNames = relaxedNames;
				this.prefix = prefix;
				this.content = new HashMap<String, Object>();
				DataBinder binder = new RelaxedDataBinder(this.content, this.prefix);
				binder.bind(new PropertySourcesPropertyValues(
						environment.getPropertySources()));
			}

			Map.Entry<String, Object> resolveProperty(String name) {
				if (this.relaxedNames) {
					return resolveRelaxedProperty(name);
				}
				else {
					String key = this.prefix + name;
					if (this.content.containsKey(name)) {
						return new AbstractMap.SimpleEntry<String, Object>(key,
								this.content.get(name));
					}
					return null;
				}
			}

			private Map.Entry<String, Object> resolveRelaxedProperty(String name) {
				RelaxedNames prefixes = new RelaxedNames(this.prefix);
				RelaxedNames keys = new RelaxedNames(name);
				for (String prefix : prefixes) {
					for (String relaxedKey : keys) {
						String key = prefix + relaxedKey;
						if (this.content.containsKey(relaxedKey)) {
							return new AbstractMap.SimpleEntry<String, Object>(key,
									this.content.get(relaxedKey));
						}
					}
				}
				return null;
			}

		}

	}

}
