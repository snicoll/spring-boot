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

import java.util.function.UnaryOperator;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Provide information about a fat jar structure that is meant to be extracted.
 *
 * @author Stephane Nicoll
 */
interface JarStructure {

	/**
	 * Resolve the specified {@link ZipEntry}, return {@code null} if the entry should not
	 * be handled.
	 * @param entry the entry to handle
	 * @return the resolved {@link Entry}
	 */
	Entry resolve(ZipEntry entry);

	/**
	 * Create the {@link Manifest} for the {@code run-jar} jar, applying the specified
	 * operator on each classpath entry.
	 * @param libEntry the operator to apply on each classpath entry
	 * @return the manifest to use for the {@code run-jar} jar
	 */
	Manifest createRunJarManifest(UnaryOperator<String> libEntry);

	/**
	 * Create the {@link Manifest} for the jar that is created with classes and resources.
	 * @return the manifest to use for the {@code application} jar
	 */
	Manifest createApplicationJarManifest();

	/**
	 * An entry to handle in the exploded structure.
	 *
	 * @param originalLocation the original location
	 * @param location the relative location
	 * @param library whether the entry refers to a library or a classpath resource
	 */
	record Entry(String originalLocation, String location, boolean library) {
	}

}
