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

package org.springframework.boot.orm.jpa;

import javax.sql.DataSource;

/**
 * Strategy interface to provide the default DDL Auto flag based on a {@link DataSource}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public interface DefaultDdlAutoProvider {

	/**
	 * Return the default DDL auto for the specified {@link DataSource} or {@code null}
	 * if this instance is unable to determine it.
	 * @param dataSource the dataSource to handle
	 * @return the default DDL auto for the {@link DataSource} or {@code null}
	 */
	String getDefaultDdlAuto(DataSource dataSource);

}
