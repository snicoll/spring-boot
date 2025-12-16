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

package smoketest.log4j2;

import java.util.stream.IntStream;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
public class SampleLog4j2Application {

	private static final Logger logger = LoggerFactory.getLogger(SampleLog4j2Application.class);

	@Scheduled(fixedRate = 1000)
	public void logSomething() {
		IntStream.range(0, 100).forEach(i -> logger.debug("Sample Debug Message %s".formatted(i)));
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleLog4j2Application.class, args);
	}

}
