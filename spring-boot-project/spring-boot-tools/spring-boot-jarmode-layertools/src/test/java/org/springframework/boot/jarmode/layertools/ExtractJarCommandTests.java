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

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.jarmode.layertools.Command.Option;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link ExtractJarCommand}.
 *
 * @author Stephane Nicoll
 */
@ExtendWith(MockitoExtension.class)
class ExtractJarCommandTests {

	private static final FileTime CREATION_TIME = FileTime.from(Instant.now().minus(3, ChronoUnit.DAYS));

	private static final FileTime LAST_MODIFIED_TIME = FileTime.from(Instant.now().minus(2, ChronoUnit.DAYS));

	private static final FileTime LAST_ACCESS_TIME = FileTime.from(Instant.now().minus(1, ChronoUnit.DAYS));

	@TempDir
	Path temp;

	@Mock
	private Context context;

	private Path extract;

	@BeforeEach
	void setup() throws Exception {
		this.extract = this.temp.resolve("extract");
		Files.createDirectories(this.extract);
	}

	@Test
	void runCreatesExplodedStructure() throws IOException {
		invoke(createJarFile("test.jar"));

		assertThat(this.extract.toFile().list()).containsOnly("application", "dependencies", "run-app.jar");
		assertThat(this.extract.resolve("application").toFile().list()).containsOnly("test.jar");
		assertThat(this.extract.resolve("dependencies").toFile().list()).containsOnly("a.jar", "b.jar");
	}

	@Test
	void runAppliesTimeAttributesOnLibraries() throws IOException {
		invoke(createJarFile("test.jar"));

		assertThat(this.extract.resolve("dependencies/a.jar")).exists().satisfies(this::timeAttributes);
		assertThat(this.extract.resolve("dependencies/b.jar")).exists().satisfies(this::timeAttributes);
	}

	@Test
	void runCreatesRunAppJarWithProperManifest() throws IOException {
		invoke(createJarFile("my-app.jar"));

		Path runAppJar = this.extract.resolve("run-app.jar");
		assertManifest(runAppJar, (manifest) -> {
			Attributes attributes = manifest.getMainAttributes();
			assertThat(attributes.getValue(Name.CLASS_PATH))
				.isEqualTo("application/my-app.jar dependencies/a.jar dependencies/b.jar");
			assertThat(attributes.getValue(Name.MAIN_CLASS)).isEqualTo("com.example.DemoApplication");
		});
	}

	@Test
	void runWithAdditionalJarsCopyExtLibraries() throws IOException {
		Path libs = this.temp.resolve("libs");
		Files.createDirectories(libs);
		Path extA = Files.createFile(libs.resolve("ext-a.jar"));
		Path extB = Files.createFile(libs.resolve("ext-b.jar"));
		Map<Option, String> options = Map.of(ExtractJarCommand.ADDITIONAL_JARS_OPTION, "%s,%s".formatted(extA, extB));
		invoke(createJarFile("my-app.jar"), options);

		assertThat(this.extract.resolve("ext").toFile().list()).containsOnly("ext-a.jar", "ext-b.jar");
		Path runAppJar = this.extract.resolve("run-app.jar");
		assertManifest(runAppJar, (manifest) -> {
			Attributes attributes = manifest.getMainAttributes();
			assertThat(attributes.getValue(Name.CLASS_PATH))
				.isEqualTo("application/my-app.jar dependencies/a.jar dependencies/b.jar ext/ext-a.jar ext/ext-b.jar");
		});
	}

	private void assertManifest(Path file, Consumer<Manifest> assertions) throws IOException {
		assertThat(file).exists().isNotEmptyFile();
		try (JarFile jarFile = new JarFile(file.toFile())) {
			Manifest manifest = jarFile.getManifest();
			assertions.accept(manifest);
		}
	}

	private void invoke(Path jarFile, Map<Option, String> options) {
		given(this.context.getArchiveFile()).willReturn(jarFile.toFile());
		given(this.context.getWorkingDir()).willReturn(this.extract.toFile());
		ExtractJarCommand command = new ExtractJarCommand(this.context);
		command.run(options, Collections.emptyList());
	}

	private void invoke(Path jarFile) {
		invoke(jarFile, Collections.emptyMap());
	}

	private void timeAttributes(Path file) {
		try {
			BasicFileAttributes basicAttributes = Files.getFileAttributeView(file, BasicFileAttributeView.class)
				.readAttributes();
			assertThat(basicAttributes.lastModifiedTime().to(TimeUnit.SECONDS))
				.isEqualTo(LAST_MODIFIED_TIME.to(TimeUnit.SECONDS));
			assertThat(basicAttributes.creationTime().to(TimeUnit.SECONDS)).satisfiesAnyOf(
					(creationTime) -> assertThat(creationTime).isEqualTo(CREATION_TIME.to(TimeUnit.SECONDS)),
					// On macOS (at least) the creation time is the last modified time
					(creationTime) -> assertThat(creationTime).isEqualTo(LAST_MODIFIED_TIME.to(TimeUnit.SECONDS)));
			assertThat(basicAttributes.lastAccessTime().to(TimeUnit.SECONDS))
				.isEqualTo(LAST_ACCESS_TIME.to(TimeUnit.SECONDS));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private Path createJarFile(String name) throws IOException {
		return createJarFile(name, (out) -> {
		});
	}

	private Path createJarFile(String name, Consumer<ZipOutputStream> streamHandler) throws IOException {
		Path file = this.temp.resolve(name);
		try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(file))) {
			createDirectory(out, "BOOT-INF/");
			createDirectory(out, "BOOT-INF/classes/");
			createDirectory(out, "BOOT-INF/classes/com/");
			createDirectory(out, "BOOT-INF/classes/com/example/");
			createEntry(out, "BOOT-INF/classes/com/example/DemoApplication.class", "some binary");
			createEntry(out, "BOOT-INF/classes/application.properties", "prop=value");
			createEntry(out, "BOOT-INF/classpath.idx", getFileContent("test-classpath.idx"));
			createEntry(out, "hello.txt", "To be ignored");
			createDirectory(out, "BOOT-INF/lib/");
			createEmptyEntries(out, "BOOT-INF/lib/a.jar", "BOOT-INF/lib/b.jar", "BOOT-INF/lib/c.jar");
			createDirectory(out, "META-INF/");
			out.putNextEntry(entry("META-INF/MANIFEST.MF"));
			out.write(getFileContent("test-manifest.MF").getBytes());
			out.closeEntry();
			streamHandler.accept(out);
		}
		return file;
	}

	private String getFileContent(String fileName) throws IOException {
		ClassPathResource resource = new ClassPathResource(fileName, getClass());
		InputStreamReader reader = new InputStreamReader(resource.getInputStream());
		return FileCopyUtils.copyToString(reader);
	}

	private static void createEmptyEntries(ZipOutputStream out, String... paths) throws IOException {
		for (String path : paths) {
			out.putNextEntry(entry(path));
			out.closeEntry();
		}
	}

	private static void createDirectory(ZipOutputStream out, String path) throws IOException {
		if (!path.endsWith("/")) {
			throw new IllegalArgumentException("Path should end with / " + path);
		}
		out.putNextEntry(entry(path));
		out.closeEntry();
	}

	private static void createEntry(ZipOutputStream out, String path, String content) throws IOException {
		out.putNextEntry(entry(path));
		out.write(content.getBytes());
		out.closeEntry();
	}

	private static ZipEntry entry(String path) {
		ZipEntry entry = new ZipEntry(path);
		entry.setCreationTime(CREATION_TIME);
		entry.setLastModifiedTime(LAST_MODIFIED_TIME);
		entry.setLastAccessTime(LAST_ACCESS_TIME);
		return entry;
	}

}
