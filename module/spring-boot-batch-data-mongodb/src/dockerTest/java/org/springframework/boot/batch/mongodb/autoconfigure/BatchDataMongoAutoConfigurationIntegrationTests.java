/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.batch.mongodb.autoconfigure;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.batch.autoconfigure.BatchJobLauncherAutoConfiguration;
import org.springframework.boot.batch.autoconfigure.JobLauncherApplicationRunner;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BatchDataMongoAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
class BatchDataMongoAutoConfigurationIntegrationTests {

	@Container
	static final MongoDBContainer mongoDb = TestImage.container(MongoDBContainer.class).withReplicaSet();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(JobConfiguration.class)
		.withPropertyValues("spring.batch.data.mongodb.schema.initialize=true",
				"spring.mongodb.uri=" + mongoDb.getReplicaSetUrl())
		.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, DataMongoAutoConfiguration.class,
				BatchDataMongoAutoConfiguration.class, BatchJobLauncherAutoConfiguration.class));

	@Test
	void testDefinesAndLaunchesLocalJob() {
		this.contextRunner.withUserConfiguration(JobConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JobOperator.class);
			context.getBean(JobLauncherApplicationRunner.class).run(new DefaultApplicationArguments("jobParam=test"));
			JobParameters jobParameters = new JobParametersBuilder().addString("jobParam", "test").toJobParameters();
			assertThat(context.getBean(JobRepository.class).getLastJobExecution("job", jobParameters)).isNotNull();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class JobConfiguration {

		@Bean
		Job job(JobRepository jobRepository) {
			AbstractJob job = new AbstractJob() {

				@Override
				public Collection<String> getStepNames() {
					return Collections.emptySet();
				}

				@Override
				public Step getStep(String stepName) {
					return null;
				}

				@Override
				protected void doExecute(JobExecution execution) {
					execution.setStatus(BatchStatus.COMPLETED);
				}
			};
			job.setJobRepository(jobRepository);
			return job;
		}

	}

}
