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

import static org.springframework.boot.config.ConfigMetadataTestUtils.*;

import org.junit.Test;

/**
 *
 * @author Stephane Nicoll
 */
public class SimpleConfigMetadataRepositoryTests {

	private final SimpleConfigMetadataRepository repository = new SimpleConfigMetadataRepository();

	@Test
	public void simpleGroup() {
		ConfigMetadataGroup group = createGroup("server", "port", "name", "address");
		repository.registerRootGroup(group);
		assertRepo(repository)
				.itemsExist("server.port", "server.name", "server.address")
				.size(1, 3)
				.localGroup("server");

		assertGroup(repository, "server").localItemsExist("port", "name", "address");
	}

	@Test
	public void nestedGroup() {
		ConfigMetadataGroup group = createGroup("server", "port", "name", "address");
		repository.registerRootGroup(group);
		ConfigMetadataGroup tomcat = group.registerGroup("tomcat");
		registerItems(tomcat, "port", "timeout");

		assertRepo(repository)
				.itemsExist("server.port", "server.name", "server.address",
						"server.tomcat.port", "server.tomcat.timeout")
				.size(1, 5)
				.localGroup("server").size(1, 5).localItems(3)
				.localGroup("tomcat").size(0, 2);
	}

	@Test
	public void noGroup() {
		ConfigMetadataGroup group = createGroup(null, "fooBar", "another");
		repository.registerRootGroup(group);
		assertRepo(repository)
				.size(0, 2)
				.itemsExist("fooBar", "another")
				.localItemsExist("fooBar", "another");
	}

	@Test
	public void emptyGroup() {
		ConfigMetadataGroup group = createGroup("  ", "fooBar", "another");
		repository.registerRootGroup(group);
		assertRepo(repository)
				.size(0, 2)
				.itemsExist("fooBar", "another")
				.localItemsExist("fooBar", "another");
	}

	@Test
	public void includeSimple() {
		repository.registerRootGroup(createGroup("server", "port", "name"));
		repository.registerRootGroup(createGroup("server", "port", "address"));
		assertRepo(repository)
				.itemsExist("server.port", "server.name", "server.address")
				.size(1, 3)
				.localGroup("server").size(0, 3);
	}

	@Test
	public void includeNested() {
		// server.port, server.name server.tomcat.name, server.tomcat.address, server.tomcat.specific.name
		ConfigMetadataGroup firstGroup = createGroup("server", "port", "name");
		ConfigMetadataGroup firstTomcat = firstGroup.registerGroup("tomcat");
		registerItems(firstTomcat, "name", "address");
		ConfigMetadataGroup firstTomcatSpecific = firstTomcat.registerGroup("specific");
		registerItems(firstTomcatSpecific, "name");


		// server.port, server.ip server.tomcat.name, server.tomcat.bind, server.tomcat.specific.name
		// server.tomcat.specific2.foo
		ConfigMetadataGroup secondGroup = createGroup("server", "port", "ip");
		ConfigMetadataGroup secondTomcat = secondGroup.registerGroup("tomcat");
		registerItems(secondTomcat, "name", "bind");
		ConfigMetadataGroup secondTomcatSpecific = secondTomcat.registerGroup("specific2");
		registerItems(secondTomcatSpecific, "foo");


		repository.registerRootGroup(firstGroup);
		repository.registerRootGroup(secondGroup);
		ConfigMetadataGroupAssert tomcatGroupAssert = assertRepo(repository)
				.itemsExist("server.port", "server.name", "server.ip",
						"server.tomcat.name", "server.tomcat.address", "server.tomcat.bind",
						"server.tomcat.specific.name", "server.tomcat.specific2.foo")
				.size(1, 8)
				.localGroup("server")
				.size(1, 8)
				.localGroup("tomcat")
				.size(2, 5);

		tomcatGroupAssert.localGroup("specific").size(0, 1);
		tomcatGroupAssert.localGroup("specific2").size(0, 1);
	}

}
