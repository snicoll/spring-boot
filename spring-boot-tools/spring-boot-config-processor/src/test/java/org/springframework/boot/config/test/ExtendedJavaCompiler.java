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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * An extension of the {@link JavaCompiler} to provide more options.
 *
 * @author Stephane Nicoll
 */
public class ExtendedJavaCompiler implements JavaCompiler {

	private final JavaCompiler compiler;

	private final String classPath;

	public ExtendedJavaCompiler() {
		this(ToolProvider.getSystemJavaCompiler(), Thread.currentThread().getContextClassLoader());
	}

	public ExtendedJavaCompiler(JavaCompiler compiler, ClassLoader classLoader) {
		this.compiler = compiler;
		if (classLoader instanceof URLClassLoader) {
			classPath = getClassPath((URLClassLoader) classLoader);
		}
		else {
			throw new IllegalArgumentException("Unsupported ClassLoader " + classLoader);
		}
	}

	@Override
	public Set<SourceVersion> getSourceVersions() {
		return compiler.getSourceVersions();
	}

	@Override
	public StandardJavaFileManager getStandardFileManager(
			DiagnosticListener<? super JavaFileObject> diagnosticListener,
			Locale locale, Charset charset) {
		return compiler.getStandardFileManager(diagnosticListener, locale, charset);
	}

	@Override
	public JavaCompiler.CompilationTask getTask(Writer out, JavaFileManager fileManager,
			DiagnosticListener<? super JavaFileObject> diagnosticListener,
			Iterable<String> options, Iterable<String> classes,
			Iterable<? extends JavaFileObject> compilationUnits) {
		return compiler.getTask(out, fileManager, diagnosticListener, options, classes, compilationUnits);
	}

	@Override
	public int isSupportedOption(String option) {
		return compiler.isSupportedOption(option);
	}

	@Override
	public int run(InputStream in, OutputStream out, OutputStream err, String... arguments) {
		for (String a : arguments) {
			if (a.equals("-classpath")) {
				return compiler.run(in, out, err, arguments);
			}
		}

		// no classpath given
		List<String> args = new ArrayList<String>(arguments.length + 2);
		args.add("-classpath");
		args.add(classPath);
		args.addAll(Arrays.asList(arguments));
		return compiler.run(in, out, err, args.toArray(new String[args.size()]));
	}

	public static String getClassPath(URLClassLoader classLoader) {
		try {
			StringBuilder path = new StringBuilder();
			for (URL url : classLoader.getURLs()) {
				if (path.length() > 0) {
					path.append(File.pathSeparator);
				}
				String decodedPath = URLDecoder.decode(url.getPath(), "UTF-8");
				path.append(new File(decodedPath).getAbsolutePath());
			}
			return path.toString();
		}
		catch (UnsupportedEncodingException e) {
			throw new IllegalStateException("could not build classpath", e);
		}
	}
}

