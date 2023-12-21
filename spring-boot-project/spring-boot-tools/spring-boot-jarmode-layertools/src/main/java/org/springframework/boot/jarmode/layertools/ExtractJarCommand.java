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
import java.util.ArrayList;
import java.util.Arrays;
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
import org.springframework.util.StringUtils;

/**
 * Hacking.
 *
 * @author Stephane Nicoll
 */
class ExtractJarCommand extends Command {

	static final Option DESTINATION_OPTION = Option.of("destination", "string", "The destination to extract files to");

	static final Option ADDITIONAL_JARS_OPTION = Option.of("additional-jars", "string",
			"Comma separated list of additional jars to include");

	static final Option QUIET = Option.of("quiet", "boolean",
			"Make some output more quiet");

	private final Context context;

	private final JarStructure jarStructure;

	ExtractJarCommand(Context context) {
		this(context, IndexedJarStructure.get(context));
	}

	ExtractJarCommand(Context context, JarStructure jarStructure) {
		super("extract2", "Extracts the application to an optimized structure",
				Options.of(DESTINATION_OPTION, ADDITIONAL_JARS_OPTION), Parameters.none());
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
								if (jarEntry != null) {
									output.putNextEntry(jarEntry);
									StreamUtils.copy(zip, output);
									output.closeEntry();
								}
							}
						}
						zipEntry = zip.getNextEntry();
					}
				}
			}

			List<String> additionalJars = handleAdditionalJars(destination, options.get(ADDITIONAL_JARS_OPTION));

			// create the run-app.jar
			createRunAppJar(destination, applicationJarFile, additionalJars);

			if (!options.containsKey(QUIET)) {
				System.out.println(createOutputMessage(destination));
			}

		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}

	}

	private List<String> handleAdditionalJars(Path destination, String additionalJars) throws IOException {
		List<String> paths = new ArrayList<>();
		if (additionalJars == null) {
			return paths;
		}
		List<Path> locations = Arrays.stream(StringUtils.commaDelimitedListToStringArray(additionalJars))
				.map(this::toFile)
				.toList();

		Path ext = destination.resolve("ext");
		mkDirs(ext);
		for (Path location : locations) {
			Path file = ext.resolve(location.getFileName());
			Files.copy(location, file);
		}
		return locations.stream().map((location) -> "ext/%s".formatted(location.getFileName())).toList();
	}

	private Path toFile(String location) {
		Path path = Paths.get(location);
		if (!Files.exists(path)) {
			throw new IllegalArgumentException("Additional jar not found " + path);
		}
		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException("Invalid location, should be a file " + path);
		}
		return path;
	}

	private void createRunAppJar(Path destination, Path applicationJarFile, List<String> additionalJars)
			throws IOException {
		Manifest manifest = this.jarStructure.createRunJarManifest((dependency) -> "dependencies/" + dependency,
				additionalJars);
		String libs = manifest.getMainAttributes().getValue(Name.CLASS_PATH);
		String applicationJar = destination.relativize(applicationJarFile).toString();
		manifest.getMainAttributes().put(Name.CLASS_PATH, String.join(" ", applicationJar, libs));
		Path runJar = destination.resolve("run-app.jar");
		try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(runJar), manifest)) {
			// EMPTY
		}
	}

	private String createOutputMessage(Path destination) {
		return """
				Application successfully extracted to '%s'
				    
				To start the application from that directory:
				    
				$ java -jar run-app.jar
				    
				TIP: To improve startup performance, you can create a CDS Archive with a single training run:
				    
				$ java -XX:ArchiveClassesAtExit=application.jsa -Dspring.context.exit=onRefresh -jar run-app.jar
				    
				Then use the generated cache:
				    
				$ java -XX:SharedArchiveFile=application.jsa -jar run-app.jar
				    
				See https://docs.spring.io/spring-framework/reference/integration/class-data-sharing.html for more details.
				""".formatted(destination);
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
		if (!StringUtils.hasLength(resolvedEntry.location())) {
			return null;
		}
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
