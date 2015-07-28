/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link Condition} that checks if a health indicator is enabled.
 *
 * @author Stephane Nicoll
 * @since 1.3
 */
class OnHealthIndicatorEnabledCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
		AnnotationAttributes annotationAttributes = AnnotationAttributes.fromMap(metadata
				.getAnnotationAttributes(ConditionalOnHealthIndicatorEnabled.class.getName()));

		String value = annotationAttributes.getString("value").trim();
		PropertyResolver specificResolver =
				new RelaxedPropertyResolver(context.getEnvironment(), "management.health." + value + ".");
		Boolean enabled = specificResolver.getProperty("enabled", Boolean.class);
		if (enabled != null) {
			if (Boolean.TRUE.equals(enabled)) {
				return ConditionOutcome.match("Specific enabled flag for " + value + " is enabled.");
			}
			else {
				return ConditionOutcome.noMatch("Specific enabled flag for " + value + " is disabled.");
			}
		}

		PropertyResolver defaultResolver =
				new RelaxedPropertyResolver(context.getEnvironment(), "management.health.");
		Boolean defaultEnabled = defaultResolver.getProperty("enabled", Boolean.class);
		if (defaultEnabled != null) {
			if (Boolean.TRUE.equals(defaultEnabled)) {
				return ConditionOutcome.match("Default enabled flag for " + value + " is enabled.");
			}
			else {
				return ConditionOutcome.noMatch("Default enabled flag for " + value + " is disabled.");
			}
		}

		return ConditionOutcome.match("No specific configuration for " + value + " health indicator.");
	}

}
