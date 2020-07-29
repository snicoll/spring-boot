/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.neo4j;

import org.neo4j.driver.Driver;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.ConditionalOnRepositoryType;
import org.springframework.boot.autoconfigure.data.RepositoryType;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactoryBean;

/**
 * Shared entry point for the configuration of Spring Data Neo4j repositories in their
 * imperative and reactive forms.
 *
 * @author Dave Syer
 * @author Oliver Gierke
 * @author Josh Long
 * @author Michael J. Simons
 * @see EnableNeo4jRepositories
 * @since 1.4.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Driver.class, Neo4jRepository.class })
@ConditionalOnRepositoryType(store = "neo4j", type = RepositoryType.IMPERATIVE)
@ConditionalOnMissingBean({ Neo4jRepositoryFactoryBean.class, Neo4jRepositoryConfigurationExtension.class })
@Import(Neo4jRepositoriesRegistrar.class)
@AutoConfigureAfter(Neo4jDataAutoConfiguration.class)
public class Neo4jRepositoriesAutoConfiguration {

}
