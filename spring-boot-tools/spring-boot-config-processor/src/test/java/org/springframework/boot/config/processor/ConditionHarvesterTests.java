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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.config.SimpleConfigMetadataRepository;

/**
 *
 * @author Stephane Nicoll
 */
public class ConditionHarvesterTests {

	private final SimpleConfigMetadataRepository repository = new SimpleConfigMetadataRepository();

	private final ConditionHarvester harvester = new ConditionHarvester(repository);

	@Test
	public void appendItemToExistingGroup() {
		doAppendFooEnabledToExistingGroup("foo", "enabled");
	}

	@Test
	public void appendItemWithDottedPrefixToExistingGroup() {
		doAppendFooEnabledToExistingGroup("foo.", "enabled");
	}

	@Test
	public void appendItemNoPrefixToExistingGroup() {
		doAppendFooEnabledToExistingGroup(null, "foo.enabled");
	}

	@Test
	public void appendItemEmptyPrefixToExistingGroup() {
		doAppendFooEnabledToExistingGroup(" ", "foo.enabled");
	}

	@Test
	public void appendItemWithGarbageValue() {
		doAppendFooEnabledToExistingGroup("foo", "enabled", 1, Boolean.TRUE);
	}

	private void doAppendFooEnabledToExistingGroup(String prefix, Object... values) {
		repository.registerRootGroup(createGroup("foo", "first", "second"));
		harvester.harvest(createAnnotationAttributes(prefix, values));
		assertRepo(repository).itemsExist("foo.first", "foo.second", "foo.enabled")
				.size(1,3).group("foo").localItems(3).localGroups(0);
	}

	@Test
	public void processRootItem() {
		harvester.harvest(createAnnotationAttributes(null, "myPropertyInRoot"));
		assertRepo(repository).size(0, 1).item("myPropertyInRoot").noValueType().noDescription().noGroupType();
	}

	@Test
	public void processItemInNewGroup() {
		repository.registerRootGroup(createGroup("foo", "first", "second"));
		harvester.harvest(createAnnotationAttributes("bar", "enabled"));
		assertRepo(repository).itemsExist("foo.first", "foo.second", "bar.enabled")
				.size(2, 3)
				.localGroup("bar").id("bar").localItems(1).localGroups(0);
	}

	@Test
	public void processItemWithNoProperty() {
		repository.registerRootGroup(createGroup("foo", "first", "second"));
		harvester.harvest(createAnnotationAttributes("bar", "       "));
		assertRepo(repository).itemsExist("foo.first", "foo.second")
				.size(1, 2)
				.localGroup("foo").id("foo").localItems(2).localGroups(0);
	}

	@Test
	public void processSeveralValues() {
		repository.registerRootGroup(createGroup("foo", "first", "second"));
		harvester.harvest(createAnnotationAttributes("foo", "enabled", "flag", ""));
		assertRepo(repository)
				.itemsExist("foo.first", "foo.second", "foo.enabled", "foo.flag")
				.size(1, 4)
				.localGroup("foo").id("foo").localItems(4).localGroups(0);
	}

	private Map<String, Object> createAnnotationAttributes(String prefix, Object... values) {
		Map<String, Object> content = new HashMap<String, Object>();
		if (prefix != null) {
			content.put(ConditionHarvester.PREFIX_ATTRIBUTE, prefix);
		}
		if (values != null) {
			if (values.length == 1) {
				content.put(ConditionHarvester.VALUE_ATTRIBUTE, values[0]);
			}
			else {
				content.put(ConditionHarvester.VALUE_ATTRIBUTE, Arrays.asList(values));
			}

		}
		return content;
	}
}
