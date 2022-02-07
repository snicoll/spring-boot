/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.diagnostics;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.SpringBootExceptionReporter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.core.io.support.SpringFactoriesLoader.ArgumentResolver;
import org.springframework.core.io.support.SpringFactoriesLoader.FailureHandler;
import org.springframework.core.log.LogMessage;
import org.springframework.util.StringUtils;

/**
 * Utility to trigger {@link FailureAnalyzer} and {@link FailureAnalysisReporter}
 * instances loaded from {@code spring.factories}.
 * <p>
 * A {@code FailureAnalyzer} that requires access to the {@link BeanFactory} or
 * {@link Environment} in order to perform its analysis can implement a constructor that
 * accepts arguments of one or both of these types.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
final class FailureAnalyzers implements SpringBootExceptionReporter {

	private static final Log logger = LogFactory.getLog(FailureAnalyzers.class);

	private final ClassLoader classLoader;

	private final List<FailureAnalyzer> analyzers;

	FailureAnalyzers(ConfigurableApplicationContext context) {
		this.classLoader = (context != null) ? context.getClassLoader() : null;
		this.analyzers = loadFailureAnalyzers(context, this.classLoader);
	}

	private static ClassLoader getClassLoader(ConfigurableApplicationContext context) {
		return (context != null) ? context.getClassLoader() : null;
	}

	private static List<FailureAnalyzer> loadFailureAnalyzers(ConfigurableApplicationContext context,
			ClassLoader classLoader) {
		ArgumentResolver argumentResolver = (context != null) ? ArgumentResolver
				.of(BeanFactory.class, context.getBeanFactory()).and(Environment.class, context.getEnvironment())
				: null;
		List<FailureAnalyzer> analyzers = SpringFactoriesLoader.loadFactories(FailureAnalyzer.class, classLoader,
				argumentResolver, FailureHandler.logging(logger));
		List<FailureAnalyzer> awareAnalyzers = analyzers.stream()
				.filter((analyzer) -> analyzer instanceof BeanFactoryAware || analyzer instanceof EnvironmentAware)
				.toList();
		if (!awareAnalyzers.isEmpty()) {
			String awareAnalyzerNames = StringUtils.collectionToCommaDelimitedString(awareAnalyzers.stream()
					.map((analyzer) -> analyzer.getClass().getName()).collect(Collectors.toList()));
			logger.warn(LogMessage.format(
					"FailureAnalyzers [%s] implement BeanFactoryAware or EnvironmentAware."
							+ "Support for these interfaces on FailureAnalyzers is deprecated, "
							+ "and will be removed in a future release."
							+ "Instead provide a constructor that accepts BeanFactory or Environment parameters.",
					awareAnalyzerNames));
			if (context == null) {
				logger.trace(LogMessage.format("Skipping [%s] due to missing context", awareAnalyzerNames));
				return analyzers.stream().filter((analyzer) -> !awareAnalyzers.contains(analyzer))
						.collect(Collectors.toList());
			}
			awareAnalyzers.forEach((analyzer) -> {
				if (analyzer instanceof BeanFactoryAware) {
					((BeanFactoryAware) analyzer).setBeanFactory(context.getBeanFactory());
				}
				if (analyzer instanceof EnvironmentAware) {
					((EnvironmentAware) analyzer).setEnvironment(context.getEnvironment());
				}
			});
		}
		return analyzers;
	}

	@Override
	public boolean reportException(Throwable failure) {
		FailureAnalysis analysis = analyze(failure, this.analyzers);
		return report(analysis, this.classLoader);
	}

	private FailureAnalysis analyze(Throwable failure, List<FailureAnalyzer> analyzers) {
		for (FailureAnalyzer analyzer : analyzers) {
			try {
				FailureAnalysis analysis = analyzer.analyze(failure);
				if (analysis != null) {
					return analysis;
				}
			}
			catch (Throwable ex) {
				logger.trace(LogMessage.format("FailureAnalyzer %s failed", analyzer), ex);
			}
		}
		return null;
	}

	private boolean report(FailureAnalysis analysis, ClassLoader classLoader) {
		List<FailureAnalysisReporter> reporters = SpringFactoriesLoader.loadFactories(FailureAnalysisReporter.class,
				classLoader);
		if (analysis == null || reporters.isEmpty()) {
			return false;
		}
		for (FailureAnalysisReporter reporter : reporters) {
			reporter.report(analysis);
		}
		return true;
	}

}
