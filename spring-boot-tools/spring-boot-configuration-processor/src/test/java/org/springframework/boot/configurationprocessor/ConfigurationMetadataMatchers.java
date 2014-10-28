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

package org.springframework.boot.configurationprocessor;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.PropertyMetadata;

/**
 * Hamcrest {@link Matcher} to help test {@link ConfigurationMetadata}.
 *
 * @author Phillip Webb
 */
public class ConfigurationMetadataMatchers {

	public static ContainsPropertyMatcher containsProperty(String name) {
		return new ContainsPropertyMatcher(name);
	}

	public static ContainsPropertyMatcher containsProperty(String name, Class<?> type) {
		return new ContainsPropertyMatcher(name).ofDataType(type);
	}

	public static ContainsPropertyMatcher containsProperty(String name, String type) {
		return new ContainsPropertyMatcher(name).ofDataType(type);
	}

	public static class ContainsPropertyMatcher extends
			BaseMatcher<ConfigurationMetadata> {

		private final String name;

		private final String dataType;

		private final Class<?> sourceType;

		private final String description;

		public ContainsPropertyMatcher(String name) {
			this(name, null, null, null);
		}

		public ContainsPropertyMatcher(String name, String dataType, Class<?> sourceType,
				String description) {
			this.name = name;
			this.dataType = dataType;
			this.sourceType = sourceType;
			this.description = description;
		}

		@Override
		public boolean matches(Object item) {
			ConfigurationMetadata metadata = (ConfigurationMetadata) item;
			PropertyMetadata property = getFirstPropertyWithName(metadata, this.name);
			if (property == null) {
				return false;
			}
			if (this.dataType != null && !this.dataType.equals(property.getDataType())) {
				return false;
			}
			if (this.sourceType != null
					&& !this.sourceType.getName().equals(property.getSourceType())) {
				return false;
			}
			if (this.description != null
					&& !this.description.equals(property.getDescription())) {
				return false;
			}
			return true;
		}

		@Override
		public void describeMismatch(Object item, Description description) {
			ConfigurationMetadata metadata = (ConfigurationMetadata) item;
			PropertyMetadata property = getFirstPropertyWithName(metadata, this.name);
			if (property == null) {
				description.appendText("missing property " + this.name);
			}
			else {
				description.appendText("was property ").appendValue(property);
			}
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("metadata containing " + this.name);
			if (this.dataType != null) {
				description.appendText(" dataType ").appendValue(this.dataType);
			}
			if (this.sourceType != null) {
				description.appendText(" sourceType ").appendValue(this.sourceType);
			}
			if (this.description != null) {
				description.appendText(" description ").appendValue(this.description);
			}
		}

		public ContainsPropertyMatcher ofDataType(Class<?> dataType) {
			return new ContainsPropertyMatcher(this.name, dataType.getName(),
					this.sourceType, this.description);
		}

		public ContainsPropertyMatcher ofDataType(String dataType) {
			return new ContainsPropertyMatcher(this.name, dataType, this.sourceType,
					this.description);
		}

		public ContainsPropertyMatcher fromSource(Class<?> sourceType) {
			return new ContainsPropertyMatcher(this.name, this.dataType, sourceType,
					this.description);
		}

		public ContainsPropertyMatcher withDescription(String description) {
			return new ContainsPropertyMatcher(this.name, this.dataType, this.sourceType,
					description);
		}

		private PropertyMetadata getFirstPropertyWithName(ConfigurationMetadata metadata,
				String name) {
			for (PropertyMetadata property : metadata.getProperties()) {
				if (name.equals(property.getName())) {
					return property;
				}
			}
			return null;
		}

	}

}
