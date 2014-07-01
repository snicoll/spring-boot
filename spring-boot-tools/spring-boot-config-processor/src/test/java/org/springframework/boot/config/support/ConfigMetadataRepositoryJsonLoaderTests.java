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

package org.springframework.boot.config.support;

import static org.junit.Assert.*;
import static org.springframework.boot.config.ConfigMetadataTestUtils.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import org.springframework.boot.config.ConfigMetadataRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 *
 * @author Stephane Nicoll
 */
public class ConfigMetadataRepositoryJsonLoaderTests {

	public static final String FOO_METADATA = "metadata/config-metadata-foo.json";

	public static final String FOO2_METADATA = "metadata/config-metadata-foo2.json";


	public static final String BAR_METADATA = "metadata/config-metadata-bar.json";

	private final ConfigMetadataRepositoryJsonLoader loader = new ConfigMetadataRepositoryJsonLoader();

	@Test
	public void loadFromClasspath() throws IOException {
		ConfigMetadataRepository repository = loader.loadFromClasspath();
		assertNotNull(repository); // Can't rely on classpath
	}

	@Test(expected = IllegalArgumentException.class)
	public void loadNullResource() throws IOException {
		loader.load(null);
	}

	@Test
	public void loadSingleResource() throws IOException {
		ConfigMetadataRepository repository = loader.load(create(new ClassPathResource(FOO_METADATA)));

		String fooGroupType = "org.springframework.boot.FooProperties";
		assertGroup(repository, "foo").name("foo")
				.localItems(3).localGroups(1).types(fooGroupType)
				.localItem("id").id("foo.id").valueType(String.class).groupTypes(fooGroupType);
		assertItem(repository, "foo.id").name("id").valueType(String.class)
				.noDescription().groupTypes(fooGroupType);
		assertItem(repository, "foo.sensitive").name("sensitive").valueType(Boolean.class)
				.description("A sensitive flag").groupTypes(fooGroupType);
		assertItem(repository, "foo.enabled").name("enabled").noValueType().noDescription();
		assertItem(repository, "foo.nested.value").valueType(String.class).noDescription();
		assertItem(repository, "rootFoo").noValueType().noDescription();
		assertRepo(repository)
				.itemsExist("rootFoo", "foo.id", "foo.sensitive", "foo.enabled", "foo.nested.value")
				.size(1, 5);
	}

	@Test
	public void loadMultipleResources() throws IOException {
		ConfigMetadataRepository repository = loader.load(create(
				new ClassPathResource(FOO_METADATA), new ClassPathResource(BAR_METADATA)));

		String fooGroupType = "org.springframework.boot.FooProperties";
		String barGroupType = "org.springframework.boot.BarProperties";

		assertGroup(repository, "foo").name("foo").localItems(3).localGroups(1).types(fooGroupType);
		assertGroup(repository, "bar").name("bar").localItems(3).localGroups(1).types(barGroupType);
		assertItem(repository, "foo.id").name("id").valueType(String.class)
				.noDescription().groupTypes(fooGroupType);
		assertItem(repository, "foo.sensitive").name("sensitive").valueType(Boolean.class)
				.description("A sensitive flag").groupTypes(fooGroupType);
		assertItem(repository, "foo.enabled").name("enabled").noValueType().noDescription();
		assertItem(repository, "foo.nested.value").valueType(String.class).noDescription().noGroupType();
		assertItem(repository, "rootFoo").noValueType().noDescription().noGroupType();


		assertItem(repository, "bar.id").valueType(String.class).noDescription().groupTypes(barGroupType);
		assertItem(repository, "bar.sensitive").valueType(Boolean.class).noDescription().groupTypes(barGroupType);
		assertItem(repository, "bar.enabled").valueType(Boolean.class).noDescription().groupTypes(barGroupType);
		assertItem(repository, "bar.nested.value").valueType(String.class).noDescription().noGroupType();
		assertItem(repository, "rootBar").noValueType().noDescription().noGroupType();

		assertRepo(repository).itemsExist("rootFoo", "rootBar",
				"foo.id", "foo.sensitive", "foo.enabled", "foo.nested.value",
				"bar.id", "bar.sensitive", "bar.enabled", "bar.nested.value")
				.size(2, 10);
	}

	@Test
	public void loadConflictingGroups() throws IOException {
		ConfigMetadataRepository repository = loader.load(create(
				new ClassPathResource(FOO_METADATA), new ClassPathResource(FOO_METADATA),
				new ClassPathResource(FOO_METADATA)));
		assertRepo(repository).itemsExist("rootFoo",
				"foo.id", "foo.sensitive", "foo.enabled", "foo.nested.value")
				.size(1, 5)
				.localGroup("foo").id("foo").localItems(3).localGroups(1)
				.types("org.springframework.boot.FooProperties");
	}

	@Test
	public void loadMergingGroups() throws IOException {
		ConfigMetadataRepository repository = loader.load(create(
				new ClassPathResource(FOO_METADATA), new ClassPathResource(FOO2_METADATA)));
		String fooGroupType = "org.springframework.boot.FooProperties";
		String foo2GroupType = "org.springframework.boot.Foo2Properties";

		assertGroup(repository, "foo").id("foo").localItems(4).localGroups(1).types(fooGroupType, foo2GroupType);
		assertItem(repository, "foo.id").valueType(String.class).noDescription().groupTypes(fooGroupType);
		assertItem(repository, "foo.sensitive").valueType(Boolean.class)
				.description("A sensitive flag").groupTypes(fooGroupType, foo2GroupType);
		assertItem(repository, "foo.enabled").noValueType().noDescription().noGroupType();
		assertItem(repository, "foo.enabled2").valueType(Boolean.class).noDescription().groupTypes(foo2GroupType);
		assertItem(repository, "foo.nested.value").valueType(String.class).noDescription().noGroupType();
		assertItem(repository, "foo.nested.value2").valueType(String.class).noDescription().noGroupType();
		assertItem(repository, "rootFoo").noValueType().noDescription().noGroupType();

		assertRepo(repository).itemsExist("rootFoo",
				"foo.id", "foo.sensitive", "foo.enabled",
				"foo.enabled2", "foo.nested.value", "foo.nested.value2")
				.size(1, 7);
	}


	private Collection<Resource> create(ClassPathResource... resources) {
		List<Resource> content = new ArrayList<Resource>();
		Collections.addAll(content, resources);
		return content;

	}
}
