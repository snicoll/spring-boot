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

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;
import org.springframework.util.ClassUtils;
import org.springframework.validation.Validator;

/**
 * Builder for creating {@link ConfigurationPropertiesBinder} based on the state of
 * the {@link ApplicationContext}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class ConfigurationPropertiesBinderBuilder {

	/**
	 * The bean name of the configuration properties validator.
	 */
	public static final String VALIDATOR_BEAN_NAME = "configurationPropertiesValidator";

	private static final String[] VALIDATOR_CLASSES = { "javax.validation.Validator",
			"javax.validation.ValidatorFactory" };

	private final ApplicationContext applicationContext;

	private ConversionService conversionService;

	private Validator validator;

	private Iterable<PropertySource<?>> propertySources;

	public ConfigurationPropertiesBinderBuilder(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public ConfigurationPropertiesBinderBuilder withConversionService(
			ConversionService conversionService) {
		this.conversionService = conversionService;
		return this;
	}

	public ConfigurationPropertiesBinderBuilder withValidator(Validator validator) {
		this.validator = validator;
		return this;
	}

	public ConfigurationPropertiesBinderBuilder withPropertySources(
			Iterable<PropertySource<?>> propertySources) {
		this.propertySources = propertySources;
		return this;
	}

	public ConfigurationPropertiesBinderBuilder withEnvironment(
			ConfigurableEnvironment environment) {
		return withPropertySources(environment.getPropertySources());
	}


	public ConfigurationPropertiesBinder build() {
		return new ConfigurationPropertiesBinder(determineValidator(),
				determineConversionService(), this.propertySources);
	}

	private Validator determineValidator() {
		if (this.validator != null) {
			return this.validator;
		}
		Validator defaultValidator = getOptionalBean(VALIDATOR_BEAN_NAME, Validator.class);
		if (defaultValidator != null) {
			return defaultValidator;
		}
		if (isJsr303Present()) {
			return new ValidatedLocalValidatorFactoryBean(this.applicationContext);
		}
		return null;
	}

	private ConversionService determineConversionService() {
		if (this.conversionService != null) {
			return this.conversionService;
		}
		ConversionService conversionServiceByName = getOptionalBean(
				ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME,
				ConversionService.class);
		if (conversionServiceByName != null) {
			return conversionServiceByName;
		}
		return createDefaultConversionService();
	}

	private ConversionService createDefaultConversionService() {
		ConversionServiceFactory conversionServiceFactory = new ConversionServiceFactory();
		this.applicationContext.getAutowireCapableBeanFactory()
				.autowireBean(conversionServiceFactory);
		return conversionServiceFactory.createConversionService();
	}

	private boolean isJsr303Present() {
		for (String validatorClass : VALIDATOR_CLASSES) {
			if (!ClassUtils.isPresent(validatorClass,
					this.applicationContext.getClassLoader())) {
				return false;
			}
		}
		return true;
	}

	private <T> T getOptionalBean(String name, Class<T> type) {
		try {
			return this.applicationContext.getBean(name, type);
		}
		catch (NoSuchBeanDefinitionException ex) {
			return null;
		}
	}

	private static class ConversionServiceFactory {

		private List<Converter<?, ?>> converters = Collections.emptyList();

		private List<GenericConverter> genericConverters = Collections.emptyList();

		/**
		 * A list of custom converters (in addition to the defaults) to use when
		 * converting properties for binding.
		 * @param converters the converters to set
		 */
		@Autowired(required = false)
		@ConfigurationPropertiesBinding
		public void setConverters(List<Converter<?, ?>> converters) {
			this.converters = converters;
		}

		/**
		 * A list of custom converters (in addition to the defaults) to use when
		 * converting properties for binding.
		 * @param converters the converters to set
		 */
		@Autowired(required = false)
		@ConfigurationPropertiesBinding
		public void setGenericConverters(List<GenericConverter> converters) {
			this.genericConverters = converters;
		}

		public ConversionService createConversionService() {
			DefaultConversionService conversionService = new DefaultConversionService();
			for (Converter<?, ?> converter : this.converters) {
				conversionService.addConverter(converter);
			}
			for (GenericConverter genericConverter : this.genericConverters) {
				conversionService.addConverter(genericConverter);
			}
			return conversionService;
		}

	}

}
