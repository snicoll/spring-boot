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

package org.springframework.boot.loader.tools.layer.classes;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NameFilter}.
 *
 * @author Madhura Bhave
 * @author Stephane Nicoll
 */
class NameFilterTests {

	@Test
	void isResourceIncludedWhenPatternMatches() {
		NameFilter filter = new NameFilter(Collections.singletonList("application*.*"), Collections.emptyList());
		assertThat(filter.isResourceIncluded("application.yml")).isTrue();
	}

	@Test
	void isResourceIncludedWhenPatternDoesNotMatch() {
		NameFilter filter = new NameFilter(Collections.singletonList("other.yml"), Collections.emptyList());
		assertThat(filter.isResourceIncluded("application.yml")).isFalse();
	}

	@Test
	void isResourceExcludedWhenPatternMatches() {
		NameFilter filter = new NameFilter(Collections.emptyList(), Collections.singletonList("application*.*"));
		assertThat(filter.isResourceExcluded("application.yml")).isTrue();
	}

	@Test
	void isResourceExcludedWhenPatternDoesNotMatch() {
		NameFilter filter = new NameFilter(Collections.emptyList(), Collections.singletonList("other.yml"));
		assertThat(filter.isResourceExcluded("application.yml")).isFalse();
	}

}
