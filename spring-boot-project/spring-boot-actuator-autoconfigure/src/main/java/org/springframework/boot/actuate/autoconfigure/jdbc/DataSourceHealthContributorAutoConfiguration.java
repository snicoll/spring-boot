/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.jdbc;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.actuate.autoconfigure.health.CompositeHealthContributorConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.ConditionalOnEnabledHealthIndicator;
import org.springframework.boot.actuate.autoconfigure.jdbc.DataSourceHealthContributorAutoConfiguration.DataSourceAvailableCondition;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.jdbc.DataSourceHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.metadata.CompositeDataSourcePoolMetadataProvider;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadata;
import org.springframework.boot.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link DataSourceHealthIndicator}.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Arthur Kalimullin
 * @since 2.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ JdbcTemplate.class, AbstractRoutingDataSource.class })
@Conditional(DataSourceAvailableCondition.class)
@ConditionalOnEnabledHealthIndicator("db")
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class DataSourceHealthContributorAutoConfiguration extends
		CompositeHealthContributorConfiguration<DataSourceHealthIndicator, DataSource> implements InitializingBean {

	private final Collection<DataSourcePoolMetadataProvider> metadataProviders;

	private DataSourcePoolMetadataProvider poolMetadataProvider;

	public DataSourceHealthContributorAutoConfiguration(Map<String, DataSource> dataSources,
			ObjectProvider<DataSourcePoolMetadataProvider> metadataProviders) {
		this.metadataProviders = metadataProviders.orderedStream().collect(Collectors.toList());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.poolMetadataProvider = new CompositeDataSourcePoolMetadataProvider(this.metadataProviders);
	}

	@Bean
	@ConditionalOnMissingBean(name = { "dbHealthIndicator", "dbHealthContributor" })
	public HealthContributor dbHealthContributor(Map<String, DataSource> dataSources) {
		return createContributor(filterDataSources(dataSources));
	}

	private Map<String, DataSource> filterDataSources(Map<String, DataSource> candidates) {
		if (candidates == null) {
			return null;
		}
		Map<String, DataSource> dataSources = new LinkedHashMap<>();
		candidates.forEach((name, dataSource) -> {
			if (!(dataSource instanceof AbstractRoutingDataSource)) {
				dataSources.put(name, dataSource);
			}
		});
		return dataSources;
	}

	@Override
	protected DataSourceHealthIndicator createIndicator(DataSource source) {
		return new DataSourceHealthIndicator(source, getValidationQuery(source));
	}

	private String getValidationQuery(DataSource source) {
		DataSourcePoolMetadata poolMetadata = this.poolMetadataProvider.getDataSourcePoolMetadata(source);
		return (poolMetadata != null) ? poolMetadata.getValidationQuery() : null;
	}

	static class DataSourceAvailableCondition extends SpringBootCondition implements ConfigurationCondition {

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage.forCondition("DataSource Health Contributor Condition");
			String[] dataSourceBeanNames = context.getBeanFactory().getBeanNamesForType(DataSource.class);
			if (dataSourceBeanNames.length == 0) {
				return ConditionOutcome.noMatch(message.didNotFind("a DataSource bean").atAll());
			}
			for (String dataSourceBeanName : dataSourceBeanNames) {
				BeanDefinition beanDefinition = context.getRegistry().getBeanDefinition(dataSourceBeanName);
				Class<?> beanType = beanDefinition.getResolvableType().resolve();
				if (beanType != null && !AbstractRoutingDataSource.class.isAssignableFrom(beanType)) {
					return ConditionOutcome
							.match(message.because("found at least one DataSource that is not a routing DataSource"));
				}
			}
			return ConditionOutcome.noMatch(message.didNotFind("non routing DataSource beans").atAll());
		}

	}

}
