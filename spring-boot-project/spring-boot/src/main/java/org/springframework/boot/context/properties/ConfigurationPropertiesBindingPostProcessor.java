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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;
import org.springframework.validation.Validator;

/**
 * {@link BeanPostProcessor} to bind {@link PropertySources} to beans annotated with
 * {@link ConfigurationProperties}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Madhura Bhave
 */
public class ConfigurationPropertiesBindingPostProcessor implements BeanPostProcessor,
		BeanFactoryAware, EnvironmentAware, ApplicationContextAware, InitializingBean,
		PriorityOrdered {

	/**
	 * The bean name to use to override the default {@link ConfigurationPropertiesBinderFactory}.
	 */
	public static final String FACTORY_BEAN_NAME = ConfigurationPropertiesBinderFactory.class.getName();

	private static final Log logger = LogFactory
			.getLog(ConfigurationPropertiesBindingPostProcessor.class);

	private ConfigurationBeanFactoryMetaData beans = new ConfigurationBeanFactoryMetaData();

	private Iterable<PropertySource<?>> propertySources;

	private Validator validator;

	private ConversionService conversionService;

	private BeanFactory beanFactory;

	private Environment environment = new StandardEnvironment();

	private ApplicationContext applicationContext;

	private int order = Ordered.HIGHEST_PRECEDENCE + 1;

	private ConfigurationPropertiesBinder configurationPropertiesBinder;

	/**
	 * Set the order of the bean.
	 * @param order the order
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Return the order of the bean.
	 * @return the order
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Set the property sources to bind.
	 * @param propertySources the property sources
	 */
	public void setPropertySources(Iterable<PropertySource<?>> propertySources) {
		this.propertySources = propertySources;
	}

	/**
	 * Set the bean validator used to validate property fields.
	 * @param validator the validator
	 */
	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	/**
	 * Set the conversion service used to convert property values.
	 * @param conversionService the conversion service
	 */
	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	/**
	 * Set the bean meta-data store.
	 * @param beans the bean meta data store
	 */
	public void setBeanMetaDataStore(ConfigurationBeanFactoryMetaData beans) {
		this.beans = beans;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.propertySources == null) {
			this.propertySources = deducePropertySources();
		}
	}

	private PropertySources deducePropertySources() {
		PropertySourcesPlaceholderConfigurer configurer = getSinglePropertySourcesPlaceholderConfigurer();
		if (configurer != null) {
			return configurer.getAppliedPropertySources();
		}
		if (this.environment instanceof ConfigurableEnvironment) {
			return ((ConfigurableEnvironment) this.environment).getPropertySources();
		}
		throw new IllegalStateException("Unable to obtain PropertySources from "
				+ "PropertySourcesPlaceholderConfigurer or Environment");
	}

	private PropertySourcesPlaceholderConfigurer getSinglePropertySourcesPlaceholderConfigurer() {
		// Take care not to cause early instantiation of all FactoryBeans
		if (this.beanFactory instanceof ListableBeanFactory) {
			ListableBeanFactory listableBeanFactory = (ListableBeanFactory) this.beanFactory;
			Map<String, PropertySourcesPlaceholderConfigurer> beans = listableBeanFactory
					.getBeansOfType(PropertySourcesPlaceholderConfigurer.class, false,
							false);
			if (beans.size() == 1) {
				return beans.values().iterator().next();
			}
			if (beans.size() > 1 && logger.isWarnEnabled()) {
				logger.warn("Multiple PropertySourcesPlaceholderConfigurer "
						+ "beans registered " + beans.keySet()
						+ ", falling back to Environment");
			}
		}
		return null;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName)
			throws BeansException {
		ConfigurationProperties annotation = getAnnotation(bean, beanName);
		if (annotation != null) {
			try {
				getBinder().bind(bean, ConfigurationPropertiesBindingOptions
						.fromAnnotation(annotation));
			}
			catch (ConfigurationPropertiesBindingException ex) {
				throw new BeanCreationException(beanName, ex.getMessage(), ex.getCause());
			}
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName)
			throws BeansException {
		return bean;
	}

	private ConfigurationProperties getAnnotation(Object bean, String beanName) {
		ConfigurationProperties annotation = this.beans.findFactoryAnnotation(beanName,
				ConfigurationProperties.class);
		if (annotation == null) {
			annotation = AnnotationUtils.findAnnotation(bean.getClass(),
					ConfigurationProperties.class);
		}
		return annotation;
	}

	private ConfigurationPropertiesBinder getBinder() {
		if (this.configurationPropertiesBinder == null) {
			ConfigurationPropertiesBindingContext bindingContext = createBindingContext();
			this.configurationPropertiesBinder = determineBinderFactory()
					.createBinder(bindingContext);
		}
		return this.configurationPropertiesBinder;
	}

	private ConfigurationPropertiesBindingContext createBindingContext() {
		return new ConfigurationPropertiesBindingContextBuilder(this.applicationContext)
						.withConversionService(this.conversionService)
						.withValidator(this.validator)
						.withPropertySources(this.propertySources).build();
	}

	private ConfigurationPropertiesBinderFactory determineBinderFactory() {
		if (this.applicationContext.containsBean(FACTORY_BEAN_NAME)) {
			Object builder = this.applicationContext.getBean(FACTORY_BEAN_NAME);
			Assert.state(builder instanceof ConfigurationPropertiesBinderFactory,
					String.format("Bean with name %s must be a %s but got %s",
							FACTORY_BEAN_NAME,
							ConfigurationPropertiesBinderFactory.class.getName(),
							builder.getClass().getName()));
			return (ConfigurationPropertiesBinderFactory) builder;
		}
		return new DefaultConfigurationPropertiesBinderFactory();
	}

}
