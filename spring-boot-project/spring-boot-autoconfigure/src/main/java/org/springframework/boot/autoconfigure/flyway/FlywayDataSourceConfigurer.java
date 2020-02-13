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

package org.springframework.boot.autoconfigure.flyway;

import java.util.function.Supplier;

import javax.sql.DataSource;

import org.flywaydb.core.api.configuration.FluentConfiguration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Configure the {@link DataSource} that flyway should use.
 *
 * @author Stephane Nicoll
 * @since 2.3.0
 */
public class FlywayDataSourceConfigurer {

	private final DataSource dataSource;

	private final FlywayProperties flywayProperties;

	private final DataSourceProperties dataSourceProperties;

	FlywayDataSourceConfigurer(DataSource dataSource, FlywayProperties flywayProperties,
			DataSourceProperties dataSourceProperties) {
		this.dataSource = dataSource;
		this.flywayProperties = flywayProperties;
		this.dataSourceProperties = dataSourceProperties;
	}

	DataSource configureDataSource(FluentConfiguration configuration) {
		if (this.flywayProperties.isCreateDataSource()) {
			createFlywayDataSource(configuration);
		}
		else if (this.dataSource != null) {
			configuration.dataSource(this.dataSource);
		}
		return configuration.getDataSource();
	}

	private void createFlywayDataSource(FluentConfiguration configuration) {
		String url = getProperty(this.flywayProperties::getUrl, this.dataSourceProperties::determineUrl);
		String user = getProperty(this.flywayProperties::getUser, this.dataSourceProperties::determineUsername);
		String password = getProperty(this.flywayProperties::getPassword, this.dataSourceProperties::determinePassword);
		configuration.dataSource(url, user, password);
		if (!CollectionUtils.isEmpty(this.flywayProperties.getInitSqls())) {
			String initSql = StringUtils.collectionToDelimitedString(this.flywayProperties.getInitSqls(), "\n");
			configuration.initSql(initSql);
		}
	}

	private boolean ifAvailable(ObjectProvider<DataSource> datasourceCandidate, FluentConfiguration configuration) {
		DataSource datasource = datasourceCandidate.getIfAvailable();
		if (datasource != null) {
			configuration.dataSource(datasource);
			return true;
		}
		return false;
	}

	private String getProperty(Supplier<String> property, Supplier<String> defaultValue) {
		String value = property.get();
		return (value != null) ? value : defaultValue.get();
	}

}
