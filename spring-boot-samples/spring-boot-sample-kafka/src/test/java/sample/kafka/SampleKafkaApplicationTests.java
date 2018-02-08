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
package sample.kafka;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.test.rule.KafkaEmbedded;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for demo application.
 *
 * @author hcxin
 * @author Stephane Nicoll
 */
public class SampleKafkaApplicationTests {

	@ClassRule
	public static final KafkaEmbedded kafkaEmbedded = new KafkaEmbedded(1, true,
			"testTopic");

	@Rule
	public OutputCapture outputCapture = new OutputCapture();


	@Test
	public void sendSimpleMessage() throws Exception {
		ConfigurableApplicationContext context = SpringApplication.run(
				SampleKafkaApplication.class,
				"--spring.kafka.bootstrap-servers=" + kafkaEmbedded.getBrokersAsString(),
				"spring.kafka.consumer.auto-offset-reset=earliest");
		Thread.sleep(5000L);
		assertThat(this.outputCapture.toString().contains("Received sample message"))
				.isTrue();
		context.close();
	}

}
