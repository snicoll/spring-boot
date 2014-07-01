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


import java.io.File;

import org.springframework.util.Assert;

/**
 * Gather the output of the compilation.
 *
 * @author Stephane Nicoll
 */
public class CompilationOutput {

	private final int returnCode;

	private final File outputDir;

	private final String errorLog;

	private final String outputLog;

	CompilationOutput(int returnCode, File outputDir, String outputLog, String errorLog) {
		this.returnCode = returnCode;
		Assert.notNull(outputDir, "output directory could not be null");
		Assert.notNull(outputLog, "output log could not be null");
		Assert.notNull(errorLog, "error log could not be null");

		this.outputDir = outputDir;
		this.errorLog = errorLog;
		this.outputLog = outputLog;
	}

	/**
	 * Return the error code of a compilation. A zero return code
	 * usually means that everything went fine.
	 */
	public int getReturnCode() {
		return returnCode;
	}

	/**
	 * Return the output directory of the compilation.
	 * <p>For an annotation processor, the generated code is to be
	 * found there.
	 */
	public File getOutputDir() {
		return outputDir;
	}

	/**
	 * Return the output log (i.e. information, note).
	 * @see System#out
	 */
	public String getOutputLog() {
		return outputLog;
	}

	/**
	 * Return the error log.
	 * @see System#err
	 */
	public String getErrorLog() {
		return errorLog;
	}

}
