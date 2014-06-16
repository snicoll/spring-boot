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

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import org.springframework.jndi.JndiTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for JNDI-based {@link DataSource}.
 *
 * <p>Runs before {@link DataSourceAutoConfiguration} so that it takes precedence over
 * any other auto-discovery mechanism.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@Configuration
@ConditionalOnClass(JndiDataSourceLookup.class)
@ConditionalOnMissingBean(DataSource.class)
@ConditionalOnProperty(prefix = DataSourceAutoConfiguration.CONFIGURATION_PREFIX, value = "jndi")
@EnableConfigurationProperties(DataSourceProperties.class)
@AutoConfigureBefore(DataSourceAutoConfiguration.class)
public class JndiDataSourceAutoConfiguration {

	@Autowired
	private JndiDataSourceLookup jndiDataSourceLookup;

	@Autowired
	private DataSourceProperties properties;

	@Bean
	public DataSource dataSource() {
		String jndiName = properties.getJndi();
		return jndiDataSourceLookup.getDataSource(jndiName);
	}

	/**
	 * Provide a default {@link JndiDataSourceLookup} bean if none is set. If one
	 * and only one {@link JndiTemplate} is defined, use that by default.
	 */
	@Configuration
	static class JndiDataSourceConfig {

		@Autowired(required=false)
		private JndiTemplate jndiTemplate;

		@Bean
		@ConditionalOnMissingBean(JndiDataSourceLookup.class)
		public JndiDataSourceLookup jndiDataSourceLookup() {
			JndiDataSourceLookup jndiDataSourceLookup = new JndiDataSourceLookup();

			if (jndiTemplate != null) {
				jndiDataSourceLookup.setJndiTemplate(jndiTemplate);
			}
			return jndiDataSourceLookup;
		}
	}

}

