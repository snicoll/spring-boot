/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.jdbc;

import javax.sql.DataSource;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Unwraps a {@link DataSource} that may have been proxied or wrapped in a
 * {@link DelegatingDataSource}.
 *
 * @author Tadaya Tsuyukubo
 * @author Stephane Nicoll
 * @since 2.0.7
 */
public final class DataSourceUnwrapper {

	private DataSourceUnwrapper() {
	}

	public static <T> T unwrap(DataSource dataSource, java.lang.Class<T> target) {
		if (target.isInstance(dataSource)) {
			return target.cast(dataSource);
		}
		if (dataSource instanceof DelegatingDataSource) {
			return unwrap(((DelegatingDataSource) dataSource).getTargetDataSource(),
					target);
		}
		else if (AopUtils.isAopProxy(dataSource)) {
			Object proxyTarget = AopProxyUtils.getSingletonTarget(dataSource);
			if (proxyTarget instanceof DataSource) {
				return unwrap((DataSource) proxyTarget, target);
			}
		}
		return null;
	}

}
