/*
 * Copyright 2021-2021 the original author or authors.
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

package org.springframework.boot.actuate.info.java;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.info.Info;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link JavaInfoContributor}
 *
 * @author Jonatan Ivanov
 */
class JavaInfoContributorTests {

	@Test
	void javaInfoShouldBeAdded() {
		JavaInfoContributor javaInfoContributor = new JavaInfoContributor();
		Info.Builder builder = new Info.Builder();
		javaInfoContributor.contribute(builder);

		assertThat(builder.build().getDetails().get("java")).isInstanceOf(JavaInfo.class);
		JavaInfo javaInfo = (JavaInfo) builder.build().getDetails().get("java");

		assertThat(javaInfo.getVendor()).isEqualTo(System.getProperty("java.vendor"));
		assertThat(javaInfo.getVersion()).isEqualTo(System.getProperty("java.version"));
		assertThat(javaInfo.getRuntime().getName()).isEqualTo(System.getProperty("java.runtime.name"));
		assertThat(javaInfo.getRuntime().getVersion()).isEqualTo(System.getProperty("java.runtime.version"));
		assertThat(javaInfo.getVm().getName()).isEqualTo(System.getProperty("java.vm.name"));
		assertThat(javaInfo.getVm().getVendor()).isEqualTo(System.getProperty("java.vm.vendor"));
		assertThat(javaInfo.getVm().getVersion()).isEqualTo(System.getProperty("java.vm.version"));
	}

}
