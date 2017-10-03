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

package org.springframework.boot.jdbc;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.util.Assert;

/**
 *
 * @author Stephane Nicoll
 */
class DataSourceBuilderInitializer {


	@SuppressWarnings("unchecked")
	<T extends DataSource> DataSourceBuilder<T> initializeFrom(T dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		String type = dataSource.getClass().getName();
		if (type.equals("com.zaxxer.hikari.HikariDataSource")) {
			return (DataSourceBuilder<T>) new HikariDataSourceBuilderInitializer()
					.initializeFrom(dataSource);
		}
		throw new IllegalStateException("YOLO");
	}

	private static class HikariDataSourceBuilderInitializer {

		public DataSourceBuilder<?> initializeFrom(DataSource dataSource) {
			HikariDataSource hikariDs = (HikariDataSource) dataSource;
			return DataSourceBuilder.create()
					.type(HikariDataSource.class)
					.driverClassName(hikariDs.getDriverClassName())
					.url(hikariDs.getJdbcUrl())
					.username(hikariDs.getUsername())
					.password(hikariDs.getPassword());
		}
	}

}
