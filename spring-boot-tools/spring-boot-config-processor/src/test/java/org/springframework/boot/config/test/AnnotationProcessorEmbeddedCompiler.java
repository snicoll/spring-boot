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

package org.springframework.boot.config.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.rules.TemporaryFolder;

/**
 * Compile source code using the specified annotation {@link javax.annotation.processing.Processor}.
 * <p/>
 * Place the output of the compilation in a test directory suitable for
 * further inspections.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class AnnotationProcessorEmbeddedCompiler {

	/**
	 * The relative prefix where test source files can be found.
	 */
	public static final String JAVA_TEST_PREFIX = "src/test/java/";

	private final Log logger = LogFactory.getLog(AnnotationProcessorEmbeddedCompiler.class);

	private final AnnotationProcessorConfig config;

	/**
	 * Create an instance of the compiler with the specified configuration.
	 *
	 * @param config the configuration to use
	 */
	public AnnotationProcessorEmbeddedCompiler(AnnotationProcessorConfig config) {
		this.config = config;
	}

	/**
	 * Execute the annotation processor using the specified target and the specified
	 * list of classes.
	 * <p>Consider deleting the {@code baseDir} directory once
	 * the assertions have completed or use the {@link TemporaryFolder} rule.
	 *
	 * @param baseDir the base directory to use
	 * @param classes a list of classes to compile
	 * @return the output of the compilation
	 * @throws CompilationFailedException
	 * @see CompilationOutput#getOutputDir()
	 */
	public CompilationOutput process(File baseDir, Class<?>... classes) {
		final String[] locations = new String[classes.length];
		for (int i = 0; i < classes.length; i++) {
			locations[i] = createTestJavaSourcePath(classes[i]);
		}
		return process(baseDir, locations);
	}

	/**
	 * Execute the annotation processor using the specified target and the specified
	 * list of classes.
	 * <p>Consider deleting the {@code baseDir} directory once
	 * the assertions have completed or use the {@link TemporaryFolder} rule.
	 *
	 * @param baseDir the base directory to use
	 * @param classes a list of class locations to compile
	 * @return the output of the compilation
	 * @throws CompilationFailedException
	 * @see CompilationOutput#getOutputDir()
	 */
	public CompilationOutput process(File baseDir, String... classes) {
		try {
			return compile(config.isProcOnly(), baseDir, classes);
		}
		catch (IOException e) {
			throw new CompilationFailedException(baseDir, "Compilation failed with an unexpected exception", e);
		}
	}

	/**
	 * Run the compiler for the specified classes.
	 *
	 * @param procOnly <tt>true</tt> to only run annotation processing
	 * @param outputDir the output directory
	 * @param classes the path to the classes to compile
	 * @return the output of the compilation
	 * @throws IOException if compilation fails
	 * @throws CompilationFailedException if the return code is non null
	 * @see #getApOptions()
	 */
	protected CompilationOutput compile(boolean procOnly, File outputDir, String... classes) throws IOException {
		JavaCompiler compiler = new ExtendedJavaCompiler();
		List<String> options = new ArrayList<String>(classes.length + 3);
		options.add("-s"); // Generate source
		options.add(outputDir.getAbsolutePath());
		options.add("-d"); // Compilation output
		options.add(outputDir.getAbsolutePath());
		if (procOnly) {
			options.add("-proc:only");
		}
		else {
			// We need to pass a directory where the content will be generated
			options.add("-d");
			options.add(outputDir.getAbsolutePath());
		}
		options.add("-processor");
		options.add(config.getProcessorClass().getName());
		options.add("-sourcepath");
		options.add(JAVA_TEST_PREFIX);
		options.addAll(getApOptions());
		options.addAll(Arrays.asList(classes));

		return doCompile(compiler, outputDir, options);
	}

	/**
	 * Specify additional options to pass to the compiler.
	 *
	 * @return the additional options
	 */
	protected Collection<String> getApOptions() {
		return Collections.emptyList();
	}

	/**
	 * Create a path to a source file contained in the test directory. The
	 * return path is suitable for {@link #process(File, Class[])}
	 *
	 * @param source a class to use for compilation
	 * @return a path to use that class during compilation
	 * @see #process(File, Class[])
	 */
	public static String createTestJavaSourcePath(Class<?> source) {
		final File f = new File(toPath(JAVA_TEST_PREFIX, source));
		assertTrue("File [" + f.getAbsolutePath() + "] should exist, are you sure you are running the test " +
				"from the module directory?", f.exists());
		return f.getPath();
	}

	/**
	 * Create a path for the following source.
	 *
	 * @param prefix the prefix in which the source is to be found
	 * @param source the class
	 * @return a path for that class
	 */
	public static String toPath(String prefix, Class source) {
		return prefix + source.getName().replace('.', '/') + ".java";
	}


	/**
	 * Compile and catch error and output logs.
	 *
	 * @param compiler the compiler to use
	 * @param outputDir the output directory for the compilation
	 * @param options the options to use
	 * @return the output of the compilation
	 * @throws IOException if retrieving the logs lead to any issue
	 */
	private CompilationOutput doCompile(JavaCompiler compiler, File outputDir,
			List<String> options) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final ByteArrayOutputStream err = new ByteArrayOutputStream();

		try {
			int compilationResult = compiler.run(null, out, err, options.toArray(new String[options.size()]));

			final String outLog = out.toString();
			final String errLog = err.toString();

			if (logger.isTraceEnabled()) {
				logger.trace("Compilation log (out=" + outLog + ", err=" + errLog + ")");
			}

			final CompilationOutput compilationOutput =
					new CompilationOutput(compilationResult, outputDir, outLog, errLog);
			if (compilationResult != 0) {
				throw new CompilationFailedException(compilationOutput);
			}
			logger.info("Compilation is successful.");
			return compilationOutput;
		}
		finally {
			out.close();
			err.close();
		}
	}
}
