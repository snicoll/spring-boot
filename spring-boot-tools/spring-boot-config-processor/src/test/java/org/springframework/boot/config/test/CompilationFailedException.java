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

/**
 * Thrown when the compiler failed.
 *
 * @author Stephane Nicoll
 */
@SuppressWarnings("serial")
public class CompilationFailedException extends RuntimeException {

	private final CompilationOutput compilationOutput;

	public CompilationFailedException(CompilationOutput compilationOutput) {
		this.compilationOutput = compilationOutput;
	}

	public CompilationFailedException(File outputDir, String message, Throwable cause) {
		super(message, cause);
		this.compilationOutput = new CompilationOutput(-100, outputDir, "", "");
	}

	/**
	 * Return the compilation output.
	 */
	public CompilationOutput getCompilationOutput() {
		return compilationOutput;
	}
}
