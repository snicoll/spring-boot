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

package org.springframework.boot.actuate.metrics.jdbc;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp.BasicDataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Register the {@link DataSourceInfoProvider} instances for the supported
 * data sources.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@Configuration
public class DataSourceInfoProvidersConfiguration {

	@Configuration
	@ConditionalOnClass(org.apache.tomcat.jdbc.pool.DataSource.class)
	static class TomcatDataSourceProviderConfiguration {

		@Bean
		public DataSourceInfoProvider tomcatDataSourceProvider() {
			return new DataSourceInfoProvider() {
				@Override
				public DataSourceInfo getDataSourceInfo(DataSource dataSource) {
					if (dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource) {
						return new TomcatDataSourceInfo((org.apache.tomcat.jdbc.pool.DataSource) dataSource);
					}
					return null;
				}
			};
		}
	}

	@Configuration
	@ConditionalOnClass(HikariDataSource.class)
	static class HikariDataSourceProviderConfiguration {

		@Bean
		public DataSourceInfoProvider hikariDataSourceProvider() {
			return new DataSourceInfoProvider() {
				@Override
				public DataSourceInfo getDataSourceInfo(DataSource dataSource) {
					if (dataSource instanceof HikariDataSource) {
						return new HikariDataSourceInfo((HikariDataSource) dataSource);
					}
					return null;
				}
			};
		}
	}

	@Configuration
	@ConditionalOnClass(BasicDataSource.class)
	static class CommonsDbcpDataSourceProviderConfiguration {

		@Bean
		public DataSourceInfoProvider commonsDbcpDataSourceProvider() {
			return new DataSourceInfoProvider() {
				@Override
				public DataSourceInfo getDataSourceInfo(DataSource dataSource) {
					if (dataSource instanceof BasicDataSource) {
						return new CommonsDbcpDataSourceInfo((BasicDataSource) dataSource);
					}
					return null;
				}
			};
		}
	}

}
