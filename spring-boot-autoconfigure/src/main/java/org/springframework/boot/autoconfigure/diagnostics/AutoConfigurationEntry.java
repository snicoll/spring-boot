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

import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ClassUtils;

/**
 * Represent an entry of the auto-configuration report.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 * @see AutoConfigurationReportProcessor
 */
public final class AutoConfigurationEntry {

	private final MethodMetadata methodMetadata;

	private final ConditionOutcome conditionOutcome;

	private final boolean methodEvaluated;

	AutoConfigurationEntry(MethodMetadata methodMetadata,
			ConditionOutcome conditionOutcome, boolean methodEvaluated) {
		this.methodMetadata = methodMetadata;
		this.conditionOutcome = conditionOutcome;
		this.methodEvaluated = methodEvaluated;
	}

	public MethodMetadata getMethodMetadata() {
		return this.methodMetadata;
	}

	public ConditionOutcome getConditionOutcome() {
		return this.conditionOutcome;
	}

	public boolean isMethodEvaluated() {
		return this.methodEvaluated;
	}

	@Override
	public String toString() {
		if (this.methodEvaluated) {
			return String.format("Bean method '%s' in '%s' not loaded because %s",
					this.methodMetadata.getMethodName(),
					ClassUtils.getShortName(
							this.methodMetadata.getDeclaringClassName()),
					this.conditionOutcome.getMessage());
		}
		return String.format("Bean method '%s' not loaded because %s",
				this.methodMetadata.getMethodName(),
				this.conditionOutcome.getMessage());
	}

}
