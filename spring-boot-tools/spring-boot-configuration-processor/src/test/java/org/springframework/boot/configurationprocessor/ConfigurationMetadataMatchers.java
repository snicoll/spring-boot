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

package org.springframework.boot.configurationprocessor;

import java.util.List;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import org.springframework.boot.configurationprocessor.metadata.ConfigurationMetadata;
import org.springframework.boot.configurationprocessor.metadata.ItemMetadata;
import org.springframework.boot.configurationprocessor.metadata.TypeDescriptor;

/**
 * Hamcrest {@link Matcher} to help test {@link ConfigurationMetadata}.
 *
 * @author Phillip Webb
 */
public class ConfigurationMetadataMatchers {

	public static ContainsGroupMatcher containsGroup(String name) {
		return new ContainsGroupMatcher(name);
	}

	public static ContainsGroupMatcher containsGroup(String name, Class<?> type) {
		return new ContainsGroupMatcher(name).ofType(type);
	}

	public static ContainsGroupMatcher containsGroup(String name, String type) {
		return new ContainsGroupMatcher(name).ofType(type);
	}

	public static ContainsPropertyMatcher containsProperty(String name) {
		return new ContainsPropertyMatcher(name);
	}

	public static ContainsPropertyMatcher containsProperty(String name, Class<?> type) {
		return new ContainsPropertyMatcher(name).ofType(type);
	}

	public static ContainsPropertyMatcher containsProperty(String name, String type) {
		return new ContainsPropertyMatcher(name).ofType(type);
	}


	public static abstract class AbstractItemMatcher<B> extends BaseMatcher<ConfigurationMetadata> {

		protected final String name;

		private final String type;

		private final Class<?> sourceType;

		private final String description;

		private final Matcher<?> defaultValue;

		private final boolean deprecated;

		protected AbstractItemMatcher(String name) {
			this(name, null, null, null, null, false);
		}

		protected AbstractItemMatcher(String name, String type,
				Class<?> sourceType, String description, Matcher<?> defaultValue,
				boolean deprecated) {
			this.name = name;
			this.type = type;
			this.sourceType = sourceType;
			this.description = description;
			this.defaultValue = defaultValue;
			this.deprecated = deprecated;
		}

		@Override
		public boolean matches(Object item) {
			ItemMetadata itemMetadata = getItemMetadata(item);
			if (itemMetadata == null) {
				return false;
			}
			if (this.type != null && !this.type.equals(itemMetadata.getType())) {
				return false;
			}
			if (this.sourceType != null
					&& !this.sourceType.getName().equals(itemMetadata.getSourceType())) {
				return false;
			}
			if (this.defaultValue != null
					&& !this.defaultValue.matches(itemMetadata.getDefaultValue())) {
				return false;
			}
			if (this.description != null
					&& !this.description.equals(itemMetadata.getDescription())) {
				return false;
			}
			if (this.deprecated != itemMetadata.isDeprecated()) {
				return false;
			}
			return true;
		}

		protected abstract ItemMetadata getItemMetadata(Object item);

		@Override
		public void describeMismatch(Object item, Description description) {
			ItemMetadata property = getItemMetadata(item);
			if (property == null) {
				description.appendText("missing " + this.name);
			}
			else {
				description.appendText(
						"was  ").appendValue(property);
			}
		}

		@Override
		public void describeTo(Description description) {
			description.appendText("metadata containing " + this.name);
			if (this.type != null) {
				description.appendText(" type ").appendValue(this.type);
			}
			if (this.sourceType != null) {
				description.appendText(" sourceType ").appendValue(this.sourceType);
			}
			if (this.defaultValue != null) {
				description.appendText(" defaultValue ").appendValue(this.defaultValue);
			}
			if (this.description != null) {
				description.appendText(" description ").appendValue(this.description);
			}
			if (this.deprecated) {
				description.appendText(" deprecated ").appendValue(true);
			}
		}

		protected abstract B create(String name, String type,
				Class<?> sourceType, String description, Matcher<?> defaultValue,
				boolean deprecated);

		public B ofType(Class<?> dataType) {
			return create(this.name, dataType.getName(),
					this.sourceType, this.description, this.defaultValue, this.deprecated);
		}

		public B ofType(String dataType) {
			return create(this.name, dataType,
					this.sourceType, this.description, this.defaultValue, this.deprecated);
		}

		public B fromSource(Class<?> sourceType) {
			return create(this.name, this.type,
					sourceType, this.description, this.defaultValue, this.deprecated);
		}

		public B withDescription(String description) {
			return create(this.name, this.type,
					this.sourceType, description, this.defaultValue, this.deprecated);
		}

		public B withDefaultValue(Matcher<?> defaultValue) {
			return create(this.name, this.type,
					this.sourceType, this.description, defaultValue, this.deprecated);
		}

		public B withDeprecated() {
			return create(this.name, this.type,
					this.sourceType, this.description, this.defaultValue, true);
		}


		protected <T extends ItemMetadata> T getFirstItemWithName(List<T> items, String name) {
			for (T item : items) {
				if (name.equals(item.getName())) {
					return item;
				}
			}
			return null;
		}

	}

	public static class ContainsGroupMatcher extends AbstractItemMatcher<ContainsGroupMatcher> {

		public ContainsGroupMatcher(String name) {
			super(name);
		}

		public ContainsGroupMatcher(String name, String type, Class<?> sourceType,
				String description, Matcher<?> defaultValue, boolean deprecated) {
			super(name, type, sourceType, description, defaultValue, deprecated);
		}

		@Override
		protected ItemMetadata getItemMetadata(Object item) {
			ConfigurationMetadata metadata = (ConfigurationMetadata) item;
			return getFirstItemWithName(metadata.getGroups(), this.name);
		}

		@Override
		protected ContainsGroupMatcher create(String name, String type, Class<?> sourceType,
				String description, Matcher<?> defaultValue, boolean deprecated) {
			return new ContainsGroupMatcher(name, type, sourceType, description, defaultValue, deprecated);
		}
	}

	public static class ContainsPropertyMatcher extends AbstractItemMatcher<ContainsPropertyMatcher> {

		private final TypeDescriptor typeDescriptor;

		public ContainsPropertyMatcher(String name) {
			super(name);
			this.typeDescriptor = null;
		}

		public ContainsPropertyMatcher(String name, TypeDescriptor type, Class<?> sourceType,
				String description, Matcher<?> defaultValue, boolean deprecated) {
			super(name, "", sourceType, description, defaultValue, deprecated);
			this.typeDescriptor = type;
		}

		@Override
		protected ItemMetadata getItemMetadata(Object item) {
			ConfigurationMetadata metadata = (ConfigurationMetadata) item;
			return getFirstItemWithName(metadata.getProperties(), this.name);
		}

		@Override
		protected ContainsPropertyMatcher create(String name, String type, Class<?> sourceType,
				String description, Matcher<?> defaultValue, boolean deprecated) {
			return new ContainsPropertyMatcher(name, createFrom(type), sourceType, description, defaultValue, deprecated);
		}

		private static TypeDescriptor createFrom(String type) {
			return (type != null ? new TypeDescriptor(type) : null);
		}
	}

}
