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

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link QueryLessDatasourceHealthIndicator}.
 *
 * @author Stephane Nicoll
 */
class QueryLessDatasourceHealthIndicatorTests {

	@Test
	void healthIndicatorWithDefaultSettings() {
		EmbeddedDatabaseConnection db = EmbeddedDatabaseConnection.HSQL;
		SingleConnectionDataSource dataSource = new SingleConnectionDataSource(db.getUrl("testdb") + ";shutdown=true",
				"sa", "", false);
		try {
			dataSource.setDriverClassName(db.getDriverClassName());
			QueryLessDatasourceHealthIndicator indicator = new QueryLessDatasourceHealthIndicator(dataSource);
			Health health = indicator.health();
			assertThat(health.getStatus()).isEqualTo(Status.UP);
			assertThat(health.getDetails()).isEmpty();
		}
		finally {
			dataSource.destroy();
		}
	}

}
