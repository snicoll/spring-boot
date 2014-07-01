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
import org.springframework.boot.config.sample.method.EmptyTypeMethodConfig;
import org.springframework.boot.config.sample.method.InvalidMethodConfig;
import org.springframework.boot.config.sample.method.MethodAndClassConfig;
import org.springframework.boot.config.sample.method.SimpleMethodConfig;

/**
 *
 * @author Stephane Nicoll
 */
public class MethodProcessorTests extends AbstractProcessorTests {

	@Test
	public void processSimpleMethodConfig() {
		ConfigMetadataRepository repository = process(SimpleMethodConfig.class);
		assertItem(repository, "foo.name").valueType(String.class).groupTypes(SimpleMethodConfig.Foo.class);
		assertItem(repository, "foo.flag").valueType(Boolean.class).groupTypes(SimpleMethodConfig.Foo.class);
		assertRepo(repository).size(1, 2)
				.group("foo").name("foo").types( SimpleMethodConfig.Foo.class).localItems(2).localGroups(0);
	}

	@Test
	public void processInvalidMethodConfig() {
		ConfigMetadataRepository repository = process(InvalidMethodConfig.class);
		assertItem(repository, "something.name").valueType(String.class).groupTypes(InvalidMethodConfig.class);
		assertRepo(repository).itemsDoNotExist("invalid.name").size(1, 1);
	}

	@Test
	public void processMethodAndClassConfig() {
		ConfigMetadataRepository repository = process(MethodAndClassConfig.class);
		assertItem(repository, "conflict.name").valueType(String.class).groupTypes(MethodAndClassConfig.Foo.class);
		assertItem(repository, "conflict.flag").valueType(Boolean.class).groupTypes(MethodAndClassConfig.Foo.class);
		assertItem(repository, "conflict.value").valueType(String.class).groupTypes(MethodAndClassConfig.class);
		assertRepo(repository).size(1, 3);
	}

	@Test
	public void processEmptyTypeMethodConfig() {
		ConfigMetadataRepository repository = process(EmptyTypeMethodConfig.class);
		assertRepo(repository).size(1, 1).group("something")
				.localGroups(0)
				.localItems(1)
				.types(EmptyTypeMethodConfig.class); // Foo class should not be registered (no item)
	}
}
