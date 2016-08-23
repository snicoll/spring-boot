/*
 * Copyright 2012-2016 the original author or authors.
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

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.PriorityOrdered;

/**
 *
 * @author Stephane Nicoll
 */
class HikariDataSourcePostProcessor implements BeanPostProcessor, PriorityOrdered {

	private static Log logger = LogFactory.getLog(HikariDataSourcePostProcessor.class);

	@Override
	public Object postProcessBeforeInitialization(Object bean, String s)
			throws BeansException {
		if (bean instanceof HikariDataSource) {
			HikariDataSource dataSource = (HikariDataSource) bean;
			if (dataSource.getDriverClassName() != null
					&& dataSource.getDataSourceClassName() != null) {
				try {
					DirectFieldAccessor dfa = new DirectFieldAccessor(dataSource);
					dfa.setPropertyValue("driverClassName", null);
				}
				catch (InvalidPropertyException ex) {
					logger.warn("Cannot nullify 'driverClassName' property", ex);
				}
			}
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String s) throws BeansException {
		return bean;
	}

	@Override
	public int getOrder() {
		return PriorityOrdered.LOWEST_PRECEDENCE;
	}

}
