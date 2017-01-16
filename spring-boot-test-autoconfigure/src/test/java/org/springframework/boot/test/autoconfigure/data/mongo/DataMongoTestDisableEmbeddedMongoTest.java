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

package org.springframework.boot.test.autoconfigure.data.mongo;

import de.flapdoodle.embed.mongo.MongodExecutable;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Sample test that disables the embedded mongodb support.
 *
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
@TestPropertySource(properties = "spring.data.mongodb.port=12345")
public class DataMongoTestDisableEmbeddedMongoTest {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MongoProperties mongoProperties;

	@Test
	public void testEmbeddedSupportIsDisabled() throws Exception {
		assertThat(this.context).isNotNull();
		assertThat(this.context.getBeanNamesForType(MongodExecutable.class)).isEmpty();
	}

	@Test
	public void testMongoPortIsNotOverridden() throws Exception {
		assertThat(this.mongoProperties.getPort()).isEqualTo(12345);
	}

}
