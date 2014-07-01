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
public class ConfigMetadataItemTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	@Test
	public void mergeWithDifferentId() {
		ConfigMetadataItem item = new ConfigMetadataItem("foo");
		item.setId("foo");

		ConfigMetadataItem item2 = new ConfigMetadataItem("foo");
		item.setId("foo2"); // different id

		thrown.expect(IllegalArgumentException.class);
		item.merge(item2);
	}

	@Test
	public void mergeMissingDescription() {
		ConfigMetadataItem item = createItem("foo", String.class, null);
		ConfigMetadataItem item2 = createItem("foo", null, "Foo is nice");
		item.merge(item2);

		assertItem(item).id("foo").name("foo").valueType(String.class).description("Foo is nice").noGroupType();
	}

	@Test
	public void mergeMissingType() {
		ConfigMetadataItem item = createItem("foo", null, "Foo is nice");
		ConfigMetadataItem item2 = createItem("foo", String.class, "Foo is nice");
		item.merge(item2);

		assertItem(item).id("foo").name("foo").valueType(String.class).description("Foo is nice").noGroupType();
	}

	@Test
	public void mergeNewGroupType() {
		ConfigMetadataItem item = createItem("foo", null, "Foo is nice");
		ConfigMetadataItem item2 = createItem("foo", String.class, "Foo is nice", "org.foo.Type2");
		item.merge(item2);

		assertItem(item).id("foo").name("foo").valueType(String.class)
				.description("Foo is nice").groupTypes("org.foo.Type2");
	}

	@Test
	public void mergeExistingGroupType() {
		ConfigMetadataItem item = createItem("foo", null, "Foo is nice", "org.foo.Type1");
		ConfigMetadataItem item2 = createItem("foo", String.class, "Foo is nice",
				"org.foo.Type2", "org.foo.Type1");
		item.merge(item2);

		assertItem(item).id("foo").name("foo").valueType(String.class)
				.description("Foo is nice").groupTypes("org.foo.Type1", "org.foo.Type2");
	}

	@Test
	public void computeTagLineNull() {
		ConfigMetadataItem item = createItem("foo", String.class, null);
		assertNull(item.getTagLine());
	}

	@Test
	public void computeTagLineDot() {
		ConfigMetadataItem item = createItem("foo",
				String.class, "This is the tag line. And then a lengthy description \n that is multi-lines");
		assertEquals("This is the tag line", item.getTagLine());
	}

	@Test
	public void computeTagLineNewline() {
		ConfigMetadataItem item = createItem("foo",
				String.class, "This is the tag line\n And then a lengthy description after");
		assertEquals("This is the tag line", item.getTagLine());
	}

	@Test
	public void computeTagLineNewlineAndSpaces() {
		ConfigMetadataItem item = createItem("foo",
				String.class, "This is the tag line   \n And then a lengthy description after");
		assertEquals("This is the tag line", item.getTagLine());
	}

	@Test
	public void computeTagLineNewlineWithDot() {
		ConfigMetadataItem item = createItem("foo",
				String.class, "This is the tag line.\n And then a lengthy description after");
		assertEquals("This is the tag line", item.getTagLine());
	}

}
