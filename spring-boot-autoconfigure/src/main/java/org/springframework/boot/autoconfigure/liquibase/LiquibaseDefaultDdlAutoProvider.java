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

package org.springframework.boot.autoconfigure.liquibase;

import java.util.List;

import javax.sql.DataSource;

import liquibase.integration.spring.SpringLiquibase;

import org.springframework.boot.orm.jpa.DefaultDdlAutoProvider;

/**
 * A Liquibase {@link DefaultDdlAutoProvider} that disables automatic DDL generation if
 * Liquibase is handling the {@link DataSource}.
 *
 * @author Stephane Nicoll
 */
class LiquibaseDefaultDdlAutoProvider implements DefaultDdlAutoProvider {

	private final List<SpringLiquibase> liquibases;

	LiquibaseDefaultDdlAutoProvider(List<SpringLiquibase> liquibases) {
		this.liquibases = liquibases;
	}

	@Override
	public String getDefaultDdlAuto(DataSource dataSource) {
		for (SpringLiquibase liquibase : this.liquibases) {
			if (dataSource.equals(liquibase.getDataSource())) {
				return "none";
			}
		}
		return null;
	}

}
