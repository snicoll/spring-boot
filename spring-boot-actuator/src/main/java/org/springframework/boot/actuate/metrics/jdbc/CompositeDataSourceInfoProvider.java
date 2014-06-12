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

import java.util.ArrayList;
import java.util.Collection;

import javax.sql.DataSource;

/**
 * A {@link DataSourceInfoProvider} implementation that returns the first
 * {@link DataSourceInfo} that is found by one of its delegate.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class CompositeDataSourceInfoProvider implements DataSourceInfoProvider {

	private final Collection<DataSourceInfoProvider> providers;

	/**
	 * Create an instance with an initial collection of delegates to use.
	 */
	public CompositeDataSourceInfoProvider(Collection<DataSourceInfoProvider> providers) {
		this.providers = providers;
	}

	/**
	 * Create an instance with no delegate.
	 */
	public CompositeDataSourceInfoProvider() {
		this(new ArrayList<DataSourceInfoProvider>());
	}

	@Override
	public DataSourceInfo getDataSourceInfo(DataSource dataSource) {
		for (DataSourceInfoProvider provider : providers) {
			DataSourceInfo dataSourceInfo = provider.getDataSourceInfo(dataSource);
			if (dataSourceInfo != null) {
				return dataSourceInfo;
			}
		}
		return null;
	}

	/**
	 * Add a {@link DataSourceInfoProvider} delegate to the list.
	 */
	public void addDataSourceInfoProvider(DataSourceInfoProvider provider) {
		this.providers.add(provider);
	}

}
