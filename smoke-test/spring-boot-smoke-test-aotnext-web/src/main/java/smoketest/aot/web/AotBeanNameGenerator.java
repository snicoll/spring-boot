/*
 * Copyright 2012-present the original author or authors.
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

package smoketest.aot.web;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.FullyQualifiedConfigurationBeanNameGenerator;
import org.springframework.core.type.MethodMetadata;

public class AotBeanNameGenerator extends FullyQualifiedConfigurationBeanNameGenerator {

	private final BeanDefinitionRegistry registry;

	public AotBeanNameGenerator(BeanDefinitionRegistry registry) {
		this.registry = registry;
	}

	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		String name = buildDefaultBeanName(definition, registry);
		return generateUniqueBeanName(name);
	}

	@Override
	public String deriveBeanName(MethodMetadata beanMethod) {
		return generateUniqueBeanName(super.deriveBeanName(beanMethod));
	}

	private String generateUniqueBeanName(String candidate) {
		if (!this.registry.containsBeanDefinition(candidate)) {
			return candidate;
		}
		return generateUniqueBeanName(candidate + "_");
	}

}
