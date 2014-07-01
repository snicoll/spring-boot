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
import static org.springframework.boot.config.ConfigMetadataTestUtils.*;

import java.io.File;

import org.junit.Test;

import org.springframework.boot.config.ConfigMetadataRepository;
import org.springframework.boot.config.sample.simple.EmptyConfig;
import org.springframework.boot.config.sample.simple.HierarchicalConfig;
import org.springframework.boot.config.sample.simple.SimpleCollectionConfig;
import org.springframework.boot.config.sample.simple.SimpleConfig;
import org.springframework.boot.config.sample.simple.SimplePrefixValueConfig;
import org.springframework.boot.config.sample.simple.SimpleTypeConfig;
import org.springframework.boot.config.test.CompilationOutput;

/**
 *
 * @author Stephane Nicoll
 */
public class SimpleProcessorTests extends AbstractProcessorTests {

	@Test
	public void parseEmptyConfig() {
		CompilationOutput compile = compile(EmptyConfig.class);
		File metadataFile = getConfigMetadataFile(compile.getOutputDir());
		assertFalse("No config metadata file should have been generated when " +
				"no metadata is discovered", metadataFile.exists());
	}

	@Test
	public void parseSimpleConfig() {
		ConfigMetadataRepository repository = process(SimpleConfig.class);
		assertItem(repository, "simple.name").valueType(String.class)
				.description("The name of this simple config.").groupTypes(SimpleConfig.class);
		assertItem(repository, "simple.flag").valueType(Boolean.class)
				.description("A simple flag.").groupTypes(SimpleConfig.class);
		assertRepo(repository).itemsDoNotExist("simple.comparator").size(1, 2)
				.group("simple").types(SimpleConfig.class).localItems(2).localGroups(0);
	}

	@Test
	public void parsePrefixValueConfig() {
		ConfigMetadataRepository repository = process(SimplePrefixValueConfig.class);
		assertItem(repository, "simple.name").valueType(String.class)
				.groupTypes(SimplePrefixValueConfig.class);
		assertRepo(repository).size(1, 1).group("simple").types(SimplePrefixValueConfig.class).localItems(1);
	}

	@Test
	public void parseBasicTypes() {
		ConfigMetadataRepository repository = process(SimpleTypeConfig.class);
		assertItem(repository, "simple.type.myString").valueType(String.class);
		assertItem(repository, "simple.type.myByte").valueType(Byte.class);
		assertItem(repository, "simple.type.myPrimitiveByte").valueType(Byte.class);
		assertItem(repository, "simple.type.myChar").valueType(Character.class);
		assertItem(repository, "simple.type.myPrimitiveChar").valueType(Character.class);
		assertItem(repository, "simple.type.myBoolean").valueType(Boolean.class);
		assertItem(repository, "simple.type.myPrimitiveBoolean").valueType(Boolean.class);
		assertItem(repository, "simple.type.myShort").valueType(Short.class);
		assertItem(repository, "simple.type.myPrimitiveShort").valueType(Short.class);
		assertItem(repository, "simple.type.myInteger").valueType(Integer.class);
		assertItem(repository, "simple.type.myPrimitiveInteger").valueType(Integer.class);
		assertItem(repository, "simple.type.myLong").valueType(Long.class);
		assertItem(repository, "simple.type.myPrimitiveLong").valueType(Long.class);
		assertItem(repository, "simple.type.myDouble").valueType(Double.class);
		assertItem(repository, "simple.type.myPrimitiveDouble").valueType(Double.class);
		assertItem(repository, "simple.type.myFloat").valueType(Float.class);
		assertItem(repository, "simple.type.myPrimitiveFloat").valueType(Float.class);
		assertRepo(repository).size(1, 17);
	}

	@Test
	public void parseHierarchicalConfig() {
		ConfigMetadataRepository repository = process(HierarchicalConfig.class);
		assertItem(repository, "hierarchical.first").valueType(String.class).groupTypes(HierarchicalConfig.class);
		assertItem(repository, "hierarchical.second").valueType(String.class).groupTypes(HierarchicalConfig.class);
		assertItem(repository, "hierarchical.third").valueType(String.class).groupTypes(HierarchicalConfig.class);
		// These are declared as direct items (hierarchy is flat)
		assertRepo(repository).size(1, 3)
				.group("hierarchical").types(HierarchicalConfig.class).localItems(3).localGroups(0);
	}

	@Test
	public void parseCollectionConfig() {
		ConfigMetadataRepository repository = process(SimpleCollectionConfig.class);
		// getter and setter
		assertItem(repository, "collection.integersToNames").valueType("java.util.Map<java.lang.Integer,java.lang.String>");
		assertItem(repository, "collection.longs").valueType("java.util.Collection<java.lang.Long>");
		assertItem(repository, "collection.floats").valueType("java.util.Collection<java.lang.Float>");
		// getter only
		assertItem(repository, "collection.namesToIntegers").valueType("java.util.Map<java.lang.String,java.lang.Integer>");
		assertItem(repository, "collection.bytes").valueType("java.util.Collection<java.lang.Byte>");
		assertItem(repository, "collection.doubles").valueType("java.util.Collection<java.lang.Double>");
		assertRepo(repository).size(1, 6)
				.group("collection").types(SimpleCollectionConfig.class).localItems(6).localGroups(0);
	}
}
