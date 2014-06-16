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

package org.springframework.boot.autoconfigure.jdbc;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Random;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.jndi.JndiTemplate;

/**
 * Tests for {@link JndiDataSourceAutoConfiguration}
 *
 * @author Stephane Nicoll
 */
public class JndiDataSourceAutoConfigurationTests {

	private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	@Before
	public void init() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.initialize:false",
				"spring.datasource.url:jdbc:hsqldb:mem:testdb-" + new Random().nextInt());
	}

	@After
	public void restore() {
		context.close();
	}

	@Test
	public void noJndiProperty() {
		this.context.register(JndiDataSourceAutoConfiguration.class, DataSourceAutoConfiguration.class);
		this.context.refresh();

		assertNotNull(this.context.getBean(DataSource.class));
	}

	@Test
	public void jndiProperty() throws NamingException {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.jndi:jdbc/myTestDS");
		this.context.register(JndiTemplateTestConfig.class);
		this.context.register(JndiDataSourceAutoConfiguration.class, DataSourceAutoConfiguration.class);
		this.context.refresh();

		assertNotNull(this.context.getBean(DataSource.class));

		JndiTemplate jndiTemplate = context.getBean(JndiTemplate.class);
		verify(jndiTemplate).lookup("jdbc/myTestDS", DataSource.class);
	}

	@Test
	public void jndiPropertyWithDataSourceLookupOverride() {
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.datasource.jndi:jdbc/myTestDS");
		this.context.register(JndiDataSourceLookupTestConfig.class);
		this.context.register(JndiDataSourceAutoConfiguration.class, DataSourceAutoConfiguration.class);
		this.context.refresh();

		assertNotNull(this.context.getBean(DataSource.class));

		// Only one present
		JndiDataSourceLookup jndiDataSourceLookup = context.getBean(JndiDataSourceLookup.class);
		verify(jndiDataSourceLookup).getDataSource("jdbc/myTestDS");
	}


	@Configuration
	static class JndiTemplateTestConfig {

		@Bean
		public JndiTemplate jndiTemplate() throws NamingException {
			JndiTemplate jndiTemplate = mock(JndiTemplate.class);
			given(jndiTemplate.lookup("java:comp/env/jdbc/myTestDS", DataSource.class))
					.willThrow(new NamingException("test"));
			given(jndiTemplate.lookup("jdbc/myTestDS", DataSource.class)).willReturn(mock(DataSource.class));
			return jndiTemplate;
		}
	}

	@Configuration
	static class JndiDataSourceLookupTestConfig {

		@Bean
		public JndiDataSourceLookup jndiDataSourceLookup() {
			JndiDataSourceLookup jndiDataSourceLookup = mock(JndiDataSourceLookup.class);
			given(jndiDataSourceLookup.getDataSource("jdbc/myTestDS")).willReturn(mock(DataSource.class));
			return jndiDataSourceLookup;
		}
	}
}
