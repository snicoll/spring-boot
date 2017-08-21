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

package org.springframework.boot.autoconfigure.orm.jpa;

import java.util.List;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.orm.jpa.DefaultDdlAutoProvider;

/**
 * A {@link DefaultDdlAutoProvider} that invokes a configurable number of
 * {@link DefaultDdlAutoProvider} instances for embedded data sources only.
 *
 * @author Stephane Nicoll
 */
class HibernateDefaultDdlAutoProvider implements DefaultDdlAutoProvider {

	private final List<DefaultDdlAutoProvider> providers;

	HibernateDefaultDdlAutoProvider(List<DefaultDdlAutoProvider> providers) {
		this.providers = providers;
	}

	@Override
	public String getDefaultDdlAuto(DataSource dataSource) {
		if (!EmbeddedDatabaseConnection.isEmbedded(dataSource)) {
			return "none";
		}
		for (DefaultDdlAutoProvider provider : this.providers) {
			String defaultDdlMode = provider.getDefaultDdlAuto(dataSource);
			if (defaultDdlMode != null) {
				return defaultDdlMode;
			}
		}
		return "create-drop";
	}

}
