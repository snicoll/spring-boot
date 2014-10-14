/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.config.sample.specific;

import org.springframework.boot.context.properties.ConfigurationItem;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Demonstrate the auto-detection of a nested config class as a group.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "config")
public class InnerClassConfig {

	private final Foo first = new Foo();

	private Foo second = new Foo();

	private final SimplePojo third = new SimplePojo();

	private final SimplePojo fourth = new SimplePojo();

	@ConfigurationItem(nested = true)
	private final SimplePojo fifth = new SimplePojo();

	public Foo getFirst() {
		return first;
	}

	@ConfigurationItem
	public Foo getSecond() {
		return second;
	}

	public void setSecond(Foo second) {
		this.second = second;
	}

	public SimplePojo getThird() {
		return third;
	}

	@ConfigurationItem(nested = true)
	public SimplePojo getFourth() {
		return fourth;
	}

	public SimplePojo getFifth() {
		return fifth;
	}

	public static class Foo {

		private String name;

		private final Bar bar = new Bar();

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Bar getBar() {
			return bar;
		}

		public static class Bar {

			private String name;

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}
		}
	}
}
