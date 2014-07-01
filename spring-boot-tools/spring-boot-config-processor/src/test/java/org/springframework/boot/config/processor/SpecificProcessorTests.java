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

import static org.springframework.boot.config.ConfigMetadataTestUtils.*;

import org.junit.Test;

import org.springframework.boot.config.ConfigMetadataRepository;
import org.springframework.boot.config.ConfigMetadataRepositoryAssert;
import org.springframework.boot.config.sample.specific.CustomListTypeConfig;
import org.springframework.boot.config.sample.specific.CustomMapTypeConfig;
import org.springframework.boot.config.sample.specific.InnerClassAnnotatedGetterConfig;
import org.springframework.boot.config.sample.specific.InnerClassConfig;
import org.springframework.boot.config.sample.specific.InnerClassRootConfig;

/**
 *
 * @author Stephane Nicoll
 */
public class SpecificProcessorTests extends AbstractProcessorTests {

	@Test
	public void parserInnerClassRootConfig() {
		ConfigMetadataRepository repository = process(InnerClassRootConfig.class);
		assertRepo(repository).itemsExist("config.name").size(1, 1);
	}

	@Test
	public void parserInnerClassConfig() {
		ConfigMetadataRepository repository = process(InnerClassConfig.class);
		ConfigMetadataRepositoryAssert repoAssert = assertRepo(repository)
				.itemsExist("config.first.name", "config.first.bar.name",
						"config.second.name", "config.second.bar.name");
		repoAssert.group("config").types(InnerClassConfig.class).localGroups(2).localItems(0);
		repoAssert.group("config.first").types(InnerClassConfig.Foo.class).localGroups(1).localItems(1);
		repoAssert.group("config.second").types(InnerClassConfig.Foo.class).localGroups(1).localItems(1);
		repoAssert.group("config.second.bar").types(InnerClassConfig.Foo.Bar.class).localGroups(0).localItems(1);
		repoAssert.size(1, 4);
	}

	@Test // Demonstrates how the type is standardized so that tools can rely on it
	public void parseCustomMapConfig() {
		ConfigMetadataRepository repository = process(CustomMapTypeConfig.class);
		assertRepo(repository)
				.size(1, 1)
				.item("custom.map.content").valueType("java.util.Map<java.lang.String,java.lang.Integer>");
	}

	@Test // Demonstrates how the type is standardized so that tools can rely on it
	public void parseCustomListConfig() {
		ConfigMetadataRepository repository = process(CustomListTypeConfig.class);
		assertRepo(repository)
				.size(1, 1)
				.item("custom.list.content").valueType("java.util.Collection<java.lang.String>");
	}

	@Test
	public void parseInnerClassAnnotatedGetterConfig() {
		ConfigMetadataRepository repository = process(InnerClassAnnotatedGetterConfig.class);
		assertRepo(repository)
				.itemsExist("specific.value", "foo.name")
				.itemsDoNotExist("specific.foo")
				.size(2, 2);
		assertGroup(repository, "specific").types(InnerClassAnnotatedGetterConfig.class).localItems(1).localGroups(0);
		assertGroup(repository, "foo").types(InnerClassAnnotatedGetterConfig.Foo.class).localItems(1).localGroups(0);
	}
}
