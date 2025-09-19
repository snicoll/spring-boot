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

package smoketest.aot.web;

import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class SampleAotNextWebApplicationTests {

	@Test
	void refreshForAotProcessingAsAtLeastTheSameBeanAsRegularRuntime() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(SampleAotNextWebApplication.class);
		refreshForAotProcessing(context);

		AnnotationConfigApplicationContext strictContext = new AnnotationConfigApplicationContext();
		context.register(SampleAotNextWebApplication.class);
		refreshForAotProcessingStrict(strictContext);

		assertThat(context.getBeanDefinitionNames()).containsAll(Arrays.asList(strictContext.getBeanDefinitionNames()));
	}

	private void refreshForAotProcessing(AnnotationConfigApplicationContext context) {
		context.setBeanNameGenerator(new AotBeanNameGenerator(context));
		context.refreshForAotProcessing(new RuntimeHints());

	}

	private void refreshForAotProcessingStrict(AnnotationConfigApplicationContext context) {
		context.setBeanNameGenerator(new AotBeanNameGenerator(context));
		context.getEnvironment()
			.getPropertySources()
			.addFirst(new MapPropertySource("AOT", Map.of("spring.aot.condition-evaluation", "true")));
		context.refreshForAotProcessing(new RuntimeHints());
	}

}
