/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.liquibase;

import java.util.function.Supplier;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;

/**
 * Configure the {@link DataSource} that liquibase should use.
 *
 * @author Stephane Nicoll
 */
public class SpringLiquibaseFactory {

	private final DataSource liquibaseDataSource;

	private final LiquibaseProperties liquibaseProperties;

	private final DataSource dataSource;

	private final DataSourceProperties dataSourceProperties;

	SpringLiquibaseFactory(DataSource liquibaseDataSource, LiquibaseProperties liquibaseProperties,
			DataSource dataSource, DataSourceProperties dataSourceProperties) {
		this.liquibaseDataSource = liquibaseDataSource;
		this.liquibaseProperties = liquibaseProperties;
		this.dataSource = dataSource;
		this.dataSourceProperties = dataSourceProperties;
	}

	SpringLiquibase create() {
		DataSource liquibaseDataSource = determineDataSource();
		if (liquibaseDataSource != null) {
			SpringLiquibase liquibase = new SpringLiquibase();
			liquibase.setDataSource(liquibaseDataSource);
			return liquibase;
		}
		SpringLiquibase liquibase = new DataSourceClosingSpringLiquibase();
		liquibase.setDataSource(createNewDataSource());
		return liquibase;
	}

	private DataSource determineDataSource() {
		if (this.liquibaseDataSource != null) {
			return this.liquibaseDataSource;
		}
		if (this.liquibaseProperties.getUrl() == null && this.liquibaseProperties.getUser() == null) {
			return this.dataSource;
		}
		return null;
	}

	private DataSource createNewDataSource() {
		String url = getProperty(this.liquibaseProperties::getUrl, this.dataSourceProperties::determineUrl);
		String user = getProperty(this.liquibaseProperties::getUser, this.dataSourceProperties::determineUsername);
		String password = getProperty(this.liquibaseProperties::getPassword,
				this.dataSourceProperties::determinePassword);
		return DataSourceBuilder.create().url(url).username(user).password(password).build();
	}

	private String getProperty(Supplier<String> property, Supplier<String> defaultValue) {
		String value = property.get();
		return (value != null) ? value : defaultValue.get();
	}

}
