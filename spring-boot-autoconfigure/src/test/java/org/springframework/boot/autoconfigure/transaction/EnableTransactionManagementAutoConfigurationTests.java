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

package org.springframework.boot.autoconfigure.transaction;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Test;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EnableTransactionManagementAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class EnableTransactionManagementAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void transactionNotManagedWithNoTransactionManager() {
		load(BaseConfiguration.class);
		assertThat(this.context.getBean(TransactionalService.class)
				.isTransactionActive()).isFalse();
	}

	@Test
	public void transactionManagerUsesCglibByDefault() {
		load(TransactionManagersConfiguration.class);
		assertThat(this.context.getBean(AnotherServiceImpl.class)
				.isTransactionActive()).isTrue();
		assertThat(this.context.getBeansOfType(TransactionalServiceImpl.class)).hasSize(1);
	}

	@Test
	public void transactionManagerCanBeConfiguredToJdkProxy() {
		load(TransactionManagersConfiguration.class, "spring.aop.proxy-target-class=false");
		assertThat(this.context.getBean(AnotherService.class)
				.isTransactionActive()).isTrue();
		assertThat(this.context.getBeansOfType(AnotherServiceImpl.class)).hasSize(0);
		assertThat(this.context.getBeansOfType(TransactionalServiceImpl.class)).hasSize(0);
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(config);
		applicationContext.register(EnableTransactionManagementAutoConfiguration.class);
		EnvironmentTestUtils.addEnvironment(applicationContext, environment);
		applicationContext.refresh();
		this.context = applicationContext;
	}

	@Configuration
	static class BaseConfiguration {

		@Bean
		public TransactionalService transactionalService() {
			return new TransactionalServiceImpl();
		}

		@Bean
		public AnotherServiceImpl anotherService() {
			return new AnotherServiceImpl();
		}
	}

	@Configuration
	@Import(BaseConfiguration.class)
	static class TransactionManagersConfiguration {

		@Bean
		public DataSourceTransactionManager transactionManager() {
			return new DataSourceTransactionManager(dataSource());
		}

		@Bean
		public DataSource dataSource() {
			return DataSourceBuilder.create()
					.driverClassName("org.hsqldb.jdbc.JDBCDriver")
					.url("jdbc:hsqldb:mem:tx").username("sa").build();
		}

	}

	interface TransactionalService {

		@Transactional
		boolean isTransactionActive();

	}

	static class TransactionalServiceImpl implements TransactionalService {


		@Override
		public boolean isTransactionActive() {
			return TransactionSynchronizationManager.isActualTransactionActive();
		}
	}

	interface AnotherService {

		boolean isTransactionActive();

	}

	static class AnotherServiceImpl implements AnotherService {


		@Override
		@Transactional
		public boolean isTransactionActive() {
			return TransactionSynchronizationManager.isActualTransactionActive();
		}
	}

}
