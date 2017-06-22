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

package org.springframework.boot.autoconfigure.jdbc;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;

/**
 * Bean to handle {@link DataSource} initialization by running {@literal schema-*.sql} on
 * {@link InitializingBean#afterPropertiesSet()} and {@literal data-*.sql} SQL scripts on
 * a {@link SmartInitializingSingleton#afterSingletonsInstantiated()}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Kazuki Shimizu
 * @since 1.1.0
 * @see DataSourceAutoConfiguration
 */
class DataSourceInitializerInvoker implements InitializingBean {

	private final DataSourceInitializer dataSourceInitializer;

	DataSourceInitializerInvoker(ObjectProvider<DataSource> dataSource,
			DataSourceProperties properties,
			ApplicationContext applicationContext) {
		DataSource ds = dataSource.getIfAvailable();
		this.dataSourceInitializer = ds != null
				? new DataSourceInitializer(ds, properties, applicationContext) : null;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.dataSourceInitializer != null) {
			this.dataSourceInitializer.createSchema();
			this.dataSourceInitializer.initSchema();
		}
	}

}
