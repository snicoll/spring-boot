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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepositoryJsonBuilder;
import org.springframework.boot.configurationmetadata.diff.ConfigurationMetadataDiff;
import org.springframework.boot.configurationmetadata.diff.ConfigurationMetadataDiffFactory;

/**
 * TODO: temporary to demonstrate how the diff can be invoked
 *
 * @author Stephane Nicoll
 */
public final class ExampleAsciidoctorDiffGenerator {

	private final Path leftDirectory;

	private final Path rightDirectory;

	ExampleAsciidoctorDiffGenerator(Path leftDirectory, Path righDirectory) {
		this.leftDirectory = leftDirectory;
		this.rightDirectory = righDirectory;
	}

	public void generateDiff(PrintWriter writer) throws IOException {
		String leftVersion = extractVersion(this.leftDirectory);
		String rightVersion = extractVersion(this.rightDirectory);
		ConfigurationMetadataDiff diff = new ConfigurationMetadataDiffFactory().diff(
				loadConfigurationMetadataRepository(this.leftDirectory),
				loadConfigurationMetadataRepository(this.rightDirectory));
		AsciidoctorDiffGenerator generator = new AsciidoctorDiffGenerator(diff, leftVersion, rightVersion);
		generator.generate(writer);
	}

	private String extractVersion(Path directory) {
		String directoryName = directory.getFileName().toString();
		return directoryName.substring("spring-boot-".length());
	}

	private ConfigurationMetadataRepository loadConfigurationMetadataRepository(Path directory) throws IOException {
		ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
		List<Path> metadataFiles = Files.list(directory)
				.filter((path) -> Files.isReadable(path) && path.getFileName().toString().endsWith(".json"))
				.collect(Collectors.toList());
		for (Path metadataFile : metadataFiles) {
			builder.withJsonResource(Files.newInputStream(metadataFile));
		}
		return builder.build();
	}

	public static void main(String[] args) throws IOException {
		FileSystem fs = FileSystems.getDefault();
		Path leftDirectory = (args.length >= 1) ? fs.getPath(args[0])
				: fs.getPath("src/test/resources/temp/spring-boot-2.3.5.RELEASE");
		Path rightDirectory = (args.length >= 2) ? fs.getPath(args[1])
				: fs.getPath("src/test/resources/temp/spring-boot-2.4.0-RC1");
		StringWriter writer = new StringWriter();
		new ExampleAsciidoctorDiffGenerator(leftDirectory, rightDirectory).generateDiff(new PrintWriter(writer));
		System.out.println(writer.toString());
	}

}
