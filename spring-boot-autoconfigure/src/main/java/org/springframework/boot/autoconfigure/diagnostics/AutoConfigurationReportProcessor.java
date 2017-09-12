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

package org.springframework.boot.autoconfigure.diagnostics;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.context.annotation.Bean;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

/**
 *
 * @author Stephane Nicoll
 */
public class AutoConfigurationReportProcessor {

	private MetadataReaderFactory metadataReaderFactory;

	private ConditionEvaluationReport report;

	public AutoConfigurationReportProcessor(ConfigurableListableBeanFactory beanFactory) {
		this.metadataReaderFactory = new CachingMetadataReaderFactory(
				beanFactory.getBeanClassLoader());
		this.report = ConditionEvaluationReport.get(beanFactory);
	}

	public final ConditionEvaluationReport getReport() {
		return this.report;
	}

	/**
	 * Return the {@link AutoConfigurationEntry entries} matching the specified
	 * {@link AutoConfigurationEntryFilter filter}.
	 * @param filter the filter to use
	 * @return the entries matching the filter
	 */
	public Collection<AutoConfigurationEntry> getAutoConfigurationEntries(
			AutoConfigurationEntryFilter filter) {
		List<AutoConfigurationEntry> results = new ArrayList<>();
		collectReportedConditionOutcomes(filter, results);
		collectExcludedAutoConfiguration(filter, results);
		return results;
	}

	private void collectReportedConditionOutcomes(AutoConfigurationEntryFilter filter,
			List<AutoConfigurationEntry> results) {
		for (Map.Entry<String, ConditionAndOutcomes> entry : this.report
				.getConditionAndOutcomesBySource().entrySet()) {
			Source source = new Source(entry.getKey());
			ConditionAndOutcomes conditionAndOutcomes = entry.getValue();
			if (!conditionAndOutcomes.isFullMatch()) {
				BeanMethods methods = new BeanMethods(source, filter);
				for (ConditionAndOutcome conditionAndOutcome : conditionAndOutcomes) {
					if (!conditionAndOutcome.getOutcome().isMatch()) {
						for (MethodMetadata method : methods) {
							results.add(new AutoConfigurationEntry(method,
									conditionAndOutcome.getOutcome(), source.isMethod()));
						}
					}
				}
			}
		}
	}

	private void collectExcludedAutoConfiguration(AutoConfigurationEntryFilter filter,
			List<AutoConfigurationEntry> results) {
		for (String excludedClass : this.report.getExclusions()) {
			Source source = new Source(excludedClass);
			BeanMethods methods = new BeanMethods(source, filter);
			for (MethodMetadata method : methods) {
				String message = String.format("auto-configuration '%s' was excluded",
						ClassUtils.getShortName(excludedClass));
				results.add(new AutoConfigurationEntry(method,
						new ConditionOutcome(false, message), false));
			}
		}
	}

	private static class Source {

		private final String className;

		private final String methodName;

		Source(String source) {
			String[] tokens = source.split("#");
			this.className = (tokens.length > 1 ? tokens[0] : source);
			this.methodName = (tokens.length == 2 ? tokens[1] : null);
		}

		public String getClassName() {
			return this.className;
		}

		public String getMethodName() {
			return this.methodName;
		}

		public boolean isMethod() {
			return this.methodName != null;
		}

	}

	private class BeanMethods implements Iterable<MethodMetadata> {


		private final List<MethodMetadata> methods;

		BeanMethods(Source source, AutoConfigurationEntryFilter filter) {
			this.methods = findBeanMethods(source, filter);
		}

		private List<MethodMetadata> findBeanMethods(Source source,
				AutoConfigurationEntryFilter filter) {
			try {
				MetadataReader classMetadata = AutoConfigurationReportProcessor.this.metadataReaderFactory
						.getMetadataReader(source.getClassName());
				Set<MethodMetadata> candidates = classMetadata.getAnnotationMetadata()
						.getAnnotatedMethods(Bean.class.getName());
				List<MethodMetadata> result = new ArrayList<>();
				for (MethodMetadata candidate : candidates) {
					if (isMatch(candidate, source, filter)) {
						result.add(candidate);
					}
				}
				return Collections.unmodifiableList(result);
			}
			catch (Exception ex) {
				return Collections.emptyList();
			}
		}

		private boolean isMatch(MethodMetadata candidate, Source source,
				AutoConfigurationEntryFilter filter) {
			if (source.getMethodName() != null
					&& !source.getMethodName().equals(candidate.getMethodName())) {
				return false;
			}
			return filter.match(candidate);
		}

		@Override
		public Iterator<MethodMetadata> iterator() {
			return this.methods.iterator();
		}

	}

}
