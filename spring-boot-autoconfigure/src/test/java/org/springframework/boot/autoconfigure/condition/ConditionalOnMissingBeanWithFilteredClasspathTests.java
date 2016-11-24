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

package org.springframework.boot.autoconfigure.condition;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.testutil.ClassPathExclusions;
import org.springframework.boot.testutil.FilteredClassPathRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests {@link ConditionalOnMissingBean} with filtered classpath.
 *
 * @author Stephane Nicoll
 */
@RunWith(FilteredClassPathRunner.class)
@ClassPathExclusions("spring-web-*.jar")
public class ConditionalOnMissingBeanWithFilteredClasspathTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Test
	public void testNameOnMissingBeanTypeWithMissingImport() {
		this.context.register(OnBeanTypeConfiguration.class);
		this.context.refresh();
		assertThat(this.context.containsBean("bar")).isTrue();
	}


	@Configuration
	@ConditionalOnMissingBean(type = "org.springframework.boot.web.client.RestTemplateBuilder")
	protected static class OnBeanTypeConfiguration {

		@Bean
		public String bar() {
			return "bar";
		}

	}

}
