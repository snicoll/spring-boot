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

package org.springframework.boot.config.processor.mapper;

import static org.springframework.boot.config.ConfigMetadataTestUtils.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import org.springframework.boot.config.ConfigMetadataGroup;
import org.springframework.boot.config.ConfigMetadataItem;
import org.springframework.boot.config.ConfigMetadataRepository;
import org.springframework.boot.config.SimpleConfigMetadataRepository;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

/**
 *
 * @author Stephane Nicoll
 */
public class ConfigMetadataRepositoryJsonMapperTests {

	private final ConfigMetadataRepositoryJsonMapper mapper = new ConfigMetadataRepositoryJsonMapper();

	private final SimpleConfigMetadataRepository repository = new SimpleConfigMetadataRepository();


	@Test
	public void processSimpleGroup() {
		ConfigMetadataGroup group = createGroup("server", "port", "name", "address");
		repository.registerRootGroup(group);

		ConfigMetadataRepository result = writeAndRead(repository);
		assertRepo(result).size(1, 3)
				.itemsExist("server.port", "server.name", "server.address");
	}

	@Test
	public void processNestedGroup() {
		ConfigMetadataGroup group = createGroup("server", "port", "name", "address");
		repository.registerRootGroup(group);
		ConfigMetadataGroup tomcat = group.registerGroup("tomcat");
		registerItems(tomcat, "port", "timeout");

		ConfigMetadataRepository result = writeAndRead(repository);
		assertRepo(result)
				.itemsExist("server.port", "server.name", "server.address",
						"server.tomcat.port", "server.tomcat.timeout")
				.size(1, 5);
		assertGroup(result, "server").size(1, 5).localItems(3)
				.localGroup("tomcat").size(0, 2);
	}

	@Test
	public void noGroup() {
		ConfigMetadataGroup group = createGroup(null, "fooBar", "another");
		repository.registerRootGroup(group);

		ConfigMetadataRepository result = writeAndRead(repository);
		assertRepo(result).itemsExist("fooBar", "another").size(0, 2);
	}

	@Test
	public void severalGroupTypes() {
		ConfigMetadataGroup group = createGroup("foo");
		group.addType("org.foo.Type1");
		group.addType("org.foo.Type2");
		ConfigMetadataItem one = createItem("one", String.class, "The one", "org.foo.Type1");
		one.setDefaultValue("FooBar");
		group.registerItem(one);

		group.registerItem(createItem("two", Integer.class, "The two", "org.foo.Type2"));
		group.registerItem(createItem("three", null, null, "org.foo.Type1", "org.foo.Type2"));
		group.registerItem(createItem("four", null, null));
		repository.registerRootGroup(group);

		ConfigMetadataRepository result = writeAndRead(repository);
		assertRepo(result).size(1, 4)
				.group("foo").name("foo").types("org.foo.Type1", "org.foo.Type2").localItems(4).localGroups(0);
		assertItem(result, "foo.one").name("one").valueType(String.class)
				.defaultValue("FooBar").description("The one").groupTypes("org.foo.Type1");
		assertItem(result, "foo.two").name("two").valueType(Integer.class)
				.noDefaultValue().description("The two").groupTypes("org.foo.Type2");
		assertItem(result, "foo.three").name("three").noValueType().noDescription()
				.noDefaultValue().groupTypes("org.foo.Type1", "org.foo.Type2");
		assertItem(result, "foo.four").name("four").noValueType().noDescription()
				.noDefaultValue().noGroupType();
	}

	@Test
	public void readInvalidIndex() throws IOException {
		ConfigMetadataRepository repository = read(new ClassPathResource("metadata/invalid-metadata-wrong-index.json"));
		assertRepo(repository).size(1, 1).group("foo")
				.types("org.springframework.boot.FooProperties")
				.localItem("id").valueType(String.class).noGroupType().noDescription();
	}


	private ConfigMetadataRepository writeAndRead(ConfigMetadataRepository repository) {
		String json = write(repository);
		return read(json);
	}

	private ConfigMetadataRepository read(Resource resource) throws IOException {
		InputStream in = resource.getInputStream();
		try {
			String json = StreamUtils.copyToString(in, ConfigMetadataRepositoryJsonMapper.UTF_8);
			return read(json);
		}
		finally {
			in.close();
		}
	}

	private ConfigMetadataRepository read(String json) {

		try {
			ByteArrayInputStream in = new ByteArrayInputStream(
					json.getBytes(ConfigMetadataRepositoryJsonMapper.UTF_8));
			ConfigMetadataRepository repository = mapper.readRepository(in);
			in.close();
			return repository;
		}
		catch (IOException e) {
			throw new IllegalStateException("Should not happen", e);
		}
	}

	private String write(ConfigMetadataRepository repository) {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			mapper.writeRepository(repository, outputStream);
			outputStream.close();
			return new String(outputStream.toByteArray());
		}
		catch (IOException e) {
			throw new IllegalStateException("Should not happen", e);
		}
	}
}
