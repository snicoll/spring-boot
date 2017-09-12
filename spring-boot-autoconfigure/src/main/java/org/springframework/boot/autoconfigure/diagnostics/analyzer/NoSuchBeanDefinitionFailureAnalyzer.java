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

package org.springframework.boot.autoconfigure.diagnostics.analyzer;

import java.util.Collection;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.diagnostics.AutoConfigurationEntry;
import org.springframework.boot.autoconfigure.diagnostics.AutoConfigurationEntryFilter;
import org.springframework.boot.autoconfigure.diagnostics.AutoConfigurationReportProcessor;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.analyzer.AbstractInjectionFailureAnalyzer;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;

/**
 * An {@link AbstractInjectionFailureAnalyzer} that performs analysis of failures caused
 * by a {@link NoSuchBeanDefinitionException}.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class NoSuchBeanDefinitionFailureAnalyzer
		extends AbstractInjectionFailureAnalyzer<NoSuchBeanDefinitionException>
		implements BeanFactoryAware {

	private ConfigurableListableBeanFactory beanFactory;

	private AutoConfigurationReportProcessor processor;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory);
		this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		// Get early as won't be accessible once context has failed to start
		this.processor = new AutoConfigurationReportProcessor(this.beanFactory);
	}

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			NoSuchBeanDefinitionException cause, String description) {
		if (cause.getNumberOfBeansFound() != 0) {
			return null;
		}
		Collection<AutoConfigurationEntry> autoConfigurationResults = getAutoConfigurationEntries(
				cause);
		StringBuilder message = new StringBuilder();
		message.append(String.format("%s required %s that could not be found.%n",
				description == null ? "A component" : description,
				getBeanDescription(cause)));
		if (!autoConfigurationResults.isEmpty()) {
			for (AutoConfigurationEntry entry : autoConfigurationResults) {
				message.append(String.format("\t- %s%n", entry));
			}
		}
		String action = String.format("Consider %s %s in your configuration.",
				(!autoConfigurationResults.isEmpty()
						? "revisiting the conditions above or defining" : "defining"),
				getBeanDescription(cause));
		return new FailureAnalysis(message.toString(), action, cause);
	}

	private String getBeanDescription(NoSuchBeanDefinitionException cause) {
		if (cause.getResolvableType() != null) {
			Class<?> type = extractBeanType(cause.getResolvableType());
			return "a bean of type '" + type.getName() + "'";
		}
		return "a bean named '" + cause.getBeanName() + "'";
	}

	private static Class<?> extractBeanType(ResolvableType resolvableType) {
		ResolvableType collectionType = resolvableType.asCollection();
		if (!collectionType.equals(ResolvableType.NONE)) {
			return collectionType.getGeneric(0).getRawClass();
		}
		ResolvableType mapType = resolvableType.asMap();
		if (!mapType.equals(ResolvableType.NONE)) {
			return mapType.getGeneric(1).getRawClass();
		}
		return resolvableType.getRawClass();
	}

	private Collection<AutoConfigurationEntry> getAutoConfigurationEntries(
			NoSuchBeanDefinitionException cause) {
		AutoConfigurationEntryFilter filter = new NoSuchBeanAutoConfigurationEntryFilter(cause,
				this.beanFactory.getBeanClassLoader());
		return this.processor.getAutoConfigurationEntries(filter);
	}

	private static class NoSuchBeanAutoConfigurationEntryFilter implements AutoConfigurationEntryFilter {

		private final NoSuchBeanDefinitionException cause;

		private final ClassLoader classLoader;

		NoSuchBeanAutoConfigurationEntryFilter(NoSuchBeanDefinitionException cause,
				ClassLoader classLoader) {
			this.cause = cause;
			this.classLoader = classLoader;
		}

		@Override
		public boolean match(MethodMetadata candidate) {
			return (hasMatchingName(candidate, this.cause.getBeanName())
					|| hasMatchingType(candidate, this.cause.getResolvableType()));
		}

		private boolean hasMatchingName(MethodMetadata candidate, String name) {
			return (name != null
					&& AutoConfigurationEntryFilter.hasMatchingBeanName(candidate, name));
		}

		private boolean hasMatchingType(MethodMetadata candidate,
				ResolvableType resolvableType) {
			if (resolvableType != null) {
				Class<?> type = extractBeanType(resolvableType);
				return AutoConfigurationEntryFilter.hasMatchingType(candidate, type, this.classLoader);
			}
			return false;
		}


	}

}
