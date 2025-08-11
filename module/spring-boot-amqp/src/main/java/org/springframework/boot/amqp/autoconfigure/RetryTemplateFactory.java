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

package org.springframework.boot.amqp.autoconfigure;

import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

/**
 * Factory to create {@link RetryTemplate} instance from properties defined in
 * {@link RabbitProperties}.
 *
 * @author Stephane Nicoll
 */
class RetryTemplateFactory {

	private final @Nullable List<RabbitRetryTemplateCustomizer> customizers;

	RetryTemplateFactory(@Nullable List<RabbitRetryTemplateCustomizer> customizers) {
		this.customizers = customizers;
	}

	RetryTemplate createRetryTemplate(RabbitProperties.Retry properties, RabbitRetryTemplateCustomizer.Target target) {
		PropertyMapper map = PropertyMapper.get();
		RetryTemplate template = new RetryTemplate();
		RetryPolicy.Builder builder = RetryPolicy.builder();
		map.from(properties::getInitialInterval).whenNonNull().to(builder::delay);
		map.from(properties::getMultiplier).to(builder::multiplier);
		map.from(properties::getMaxInterval).whenNonNull().to(builder::maxDelay);
		RetryPolicy policy = builder.maxAttempts(properties.getMaxAttempts()).build();
		template.setRetryPolicy(policy);
		if (this.customizers != null) {
			for (RabbitRetryTemplateCustomizer customizer : this.customizers) {
				customizer.customize(target, template);
			}
		}
		return template;
	}

}
