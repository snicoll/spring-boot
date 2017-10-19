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

import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;

/**
 * The {@link ConfigurationPropertiesBinder} context.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see ConfigurationPropertiesBinderFactory
 */
public final class ConfigurationPropertiesBindingContext {

	private final Iterable<PropertySource<?>> propertySources;

	private final ConversionService conversionService;

	private final Validator validator;

	public ConfigurationPropertiesBindingContext(Iterable<PropertySource<?>> propertySources,
			ConversionService conversionService, Validator validator) {
		Assert.notNull(propertySources, "PropertySources must not be null");
		this.propertySources = propertySources;
		this.conversionService = conversionService;
		this.validator = validator;
	}

	/**
	 * Return the {@link PropertySource property sources} to use.
	 * @return the property sources
	 */
	public Iterable<PropertySource<?>> getPropertySources() {
		return this.propertySources;
	}

	/**
	 * Return the {@link ConversionService} to use for binding or {@code null} if
	 * no {@link ConversionService} is available.
	 * @return the conversion service
	 */
	public ConversionService getConversionService() {
		return this.conversionService;
	}

	/**
	 * Return the {@link Validator} to use for binding or {@code null} if
	 * no {@link Validator} is available.
	 * @return the validator
	 */
	public Validator getValidator() {
		return this.validator;
	}

}
