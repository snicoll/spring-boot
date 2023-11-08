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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.jarmode.layertools.JarStructure.Entry;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IndexedJarStructure}.
 *
 * @author Stephane Nicoll
 */
class IndexedJarStructureTests {

	@Test
	void createWhenIndexFileIsEmptyThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> createInstance(" \n "))
			.withMessage("Empty classpath index file loaded");
	}

	@Test
	void createWhenIndexFileIsMalformedThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> createInstance("test"))
			.withMessage("Classpath index file is malformed");
	}

	@Test
	void createWhenManifestDoesNotHaveLibLocationThrowsException() {
		Manifest manifest = createDefaultManifest();
		manifest.getMainAttributes().remove(new Name("Spring-Boot-Lib"));
		assertThatIllegalStateException().isThrownBy(() -> new IndexedJarStructure(manifest, getIndex()))
			.withMessage("Manifest attribute 'Spring-Boot-Lib' is mandatory");
	}

	@Test
	void createWhenManifestDoesNotHaveClassesLocationThrowsException() {
		Manifest manifest = createDefaultManifest();
		manifest.getMainAttributes().remove(new Name("Spring-Boot-Classes"));
		assertThatIllegalStateException().isThrownBy(() -> new IndexedJarStructure(manifest, getIndex()))
			.withMessage("Manifest attribute 'Spring-Boot-Classes' is mandatory");
	}

	@Test
	void toRunManifestCreateClasspathEntriesInOrder() throws Exception {
		IndexedJarStructure jarStructure = createInstance(getIndex());
		Manifest manifest = jarStructure.createRunJarManifest((lib) -> "libs/" + lib);
		assertThat(manifest.getMainAttributes().get(Name.CLASS_PATH)).isEqualTo("libs/a.jar libs/b.jar");
	}

	@Test
	void resolveEntryOnMatchingLib() throws Exception {
		IndexedJarStructure jarStructure = createInstance(getIndex());
		assertThat(jarStructure.resolve(mockEntry("BOOT-INF/lib/a.jar")))
			.isEqualTo(new Entry("BOOT-INF/lib/a.jar", "a.jar", true));
	}

	@Test
	void resolveEntryOnNonMatchingLib() throws Exception {
		IndexedJarStructure jarStructure = createInstance(getIndex());
		assertThat(jarStructure.resolve(mockEntry("BOOT-INF/lib/not-in-index.jar"))).isNull();
	}

	@Test
	void resolveEntryOnClass() throws Exception {
		IndexedJarStructure jarStructure = createInstance(getIndex());
		assertThat(jarStructure.resolve(mockEntry("BOOT-INF/classes/com/example/DemoApplication.class")))
			.isEqualTo(new Entry("BOOT-INF/classes/com/example/DemoApplication.class",
					"com/example/DemoApplication.class", false));
	}

	@Test
	void getFileTypeOnNonMatchingResource() throws Exception {
		IndexedJarStructure jarStructure = createInstance(getIndex());
		assertThat(jarStructure.resolve(mockEntry("META-INF/test.txt"))).isNull();
	}

	@Test
	void getShouldReturnIndexedLayersFromContext(@TempDir File temp) throws Exception {
		Context context = mock(Context.class);
		given(context.getArchiveFile()).willReturn(createJarFile(temp, "test.jar"));
		IndexedJarStructure jarStructure = IndexedJarStructure.get(context);
		Manifest manifest = jarStructure.createRunJarManifest((dependency) -> "dependencies/" + dependency);
		Attributes attributes = manifest.getMainAttributes();
		assertThat(attributes.get(Name.MAIN_CLASS)).isEqualTo("com.example.DemoApplication");
		assertThat(attributes.get(Name.CLASS_PATH)).isEqualTo("dependencies/a.jar dependencies/b.jar");
		assertThat(jarStructure.resolve(mockEntry("BOOT-INF/classes/com/example/Test.class")))
			.satisfies((entry) -> assertThat(entry.location()).isEqualTo("com/example/Test.class"));
		assertThat(jarStructure.resolve(mockEntry("BOOT-INF/lib/a.jar")))
			.satisfies((entry) -> assertThat(entry.location()).isEqualTo("a.jar"));
		assertThat(jarStructure.resolve(mockEntry("BOOT-INF/lib/c.jar"))).isNull();
	}

	private IndexedJarStructure createInstance(String indexFile) {
		return new IndexedJarStructure(createDefaultManifest(), indexFile);
	}

	private Manifest createDefaultManifest() {
		return createManifest("com.example.DemoApplication", "BOOT-INF/lib/", "BOOT-INF/classes/");
	}

	private Manifest createManifest(String className, String libLocation, String classesLocation) {
		Manifest manifest = new Manifest();
		Attributes attributes = manifest.getMainAttributes();
		attributes.putValue("Start-Class", className);
		attributes.putValue("Spring-Boot-Lib", libLocation);
		attributes.putValue("Spring-Boot-Classes", classesLocation);
		return manifest;
	}

	private String getIndex() throws Exception {
		return getFile("test-classpath.idx");
	}

	private String getFile(String fileName) throws Exception {
		ClassPathResource resource = new ClassPathResource(fileName, getClass());
		InputStreamReader reader = new InputStreamReader(resource.getInputStream());
		return FileCopyUtils.copyToString(reader);
	}

	private ZipEntry mockEntry(String name) {
		ZipEntry entry = mock(ZipEntry.class);
		given(entry.getName()).willReturn(name);
		return entry;
	}

	private File createJarFile(File temp, String name) throws Exception {
		File file = new File(temp, name);
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
			createEntries(out, "BOOT-INF/lib/a.jar", "BOOT-INF/lib/b.jar", "BOOT-INF/lib/c.jar",
					"BOOT-INF/classes/com/example/DemoApplication.class");
			out.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
			out.write(getFile("test-manifest.MF").getBytes());
			out.closeEntry();
			out.putNextEntry(new ZipEntry("BOOT-INF/classpath.idx"));
			out.write(getIndex().getBytes());
			out.closeEntry();
		}
		return file;
	}

	private static void createEntries(ZipOutputStream out, String... paths) throws IOException {
		for (String path : paths) {
			out.putNextEntry(new ZipEntry(path));
			out.closeEntry();
		}
	}

}
