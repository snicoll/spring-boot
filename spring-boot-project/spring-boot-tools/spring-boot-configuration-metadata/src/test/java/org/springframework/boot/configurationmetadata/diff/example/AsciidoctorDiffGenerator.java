/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.configurationmetadata.diff.example;

import java.io.PrintWriter;
import java.text.BreakIterator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation;
import org.springframework.boot.configurationmetadata.diff.ConfigurationMetadataDiff;
import org.springframework.boot.configurationmetadata.diff.ConfigurationMetadataDiffEntry;
import org.springframework.boot.configurationmetadata.diff.DiffPredicates;

/**
 * Generates a diff between two versions in asciidoc format.
 *
 * @author Stephane Nicoll
 */
class AsciidoctorDiffGenerator {

	private final ConfigurationMetadataDiff diff;

	private final String leftVersion;

	private final String rightVersion;

	AsciidoctorDiffGenerator(ConfigurationMetadataDiff diff, String leftVersion, String rightVersion) {
		this.diff = diff;
		this.leftVersion = leftVersion;
		this.rightVersion = rightVersion;
	}

	void generate(PrintWriter writer) {
		writer.append(String.format("Configuration properties change between `%s` and " + "`%s`%n", this.leftVersion,
				this.rightVersion));
		writer.append(System.lineSeparator());
		writer.append(String.format("== Deprecated in `%s`%n", this.rightVersion));
		appendDeprecatedProperties(writer);
		writer.append(System.lineSeparator());
		writer.append(String.format("== New in `%s`%n", this.rightVersion));
		appendAddedProperties(writer);
		writer.append(System.lineSeparator());
		writer.append(String.format("== Removed in `%s`%n", this.rightVersion));
		appendRemovedProperties(writer);
	}

	private void appendDeprecatedProperties(PrintWriter writer) {
		List<ConfigurationMetadataDiffEntry<ConfigurationMetadataProperty>> properties = this.diff.properties()
				.filter(DiffPredicates.DEPRECATED).sorted(Comparator.comparing(ConfigurationMetadataDiffEntry::getId))
				.collect(Collectors.toList());
		if (properties.isEmpty()) {
			writer.append(String.format("None.%n"));
		}
		else {
			writer.append(String.format("|======================%n"));
			writer.append(String.format("|Key  |Replacement |Reason%n"));
			properties.forEach((diff) -> {
				ConfigurationMetadataProperty property = diff.getRight();
				appendDeprecatedProperty(writer, property);
			});
			writer.append(String.format("|======================%n"));
		}
		writer.append(String.format("%n%n"));
	}

	private void appendAddedProperties(PrintWriter writer) {
		List<ConfigurationMetadataDiffEntry<ConfigurationMetadataProperty>> properties = this.diff.properties()
				.filter(DiffPredicates.ADDED).sorted(Comparator.comparing(ConfigurationMetadataDiffEntry::getId))
				.collect(Collectors.toList());
		if (properties.isEmpty()) {
			writer.append(String.format("None.%n"));
		}
		else {
			writer.append(String.format("|======================%n"));
			writer.append(String.format("|Key  |Default value |Description%n"));
			properties.forEach((diff) -> appendRegularProperty(writer, diff.getRight()));
			writer.append(String.format("|======================%n"));
		}
		writer.append(String.format("%n%n"));
	}

	private void appendRemovedProperties(PrintWriter writer) {
		List<ConfigurationMetadataDiffEntry<ConfigurationMetadataProperty>> properties = this.diff.properties()
				.filter(DiffPredicates.REMOVED).sorted(Comparator.comparing(ConfigurationMetadataDiffEntry::getId))
				.collect(Collectors.toList());
		if (properties.isEmpty()) {
			writer.append(String.format("None.%n"));
		}
		else {
			writer.append(String.format("|======================%n"));
			writer.append(String.format("|Key  |Replacement |Reason%n"));
			properties.forEach((diff) -> {
				if (diff.getRight() != null) {
					appendDeprecatedProperty(writer, diff.getRight());
				}
				else {
					appendRegularProperty(writer, diff.getLeft());
				}
			});
			writer.append(String.format("|======================%n"));
		}
	}

	private void appendRegularProperty(PrintWriter writer, ConfigurationMetadataProperty property) {
		writer.append("|`").append(property.getId()).append("` |");
		if (property.getDefaultValue() != null) {
			writer.append("`").append(defaultValueToString(property.getDefaultValue())).append("`");
		}
		writer.append(" |");
		if (property.getDescription() != null) {
			writer.append(property.getShortDescription());
		}
		writer.append(System.lineSeparator());
	}

	private void appendDeprecatedProperty(PrintWriter writer, ConfigurationMetadataProperty property) {
		Deprecation deprecation = property.getDeprecation();
		writer.append("|`").append(property.getId()).append("` |");
		if (deprecation.getReplacement() != null) {
			writer.append("`").append(deprecation.getReplacement()).append("`");
		}
		writer.append(" |");
		if (deprecation.getReason() != null) {
			writer.append(extractFirstSentence(deprecation.getReason()));
		}
		writer.append(System.lineSeparator());
	}

	private String defaultValueToString(Object defaultValue) {
		if (defaultValue instanceof Object[]) {
			return Arrays.stream((Object[]) defaultValue).map(Object::toString).collect(Collectors.joining(","));
		}
		else {
			return defaultValue.toString();
		}
	}

	private String extractFirstSentence(String text) {
		if (text == null) {
			return null;
		}
		int dot = text.indexOf('.');
		if (dot != -1) {
			BreakIterator breakIterator = BreakIterator.getSentenceInstance(Locale.US);
			breakIterator.setText(text);
			String sentence = text.substring(breakIterator.first(), breakIterator.next()).trim();
			return removeSpaceBetweenLine(sentence);
		}
		else {
			String[] lines = text.split(System.lineSeparator());
			return lines[0].trim();
		}
	}

	private String removeSpaceBetweenLine(String text) {
		String[] lines = text.split(System.lineSeparator());
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append(line.trim()).append(" ");
		}
		return sb.toString().trim();
	}

}
