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

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.batch.autoconfigure.BatchTransactionManager;
import org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BatchDataMongoAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
class BatchDataMongoAutoConfigurationIntegrationTests {

	@Container
	static final MongoDBContainer mongoDb = new MongoDBContainer(DockerImageName.parse("mongo:8.0.11")).withReplicaSet();

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(JobConfiguration.class)
		.withPropertyValues("spring.batch.data.mongodb.schema.initialize=true",
				"spring.mongodb.uri=" + mongoDb.getConnectionString() + "/test")
		.withConfiguration(AutoConfigurations.of(MongoAutoConfiguration.class, DataMongoAutoConfiguration.class,
				BatchDataMongoAutoConfiguration.class));

	@Test
	void testDefinesAndLaunchesLocalJob() {
		this.contextRunner.withUserConfiguration(JobConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(JobOperator.class).hasSingleBean(JobRepository.class).hasSingleBean(Job.class);
			JobParameters jobParameters = new JobParametersBuilder().addString("name", "foo")
				.addLocalDateTime("runtime", LocalDateTime.now())
				.toJobParameters();

			JobExecution jobExecution = context.getBean(JobOperator.class)
				.start(context.getBean(Job.class), jobParameters);
			assertThat(jobExecution).isNotNull();
			assertThat(context.getBean(JobRepository.class).getLastJobExecution("job", jobParameters)).isNotNull();
		});
	}

	@Configuration(proxyBeanMethods = false)
	static class JobConfiguration {

		@Bean
		@BatchTransactionManager
		MongoTransactionManager transactionManager(MongoDatabaseFactory dbFactory) {
			return new MongoTransactionManager(dbFactory);
		}

		@Bean
		Job job(JobRepository jobRepository, MongoTransactionManager transactionManager) {
			return new JobBuilder("job", jobRepository)
				.start(new StepBuilder("step1", jobRepository)
					.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, transactionManager)
					.build())
				.build();
		}

	}

}
