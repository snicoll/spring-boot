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

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 *
 * @author Stephane Nicoll
 */
public class CompositeDataSourceInfoProviderTests {

	@Mock
	private DataSourceInfoProvider firstProvider;

	@Mock
	private DataSourceInfo first;

	@Mock
	private DataSource firstDataSource;

	@Mock
	private DataSourceInfoProvider secondProvider;

	@Mock
	private DataSourceInfo second;

	@Mock
	private DataSource secondDataSource;

	@Mock
	private DataSource unknownDataSource;


	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		given(firstProvider.getDataSourceInfo(firstDataSource)).willReturn(first);
		given(firstProvider.getDataSourceInfo(secondDataSource)).willReturn(second);
	}

	@Test
	public void createWithProviders() {
		CompositeDataSourceInfoProvider provider =
				new CompositeDataSourceInfoProvider(Arrays.asList(firstProvider, secondProvider));
		assertSame(first, provider.getDataSourceInfo(firstDataSource));
		assertSame(second, provider.getDataSourceInfo(secondDataSource));
		assertNull(provider.getDataSourceInfo(unknownDataSource));
	}

	@Test
	public void addProvider() {
		CompositeDataSourceInfoProvider provider =
				new CompositeDataSourceInfoProvider();
		assertNull(provider.getDataSourceInfo(firstDataSource));
		provider.addDataSourceInfoProvider(firstProvider);
		assertSame(first, provider.getDataSourceInfo(firstDataSource));
	}

}
