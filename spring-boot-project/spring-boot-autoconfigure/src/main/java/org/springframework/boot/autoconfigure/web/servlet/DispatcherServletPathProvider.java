/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import java.util.Set;

import org.springframework.web.servlet.DispatcherServlet;

/**
 * Interface that provides the paths that the {@link DispatcherServlet} in an application
 * context is mapped to.
 *
 * @author Madhura Bhave
 * @since 2.0.2
 */
@FunctionalInterface
public interface DispatcherServletPathProvider {

	Set<String> getServletPaths();

	/**
	 * Return a full path from the specified {@code pattern} using the specified
	 * {@code servletPath}.
	 * @param servletPath the servlet path
	 * @param pattern the pattern
	 * @return the path for the specified pattern
	 */
	default String getPath(String servletPath, String pattern) {
		String prefix = getServletPrefix(servletPath);
		if (!pattern.startsWith("/")) {
			pattern = "/" + pattern;
		}
		return prefix + pattern;
	}

	/**
	 * Return the servlet mapping of the specified {@code path}.
	 * @param path the path of a servlet
	 * @return the servlet mapping
	 * @see #getServletPaths()
	 */
	default String getServletMapping(String path) {
		if (path.equals("") || path.equals("/")) {
			return "/";
		}
		if (path.contains("*")) {
			return path;
		}
		if (path.endsWith("/")) {
			return path + "*";
		}
		return path + "/*";
	}

	/**
	 * Return the servlet prefix of the specified {@code path}, i.e. without trailing
	 * slash or `*`.
	 * @param path a path
	 * @return the servlet prefix for that path
	 */
	default String getServletPrefix(String path) {
		String result = path;
		int index = result.indexOf('*');
		if (index != -1) {
			result = result.substring(0, index);
		}
		if (result.endsWith("/")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

}
