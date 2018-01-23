/*
 * Copyright 2012-2018 the original author or authors.
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
 * @see ConfigurationProperties
 */
final class ConfigurationPropertiesBindingOptions {

	private final String prefix;

	private final boolean ignoreInvalidFields;

	private final boolean ignoreUnknownFields;

	private final boolean validated;

	/**
	 * Create a new instance.
	 * @param prefix name prefix of the properties
	 * @param ignoreInvalidFields indicate that when binding to this object invalid fields
	 * should be ignored
	 * @param ignoreUnknownFields indicate that when binding to this object unknown fields
	 * @param validated indicate that this object should be validated
	 */
	ConfigurationPropertiesBindingOptions(String prefix,
			boolean ignoreInvalidFields, boolean ignoreUnknownFields, boolean validated) {
		this.prefix = prefix;
		this.ignoreInvalidFields = ignoreInvalidFields;
		this.ignoreUnknownFields = ignoreUnknownFields;
		this.validated = validated;
	}

	/**
	 * Create  an instance based on the specified {@link ConfigurationProperties
	 * annotation}.
	 * @param annotation the annotation to process
	 * @return the options defined by the {@code annotation}
	 */
	public static ConfigurationPropertiesBindingOptions of(
			ConfigurationProperties annotation) {
		Assert.notNull(annotation, "Annotation must not be null");
		return new ConfigurationPropertiesBindingOptions(annotation.prefix(),
				annotation.ignoreInvalidFields(), annotation.ignoreUnknownFields(), true);
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

	public boolean isValidated() {
		return this.validated;
	}

	@Override
	public String toString() {
		StringBuilder details = new StringBuilder();
		details.append("prefix=").append(this.prefix);
		details.append(", ignoreInvalidFields=").append(this.ignoreInvalidFields);
		details.append(", ignoreUnknownFields=").append(this.ignoreUnknownFields);
		details.append(", validated=").append(this.validated);
		return details.toString();
	}

}
