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

package org.springframework.boot.context.properties;

import org.springframework.util.Assert;

/**
 * Configuration properties binding options.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see ConfigurationProperties
 */
public final class ConfigurationPropertiesBindingOptions {

	private final String prefix;

	private final boolean ignoreInvalidFields;

	private final boolean ignoreUnknownFields;

	/**
	 * Create a new instance.
	 * @param prefix name prefix of the properties
	 * @param ignoreInvalidFields indicate that when binding to this object invalid fields
	 * should be ignored
	 * @param ignoreUnknownFields indicate that when binding to this object unknown fields
	 * should be ignored.
	 */
	public ConfigurationPropertiesBindingOptions(String prefix,
			boolean ignoreInvalidFields, boolean ignoreUnknownFields) {
		this.prefix = prefix;
		this.ignoreInvalidFields = ignoreInvalidFields;
		this.ignoreUnknownFields = ignoreUnknownFields;
	}

	/**
	 * Create an instance from the specified {@link ConfigurationProperties} annotation.
	 * @param annotation the annotation
	 * @return the options defined by the specified annotation
	 */
	public static ConfigurationPropertiesBindingOptions fromAnnotation(
			ConfigurationProperties annotation) {
		Assert.notNull(annotation, "Properties must not be null");
		return new ConfigurationPropertiesBindingOptions(annotation.prefix(),
				annotation.ignoreInvalidFields(), annotation.ignoreUnknownFields());
	}

	public String getPrefix() {
		return this.prefix;
	}

	public boolean isIgnoreInvalidFields() {
		return this.ignoreInvalidFields;
	}

	public boolean isIgnoreUnknownFields() {
		return this.ignoreUnknownFields;
	}

	@Override
	public String toString() {
		StringBuilder details = new StringBuilder();
		details.append("prefix=").append(this.prefix);
		details.append(", ignoreInvalidFields=").append(this.ignoreInvalidFields);
		details.append(", ignoreUnknownFields=").append(this.ignoreUnknownFields);
		return details.toString();
	}

}
