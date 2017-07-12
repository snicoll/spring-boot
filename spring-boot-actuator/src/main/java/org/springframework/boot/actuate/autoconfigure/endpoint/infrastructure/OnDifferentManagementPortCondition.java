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

package org.springframework.boot.actuate.autoconfigure.endpoint.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * A {@link SpringBootCondition} that matches when the management port and server port are
 * different.
 *
 * @author Andy Wilkinson
 */
class OnDifferentManagementPortCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		ConditionMessage.Builder message = ConditionMessage
				.forCondition("Different Management Port");
		ManagementPortType type = ManagementPortType.get(context.getEnvironment());
		if (type == ManagementPortType.DIFFERENT) {
			return ConditionOutcome
					.match(message.because("management port is different"));
		}
		return ConditionOutcome
				.noMatch(message.because("management port is not different"));
	}

}
