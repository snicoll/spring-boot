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
import org.springframework.boot.config.sample.condition.ConditionAndPropertiesConfig;
import org.springframework.boot.config.sample.condition.ConditionAndPropertiesDifferentStyleConfig;
import org.springframework.boot.config.sample.condition.ConditionAndPropertiesOverlapConfig;
import org.springframework.boot.config.sample.condition.SimpleConditionConfig;

/**
 *
 * @author Stephane Nicoll
 */
public class ConditionProcessorTests extends AbstractProcessorTests {

	@Test
	public void parseSimpleConditionConfig() {
		ConfigMetadataRepository repository = process(SimpleConditionConfig.class);
		assertItem(repository, "foo.enabled").noValueType().noDescription().noGroupType();
		assertItem(repository, "foo.flag").noValueType().noDescription().noGroupType();
		assertItem(repository, "foo.ignore").noValueType().noDescription().noGroupType();
		assertRepo(repository)
				.size(1, 3)
				.localGroup("foo").id("foo").noType().localItems(3).localGroups(0);
	}

	@Test
	public void parseConditionAndPropertiesConfig() {
		ConfigMetadataRepository repository = process(ConditionAndPropertiesConfig.class);
		assertItem(repository, "foo.name").valueType(String.class).noDescription()
				.groupTypes(ConditionAndPropertiesConfig.class);
		assertItem(repository, "foo.bar").noValueType().noDescription().noGroupType();
		assertRepo(repository).size(1, 2)
				.localGroup("foo").id("foo").types(ConditionAndPropertiesConfig.class).localItems(2).localGroups(0);
	}

	@Test
	public void parseConditionAndPropertiesOverlapConfig() {
		ConfigMetadataRepository repository = process(ConditionAndPropertiesOverlapConfig.class);
		assertItem(repository, "foo.bar").valueType(String.class.getName())
				.description("Some doc.").groupTypes(ConditionAndPropertiesOverlapConfig.class);
		assertItem(repository, "foo.flag").noValueType().noDescription().noGroupType();
		assertRepo(repository).size(1, 2)
				.localGroup("foo").id("foo").types(ConditionAndPropertiesOverlapConfig.class)
				.localItems(2).localGroups(0);
	}

	@Test
	public void parseConditionAndPropertiesDifferentStyleConfig() {
		ConfigMetadataRepository repository = process(ConditionAndPropertiesDifferentStyleConfig.class);
		assertItem(repository, "longGroup.fooProperty").valueType(String.class.getName())
				.description("foo doc.").groupTypes(ConditionAndPropertiesDifferentStyleConfig.class);
		assertItem(repository, "longGroup.barProperty").valueType(String.class.getName())
				.description("bar doc.").groupTypes(ConditionAndPropertiesDifferentStyleConfig.class);
		assertRepo(repository)
				.itemsDoNotExist("long-group.foo-property", "longGroup.bar-property")
				.size(1, 2)
				.localGroup("longGroup").id("longGroup").types(ConditionAndPropertiesDifferentStyleConfig.class)
				.localItems(2).localGroups(0);
	}
}
