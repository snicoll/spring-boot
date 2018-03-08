/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.context.annotation.DeferredImportSelector;
import org.springframework.core.Ordered;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

/**
 * A {@link DeferredImportSelector} that provides a consolidated set of the
 * auto-configurations to import for the context.
 *
 * @author Stephane Nicoll
 * @since 2.0.1
 */
public class AutoConfigurationDeferredImportSelector
		implements BeanFactoryAware, Ordered, DeferredImportSelector {

	private BeanFactory beanFactory;

	private boolean called;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public String[] selectImports(AnnotationMetadata annotationMetadata) {
		if (!this.called) {
			this.called = true;
			return StringUtils.toStringArray(determineAutoConfigurations());
		}
		return new String[0];
	}

	private List<String> determineAutoConfigurations() {
		if (this.beanFactory.containsBean(AutoConfigurationImportSelector.REGISTRAR_BEAN_NAME)) {
			return this.beanFactory.getBean(AutoConfigurationImportSelector.REGISTRAR_BEAN_NAME,
					AutoConfigurationImportSelector.Registrar.class).determineImports();
		}
		return Collections.emptyList();
	}

}
