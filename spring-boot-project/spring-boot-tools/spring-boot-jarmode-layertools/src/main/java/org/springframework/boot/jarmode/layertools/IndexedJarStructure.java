/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.jarmode.layertools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link JarStructure} implementation backed by a {@code classpath.idx} file.
 *
 * @author Stephane Nicoll
 */
class IndexedJarStructure implements JarStructure {

	private final Manifest originalManifest;

	private final String libLocation;

	private final String classesLocation;

	private final List<String> classpathEntries;

	IndexedJarStructure(Manifest originalManifest, String indexFile) {
		this.originalManifest = originalManifest;
		this.libLocation = getLocation(originalManifest, "Spring-Boot-Lib");
		this.classesLocation = getLocation(originalManifest, "Spring-Boot-Classes");
		this.classpathEntries = readIndexFile(indexFile);
	}

	private static String getLocation(Manifest manifest, String attribute) {
		String location = getMandatoryAttribute(manifest, attribute);
		if (!location.endsWith("/")) {
			location = location + "/";
		}
		return location;
	}

	private static List<String> readIndexFile(String indexFile) {
		String[] lines = Arrays.stream(indexFile.split("\n"))
			.map((line) -> line.replace("\r", ""))
			.filter(StringUtils::hasText)
			.toArray(String[]::new);
		List<String> classpathEntries = new ArrayList<>();
		for (String line : lines) {
			if (line.startsWith("- ")) {
				classpathEntries.add(line.substring(3, line.length() - 1));
			}
			else {
				throw new IllegalStateException("Classpath index file is malformed");
			}
		}
		Assert.state(!classpathEntries.isEmpty(), "Empty classpath index file loaded");
		return classpathEntries;
	}

	@Override
	public Entry resolve(ZipEntry entry) {
		if (entry.isDirectory()) {
			return null;
		}
		String name = entry.getName();
		if (this.classpathEntries.contains(name)) {
			return new Entry(name, toStructureDependency(name), true);
		}
		else if (name.startsWith(this.classesLocation)) {
			return new Entry(name, name.substring(this.classesLocation.length()), false);
		}
		return null;
	}

	@Override
	public Manifest createRunJarManifest(UnaryOperator<String> libEntry) {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.put(Name.MANIFEST_VERSION, "1.0");
		attributes.put(Name.MAIN_CLASS, getMandatoryAttribute(this.originalManifest, "Start-Class"));
		attributes.put(Name.CLASS_PATH,
				this.classpathEntries.stream()
					.map(this::toStructureDependency)
					.map(libEntry)
					.collect(Collectors.joining(" ")));
		return manifest;
	}

	@Override
	public Manifest createApplicationJarManifest() {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.put(Name.MANIFEST_VERSION, "1.0");
		copy(this.originalManifest, manifest, "Implementation-Title");
		copy(this.originalManifest, manifest, "Implementation-Version");
		return manifest;
	}

	private static void copy(Manifest left, Manifest right, String attribute) {
		String value = left.getMainAttributes().getValue(attribute);
		if (value != null) {
			right.getMainAttributes().putValue(attribute, value);
		}
	}

	private String toStructureDependency(String libEntryName) {
		Assert.state(libEntryName.startsWith(this.libLocation), "Invalid library location " + libEntryName);
		return libEntryName.substring(this.libLocation.length());
	}

	private static String getMandatoryAttribute(Manifest manifest, String attribute) {
		String value = manifest.getMainAttributes().getValue(attribute);
		if (value == null) {
			throw new IllegalStateException("Manifest attribute '" + attribute + "' is mandatory");
		}
		return value;
	}

	static IndexedJarStructure get(Context context) {
		try {
			try (JarFile jarFile = new JarFile(context.getArchiveFile())) {
				Manifest manifest = jarFile.getManifest();
				String location = getMandatoryAttribute(manifest, "Spring-Boot-Classpath-Index");
				ZipEntry entry = jarFile.getEntry(location);
				if (entry != null) {
					String indexFile = StreamUtils.copyToString(jarFile.getInputStream(entry), StandardCharsets.UTF_8);
					return new IndexedJarStructure(manifest, indexFile);
				}
			}
			return null;
		}
		catch (FileNotFoundException | NoSuchFileException ex) {
			return null;
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
