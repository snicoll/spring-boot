/*
 * Copyright 2012-2019 the original author or authors.
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
package org.springframework.boot.context.properties.bind;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MockConfigurationPropertySource;
import org.springframework.boot.convert.Delimiter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConstructorParametersBinder}.
 *
 * @author Madhura Bhave
 */
public class ConstructorParametersBinderTests {

	private List<ConfigurationPropertySource> sources = new ArrayList<>();

	private Binder binder;

	@Before
	public void setup() {
		this.binder = new Binder(this.sources);
	}

	@Test
	public void bindToClassShouldCreateBoundBean() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "12");
		source.put("foo.long-value", "34");
		source.put("foo.string-value", "foo");
		this.sources.add(source);
		ExampleValueBean bean = this.binder
				.bind("foo", Bindable.of(ExampleValueBean.class)).get();
		assertThat(bean.getIntValue()).isEqualTo(12);
		assertThat(bean.getLongValue()).isEqualTo(34);
		assertThat(bean.getStringValue()).isEqualTo("foo");
	}

	@Test
	public void bindToClassWhenHasNoPrefixShouldCreateBoundBean() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("int-value", "12");
		source.put("long-value", "34");
		source.put("string-value", "foo");
		this.sources.add(source);
		ExampleValueBean bean = this.binder.bind(ConfigurationPropertyName.of(""),
				Bindable.of(ExampleValueBean.class)).get();
		assertThat(bean.getIntValue()).isEqualTo(12);
		assertThat(bean.getLongValue()).isEqualTo(34);
		assertThat(bean.getStringValue()).isEqualTo("foo");
	}

	@Test
	public void bindToClassWithMultipleConstructorsShouldNotBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "12");
		this.sources.add(source);
		boolean bound = this.binder
				.bind("foo", Bindable.of(MultipleConstructorsBean.class)).isBound();
		assertThat(bound).isFalse();
	}

	@Test
	public void bindToClassWithOnlyDefaultConstructorShouldNotBind() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.int-value", "12");
		this.sources.add(source);
		boolean bound = this.binder.bind("foo", Bindable.of(DefaultConstructorBean.class))
				.isBound();
		assertThat(bound).isFalse();
	}

	@Test
	public void bindToClassShouldBindNested() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.value-bean.int-value", "123");
		source.put("foo.value-bean.long-value", "34");
		source.put("foo.value-bean.string-value", "foo");
		this.sources.add(source);
		ExampleNestedBean bean = this.binder
				.bind("foo", Bindable.of(ExampleNestedBean.class)).get();
		assertThat(bean.getValueBean().getIntValue()).isEqualTo(123);
		assertThat(bean.getValueBean().getLongValue()).isEqualTo(34);
		assertThat(bean.getValueBean().getStringValue()).isEqualTo("foo");
	}

	@Test
	public void bindToClassWithNoValueForPrimitiveShouldUseZero() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.string-value", "foo");
		this.sources.add(source);
		ExampleValueBean bean = this.binder
				.bind("foo", Bindable.of(ExampleValueBean.class)).get();
		assertThat(bean.getIntValue()).isEqualTo(0);
		assertThat(bean.getLongValue()).isEqualTo(0);
		assertThat(bean.getStringValue()).isEqualTo("foo");
	}

	@Test
	public void bindtoClassWithDefaultValueShouldUseDefaultValue() {
		MockConfigurationPropertySource source = new MockConfigurationPropertySource();
		source.put("foo.string-value", "foo");
		this.sources.add(source);
		ExampleDefaultValuBean bean = this.binder
				.bind("foo", Bindable.of(ExampleDefaultValuBean.class)).get();
		assertThat(bean.getIntValue()).isEqualTo(5);
		assertThat(bean.getStringsList()).contains("a", "b", "c");
		assertThat(bean.getCustomList()).contains("x,y,z");
	}

	public static class ExampleValueBean {

		private final int intValue;

		private final long longValue;

		private final String stringValue;

		public ExampleValueBean(int intValue, long longValue, String stringValue) {
			this.intValue = intValue;
			this.longValue = longValue;
			this.stringValue = stringValue;
		}

		public int getIntValue() {
			return this.intValue;
		}

		public long getLongValue() {
			return this.longValue;
		}

		public String getStringValue() {
			return this.stringValue;
		}

	}

	public static class MultipleConstructorsBean {

		private final int intValue;

		private final long longValue;

		private final String stringValue;

		public MultipleConstructorsBean(int intValue) {
			this(intValue, 23L, "hello");
		}

		public MultipleConstructorsBean(int intValue, long longValue,
				String stringValue) {
			this.intValue = intValue;
			this.longValue = longValue;
			this.stringValue = stringValue;
		}

	}

	public static class DefaultConstructorBean {

		private final int intValue;

		private final long longValue;

		private final String stringValue;

		public DefaultConstructorBean() {
			this.intValue = 1;
			this.longValue = 10;
			this.stringValue = "hello";
		}

	}

	public static class ExampleNestedBean {

		private final ExampleValueBean valueBean;

		public ExampleNestedBean(ExampleValueBean valueBean) {
			this.valueBean = valueBean;
		}

		public ExampleValueBean getValueBean() {
			return this.valueBean;
		}

	}

	public static class ExampleDefaultValuBean {

		private final int intValue;

		private final List<String> stringsList;

		private final List<String> customList;

		public ExampleDefaultValuBean(@DefaultValue("5") int intValue,
				@DefaultValue("a,b,c") List<String> stringsList,
				@DefaultValue("x,y,z") @Delimiter(Delimiter.NONE) List<String> customList) {
			this.intValue = intValue;
			this.stringsList = stringsList;
			this.customList = customList;
		}

		public int getIntValue() {
			return this.intValue;
		}

		public List<String> getStringsList() {
			return this.stringsList;
		}

		public List<String> getCustomList() {
			return this.customList;
		}

	}

}
