/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.context.properties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.annotation.AnnotationUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConfigurationPropertiesBindingOptions}.
 *
 * @author Stephane Nicoll
 */
public class ConfigurationPropertiesBindingOptionsTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void fromAnnotationWithNull() {
		this.thrown.expect(IllegalArgumentException.class);
		ConfigurationPropertiesBindingOptions.fromAnnotation(null);
	}

	@Test
	public void fromAnnotation() {
		ConfigurationPropertiesBindingOptions bindingOptions = ConfigurationPropertiesBindingOptions.fromAnnotation(
				AnnotationUtils.findAnnotation(SampleConfigurationProperties.class,
						ConfigurationProperties.class));
		assertThat(bindingOptions).isNotNull();
		assertThat(bindingOptions.getPrefix()).isEqualTo("test");
		assertThat(bindingOptions.isIgnoreUnknownFields()).isFalse();
		assertThat(bindingOptions.isIgnoreInvalidFields()).isFalse();
	}

	@ConfigurationProperties(prefix = "test", ignoreUnknownFields = false)
	private static class SampleConfigurationProperties {

	}

}
