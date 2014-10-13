/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.config.processor;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import org.springframework.boot.config.ConfigMetadataRepository;
import org.springframework.boot.config.processor.mapper.ConfigMetadataRepositoryJsonMapper;
import org.springframework.boot.config.processor.mapper.ConfigMetadataRepositoryMapper;
import org.springframework.boot.config.test.AnnotationProcessorConfig;
import org.springframework.boot.config.test.AnnotationProcessorEmbeddedCompiler;
import org.springframework.boot.config.test.CompilationOutput;

/**
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractProcessorTests {

	@Rule
	public final TemporaryFolder folder = new TemporaryFolder();

	private final AnnotationProcessorEmbeddedCompiler compiler;

	private final ConfigMetadataRepositoryMapper mapper;

	protected AbstractProcessorTests() {
		final AnnotationProcessorConfig config =
				new AnnotationProcessorConfig(ConfigMetadataProcessor.class);
		config.setProcOnly(true);
		this.compiler = new AnnotationProcessorEmbeddedCompiler(config);
		this.mapper = new ConfigMetadataRepositoryJsonMapper();
	}

	protected AnnotationProcessorEmbeddedCompiler getCompiler() {
		return compiler;
	}

	protected CompilationOutput compile(Class<?>... configClasses) {
		return getCompiler().process(createTempDir(), configClasses);
	}

	protected ConfigMetadataRepository process(Class<?>... configClasses) {
		try {
			CompilationOutput compilationOutput = compile(configClasses);
			return readConfigMetadataRepository(compilationOutput.getOutputDir());
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to read config metadata", e);
		}
	}


	protected ConfigMetadataRepository readConfigMetadataRepository(File baseDir) throws IOException {
		final File f = getConfigMetadataFile(baseDir);
		assertTrue("File [" + f.getAbsolutePath() + "] should exist, are you sure you are running the test " +
				"from the module directory?", f.exists());
		FileInputStream in = new FileInputStream(f);
		try {
			return mapper.readRepository(in);
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to read config metadata", e);
		}
		finally {
			in.close();
		}
	}

	/**
	 * Return the file holding the configuration metadata. May or may not exist.
	 * @param baseDir the base directory to use to locate the file
	 */
	protected File getConfigMetadataFile(File baseDir) {
		return new File(baseDir, "META-INF/spring-harvested-config.metadata");
	}

	private File createTempDir() {
		try {
			return folder.newFolder();
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to create temp directory", e);
		}
	}

}
