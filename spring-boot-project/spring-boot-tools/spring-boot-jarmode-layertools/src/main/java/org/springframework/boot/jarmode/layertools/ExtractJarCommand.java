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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.boot.jarmode.layertools.JarStructure.Entry;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * Hacking.
 *
 * @author Stephane Nicoll
 */
class ExtractJarCommand extends Command {

	static final Option DESTINATION_OPTION = Option.of("destination", "string", "The destination to extract files to");

	private final Context context;

	private final JarStructure jarStructure;

	ExtractJarCommand(Context context) {
		this(context, IndexedJarStructure.get(context));
	}

	ExtractJarCommand(Context context, JarStructure jarStructure) {
		super("extract2", "Extracts the application in optimized structure", Options.of(DESTINATION_OPTION),
				Parameters.none());
		this.context = context;
		this.jarStructure = jarStructure;
	}

	@Override
	protected void run(Map<Option, String> options, List<String> parameters) {
		try {
			Path destination = options.containsKey(DESTINATION_OPTION) ? Paths.get(options.get(DESTINATION_OPTION))
					: this.context.getWorkingDir().toPath();
			Path application = destination.resolve("application");
			mkDirs(application);
			Path applicationJarFile = application.resolve(this.context.getArchiveFile().getName());

			Path dependencies = destination.resolve("dependencies");
			mkDirs(dependencies);

			try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(applicationJarFile),
					this.jarStructure.createApplicationJarManifest())) {
				try (ZipInputStream zip = new ZipInputStream(new FileInputStream(this.context.getArchiveFile()))) {
					ZipEntry zipEntry = zip.getNextEntry();
					Assert.state(zipEntry != null, "File '" + this.context.getArchiveFile().toString()
							+ "' is not compatible with layertools; ensure jar file is valid and launch script is not enabled");
					while (zipEntry != null) {
						Entry resolvedEntry = this.jarStructure.resolve(zipEntry);
						if (resolvedEntry != null) {
							if (resolvedEntry.library()) {
								String location = "dependencies/" + resolvedEntry.location();
								write(zip, zipEntry, destination, location);
							}
							else {
								JarEntry jarEntry = createJarEntry(resolvedEntry, zipEntry);
								output.putNextEntry(jarEntry);
								StreamUtils.copy(zip, output);
								output.closeEntry();
							}
						}
						zipEntry = zip.getNextEntry();
					}
				}
			}

			// create the run-app.jar
			createRunAppJar(destination, applicationJarFile);

		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}

	}

	private void createRunAppJar(Path destination, Path applicationJarFile) throws IOException {
		Manifest manifest = this.jarStructure.createRunJarManifest((dependency) -> "dependencies/" + dependency);
		String libs = manifest.getMainAttributes().getValue(Name.CLASS_PATH);
		String applicationJar = destination.relativize(applicationJarFile).toString();
		manifest.getMainAttributes().put(Name.CLASS_PATH, String.join(" ", applicationJar, libs));
		Path runJar = destination.resolve("run-app.jar");
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(runJar), manifest)) {
			// EMPTY
		}
	}

	private void write(ZipInputStream zip, ZipEntry entry, Path destination, String name) throws IOException {
		String canonicalOutputPath = destination.toFile().getCanonicalPath() + File.separator;
		Path file = destination.resolve(name);
		String canonicalEntryPath = file.toFile().getCanonicalPath();
		Assert.state(canonicalEntryPath.startsWith(canonicalOutputPath),
				() -> "Entry '" + entry.getName() + "' would be written to '" + canonicalEntryPath
						+ "'. This is outside the output location of '" + canonicalOutputPath
						+ "'. Verify the contents of your archive.");
		mkParentDirs(file);
		try (OutputStream out = Files.newOutputStream(file)) {
			StreamUtils.copy(zip, out);
		}
		try {
			Files.getFileAttributeView(file, BasicFileAttributeView.class)
				.setTimes(entry.getLastModifiedTime(), entry.getLastAccessTime(), entry.getCreationTime());
		}
		catch (IOException ex) {
			// File system does not support setting time attributes. Continue.
		}
	}

	private static JarEntry createJarEntry(Entry resolvedEntry, ZipEntry originalEntry) {
		JarEntry jarEntry = new JarEntry(resolvedEntry.location());
		FileTime lastModifiedTime = originalEntry.getLastModifiedTime();
		if (lastModifiedTime != null) {
			jarEntry.setLastModifiedTime(lastModifiedTime);
		}
		FileTime lastAccessTime = originalEntry.getLastAccessTime();
		if (lastAccessTime != null) {
			jarEntry.setLastAccessTime(lastAccessTime);
		}
		FileTime creationTime = originalEntry.getCreationTime();
		if (creationTime != null) {
			jarEntry.setCreationTime(creationTime);
		}
		return jarEntry;
	}

	private static void mkParentDirs(Path file) throws IOException {
		mkDirs(file.getParent());
	}

	private static void mkDirs(Path file) throws IOException {
		Files.createDirectories(file);
	}

}
