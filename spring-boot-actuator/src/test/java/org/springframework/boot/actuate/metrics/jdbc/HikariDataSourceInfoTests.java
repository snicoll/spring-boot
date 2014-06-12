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

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Before;

/**
 *
 * @author Stephane Nicoll
 */
public class HikariDataSourceInfoTests extends AbstractDataSourceInfoTests<HikariDataSourceInfo> {

	private HikariDataSourceInfo dataSourceInfo;

	@Before
	public void setup() {
		this.dataSourceInfo = createDataSourceInfo(0, 2);
	}

	@Override
	protected HikariDataSourceInfo getDataSourceInfo() {
		return this.dataSourceInfo;
	}

	private HikariDataSourceInfo createDataSourceInfo(int minSize, int maxSize) {
		HikariDataSource dataSource = (HikariDataSource) initializeBuilder().type(HikariDataSource.class).build();
		dataSource.setMinimumIdle(minSize);
		dataSource.setMaximumPoolSize(maxSize);

		return new HikariDataSourceInfo(dataSource);
	}
}
