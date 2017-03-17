/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.elasticsearch;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@DataElasticsearchTest
public class DataElasticsearchTestIntegrationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;

	@Autowired
	private ExampleRepository exampleRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testRepository() {
		ExampleDocument exampleDocument = new ExampleDocument();
		exampleDocument.setIbsn("1234567890123");
		exampleDocument.setTitle("Some Title");
		exampleDocument.setDescription(
				"Some Description about a great book written by some awesome author!");

		this.exampleRepository.save(exampleDocument);

		assertThat(this.exampleRepository.findOne(exampleDocument.getIbsn()))
				.isEqualToIgnoringGivenFields(
						exampleDocument, "isbn", "title", "description");
		assertThat(this.elasticsearchTemplate.indexExists("books")).isTrue();
	}

	@Test
	public void didNotInjectExampleController() {
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.applicationContext.getBean(ExampleService.class);
	}

}
