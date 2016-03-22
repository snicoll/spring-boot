/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.neo4j;

import com.hazelcast.util.Base64;
import org.junit.Test;
import org.neo4j.ogm.authentication.Credentials;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.config.DriverConfiguration;
import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.drivers.embedded.driver.EmbeddedDriver;
import org.neo4j.ogm.drivers.http.driver.HttpDriver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Neo4jProperties}.
 *
 * @author Stephane Nicoll
 */
public class Neo4jPropertiesTests {

	private Neo4jProperties properties = new Neo4jProperties();

	@Test
	public void defaultUseEmbeddedInMemory() {
		Configuration configuration = this.properties.createConfiguration();
		assertDriver(configuration, EmbeddedDriver.class, null);
	}

	@Test
	public void httpUriUseHttpServer() {
		this.properties.setUri("http://localhost:7474");
		Configuration configuration = this.properties.createConfiguration();
		assertDriver(configuration, HttpDriver.class, "http://localhost:7474");
	}

	@Test
	public void fileUriUseEmbeddedServer() {
		this.properties.setUri("file://var/tmp/graph.db");
		Configuration configuration = this.properties.createConfiguration();
		assertDriver(configuration, EmbeddedDriver.class, "file://var/tmp/graph.db");
	}

	@Test
	public void credentialsAreSet() {
		this.properties.setUri("http://localhost:7474");
		this.properties.setUsername("user");
		this.properties.setPassword("secret");
		Configuration configuration = this.properties.createConfiguration();
		assertDriver(configuration, HttpDriver.class, "http://localhost:7474");
		assertCredentials(configuration, "user", "secret");
	}

	@Test
	public void credentialsAreSetFromUri() {
		this.properties.setUri("http://user:secret@my-server:7474");
		Configuration configuration = this.properties.createConfiguration();
		assertDriver(configuration, HttpDriver.class, "http://user:secret@my-server:7474");
		assertCredentials(configuration, "user", "secret");
	}

	@Test
	public void embeddedModeDisabledUseHttpUri() {
		this.properties.getEmbedded().setEnabled(false);
		Configuration configuration = this.properties.createConfiguration();
		assertDriver(configuration, HttpDriver.class, "http://localhost:7474");
	}

	@Test
	public void embeddedModeWithRelativeLocation() {
		this.properties.setUri("target/neo4j/my.db");
		Configuration configuration = this.properties.createConfiguration();
		assertDriver(configuration, EmbeddedDriver.class, "target/neo4j/my.db");
	}

	private static void assertDriver(Configuration actual, Class<? extends Driver> driver,
			String uri) {
		assertThat(actual).isNotNull();
		DriverConfiguration driverConfig = actual.driverConfiguration();
		assertThat(driverConfig.getDriverClassName()).isEqualTo(driver.getName());
		assertThat(driverConfig.getURI()).isEqualTo(uri);
	}

	private static void assertCredentials(Configuration actual, String username, String password) {
		Credentials credentials = actual.driverConfiguration().getCredentials();
		if (username == null & password == null) {
			assertThat(credentials).isNull();
		}
		else {
			assertThat(credentials).isNotNull();
			Object content = credentials.credentials();
			assertThat(content).isInstanceOf(String.class);
			String[] auth = new String(Base64.decode(((String) content)
					.getBytes())).split(":");
			assertThat(auth[0]).isEqualTo(username);
			assertThat(auth[1]).isEqualTo(password);
			assertThat(auth).hasSize(2);
		}
	}

}
