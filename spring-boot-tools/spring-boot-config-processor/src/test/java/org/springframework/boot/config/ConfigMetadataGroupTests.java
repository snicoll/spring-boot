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

package org.springframework.boot.config;

import static org.junit.Assert.*;
import static org.springframework.boot.config.ConfigMetadataTestUtils.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 *
 * @author Stephane Nicoll
 */
public class ConfigMetadataGroupTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void registerWithUnknownGroupType() {
		ConfigMetadataGroup group = createGroup("foo", "org.foo.Type1");
		ConfigMetadataItem item = createItem("bar", String.class, "Some doc",
				"org.foo.Type1", "org.foo.Type2");

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("org.foo.Type2");
		group.registerItem(item);
	}

	@Test
	public void mergeWithDifferentId() {
		ConfigMetadataGroup group = ConfigMetadataGroup.root("foo");

		thrown.expect(IllegalArgumentException.class);
		group.merge(ConfigMetadataGroup.root("bar"));
	}

	@Test
	public void mergeMissingType() {
		ConfigMetadataGroup group = ConfigMetadataGroup.root("foo");
		ConfigMetadataGroup group2 = createGroup("foo", "org.foo.Foo");

		group.merge(group2);
		assertGroup(group).id("foo").name("foo")
				.localItems(0).localGroups(0).types("org.foo.Foo");
	}

	@Test
	public void mergeDirectItem() {
		ConfigMetadataGroup group = ConfigMetadataGroup.root("foo");
		group.registerItem(createItem("item", null, "My description"));
		ConfigMetadataGroup group2 = createGroup("foo", "org.foo.Foo");
		group2.registerItem(createItem("item", String.class, null));

		group.merge(group2);
		assertGroup(group).id("foo").name("foo").localItems(1).localGroups(0)
				.types("org.foo.Foo")
				.item("foo.item").valueType(String.class).description("My description");
	}

	@Test
	public void mergeNestedITem() {
		ConfigMetadataGroup group = ConfigMetadataGroup.root("foo");
		ConfigMetadataGroup nestedGroup = group.registerGroup("nested");
		nestedGroup.registerItem(createItem("item", null, "My description"));
		ConfigMetadataGroup group2 = createGroup("foo", "org.foo.Foo");
		ConfigMetadataGroup nestedGroup2 = group2.registerGroup("nested");
		nestedGroup2.registerItem(createItem("item", String.class, null));

		group.merge(group2);
		assertGroup(group).id("foo").name("foo").localItems(0).localGroups(1)
				.types("org.foo.Foo")
				.item("foo.nested.item").valueType(String.class).description("My description");
	}

	@Test
	public void mergeItemWithDifferentTypes() {
		ConfigMetadataGroup group = createGroup("foo", "org.foo.Type1");
		group.registerItem(createItem("item", null, "My description", "org.foo.Type1"));
		ConfigMetadataGroup group2 = createGroup("foo", "org.foo.Type2");
		group2.registerItem(createItem("item", String.class, null, "org.foo.Type2"));

		group.merge(group2); // item has full description and is available on both types

		assertGroup(group).id("foo").name("foo").localItems(1).localGroups(0)
				.types("org.foo.Type1", "org.foo.Type2")
				.item("foo.item").valueType(String.class).description("My description")
				.groupTypes("org.foo.Type1", "org.foo.Type2");
	}

	@Test
	public void registerItemTwice() {
		ConfigMetadataGroup foo = ConfigMetadataGroup.root("foo");
		ConfigMetadataItem item = new ConfigMetadataItem("bar");
		foo.registerItem(item);
		foo.registerItem(new ConfigMetadataItem("bar"));
		assertSame(item, foo.getItems().get("bar"));
	}

	private ConfigMetadataGroup createGroup(String name, String... types) {
		ConfigMetadataGroup group = ConfigMetadataGroup.root(name);
		for (String type : types) {
			group.addType(type);
		}
		return group;
	}
}
