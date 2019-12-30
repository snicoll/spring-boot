/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.jdbc;

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;

/**
 * {@link HealthIndicator} that tests the status of a {@link DataSource} using
 * {@link Connection#isValid(int) Connection#isValid}.
 *
 * @author Stephane Nicoll
 * @since 2.3.0
 */
public class QueryLessDatasourceHealthIndicator extends AbstractHealthIndicator {

	private final DataSource dataSource;

	/**
	 * Create a new instance with the {@link DataSource} to monitor.
	 * @param dataSource the data source to monitor
	 */
	public QueryLessDatasourceHealthIndicator(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		try (Connection connection = this.dataSource.getConnection()) {
			boolean valid = connection.isValid(0);
			builder.status((valid) ? Status.UP : Status.DOWN);
		}
	}

}
