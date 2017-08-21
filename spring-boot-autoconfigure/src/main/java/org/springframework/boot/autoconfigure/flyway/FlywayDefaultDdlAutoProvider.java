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

package org.springframework.boot.autoconfigure.flyway;

import java.util.List;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;

import org.springframework.boot.orm.jpa.DefaultDdlAutoProvider;

/**
 * A Flyway {@link DefaultDdlAutoProvider} that disables automatic DDL generation if
 * Flyway is handling the {@link DataSource}.
 *
 * @author Stephane Nicoll
 */
class FlywayDefaultDdlAutoProvider implements DefaultDdlAutoProvider {

	private final List<Flyway> flyways;

	FlywayDefaultDdlAutoProvider(List<Flyway> flyways) {
		this.flyways = flyways;
	}

	@Override
	public String getDefaultDdlAuto(DataSource dataSource) {
		for (Flyway flyway : this.flyways) {
			if (dataSource.equals(flyway.getDataSource())) {
				return "none";
			}
		}
		return null;
	}

}
