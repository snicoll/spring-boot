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

package org.springframework.boot.actuate.autoconfigure;

import java.io.IOException;
import java.util.Properties;

import org.springframework.boot.actuate.info.EnvironmentInfoContributor;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.actuate.info.SimpleInfoContributor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for standard
 * {@link InfoContributor}s.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Configuration
@AutoConfigureAfter(ProjectInfoAutoConfiguration.class)
@AutoConfigureBefore(EndpointAutoConfiguration.class)
@EnableConfigurationProperties(InfoContributorProperties.class)
public class InfoContributorAutoConfiguration {

	/**
	 * The default order for the core {@link InfoContributor} beans.
	 */
	public static final int DEFAULT_ORDER = Ordered.HIGHEST_PRECEDENCE + 10;

	private final InfoContributorProperties properties;

	public InfoContributorAutoConfiguration(InfoContributorProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnEnabledInfoContributor("env")
	@Order(DEFAULT_ORDER)
	public EnvironmentInfoContributor envInfoContributor(
			ConfigurableEnvironment environment) {
		return new EnvironmentInfoContributor(environment);
	}

	@Bean
	@ConditionalOnEnabledInfoContributor("git")
	@ConditionalOnSingleCandidate(GitProperties.class)
	@Order(DEFAULT_ORDER)
	public InfoContributor gitInfoContributor(GitProperties gitProperties) throws IOException {
		InfoContributorProperties.GitMode mode = this.properties.getGit().getMode();
		Properties content = filterGitProperties(gitProperties, mode);
		return SimpleInfoContributor.fromProperties("git", content);
	}

	private Properties filterGitProperties(GitProperties gitProperties, InfoContributorProperties.GitMode mode) {
		if (mode.equals(InfoContributorProperties.GitMode.FULL)) {
			return gitProperties.toProperties();
		}
		else {
			Properties target = new Properties();
			copyIfSet(gitProperties, target, "branch");
			copyIfSet(gitProperties, target, "commit.id");
			copyIfSet(gitProperties, target, "commit.time");
			return target;
		}
	}

	private void copyIfSet(GitProperties source, Properties target, String key) {
		String value = source.get(key);
		if (StringUtils.hasText(value)) {
			target.put(key, value);
		}
	}

}
