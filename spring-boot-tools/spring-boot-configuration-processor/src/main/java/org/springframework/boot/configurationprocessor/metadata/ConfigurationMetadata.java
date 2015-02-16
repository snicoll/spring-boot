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

package org.springframework.boot.configurationprocessor.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration meta-data.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 * @see ItemMetadata
 */
public class ConfigurationMetadata {

	private static final Pattern CAMEL_CASE_PATTERN = Pattern.compile("([^A-Z-])([A-Z])");

	private final List<GroupMetadata> groups;

	private final List<PropertyMetadata> properties;

	public ConfigurationMetadata() {
		this.groups = new ArrayList<GroupMetadata>();
		this.properties = new ArrayList<PropertyMetadata>();
	}

	public ConfigurationMetadata(ConfigurationMetadata metadata) {
		this.groups = new ArrayList<GroupMetadata>(metadata.groups);
		this.properties = new ArrayList<PropertyMetadata>(metadata.properties);
	}

	public boolean isEmpty() {
		return this.groups.isEmpty() && this.properties.isEmpty();
	}

	/**
	 * Add group meta-data.
	 * @param groupMetadata the meta-data to add
	 */
	public void add(GroupMetadata groupMetadata) {
		this.groups.add(groupMetadata);
		Collections.sort(this.groups);
	}

	/**
	 * Add property meta-data.
	 * @param propertyMetadata the meta-data to add
	 */
	public void add(PropertyMetadata propertyMetadata) {
		this.properties.add(propertyMetadata);
		Collections.sort(this.properties);
	}

	/**
	 * Add all properties from another {@link ConfigurationMetadata}.
	 * @param metadata the {@link ConfigurationMetadata} instance to merge
	 */
	public void addAll(ConfigurationMetadata metadata) {
		this.groups.addAll(metadata.groups);
		this.properties.addAll(metadata.properties);
		Collections.sort(this.groups);
		Collections.sort(this.properties);
	}

	/**
	 * @return the meta-data groups.
	 */
	public List<GroupMetadata> getGroups() {
		return Collections.unmodifiableList(this.groups);
	}

	/**
	 * @return the meta-data properties.
	 */
	public List<PropertyMetadata> getProperties() {
		return Collections.unmodifiableList(this.properties);
	}

	public static String nestedPrefix(String prefix, String name) {
		String nestedPrefix = (prefix == null ? "" : prefix);
		String dashedName = toDashedCase(name);
		nestedPrefix += ("".equals(nestedPrefix) ? dashedName : "." + dashedName);
		return nestedPrefix;
	}

	static String toDashedCase(String name) {
		Matcher matcher = CAMEL_CASE_PATTERN.matcher(name);
		StringBuffer result = new StringBuffer();
		while (matcher.find()) {
			matcher.appendReplacement(result, getDashed(matcher));
		}
		matcher.appendTail(result);
		return result.toString().toLowerCase();
	}

	private static String getDashed(Matcher matcher) {
		String first = matcher.group(1);
		String second = matcher.group(2);
		if (first.equals("_")) {
			// not a word for the binder
			return first + second;
		}
		return first + "-" + second;
	}

}
