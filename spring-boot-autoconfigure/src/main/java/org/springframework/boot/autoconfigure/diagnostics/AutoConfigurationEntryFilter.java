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

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ClassUtils;

/**
 *
 * @author Stephane Nicoll
 */
public interface AutoConfigurationEntryFilter {

	boolean match(MethodMetadata candidate);

	static boolean hasMatchingBeanName(MethodMetadata methodMetadata, String beanName) {
		Map<String, Object> attributes = methodMetadata
				.getAnnotationAttributes(Bean.class.getName());
		String[] candidates = (attributes == null ? null
				: (String[]) attributes.get("name"));
		if (candidates != null) {
			for (String candidate : candidates) {
				if (candidate.equals(beanName)) {
					return true;
				}
			}
			return false;
		}
		return methodMetadata.getMethodName().equals(beanName);
	}

	static boolean hasMatchingType(MethodMetadata candidate, Class<?> type,
			ClassLoader classLoader) {
		String returnTypeName = candidate.getReturnTypeName();
		if (type.getName().equals(returnTypeName)) {
			return true;
		}
		try {
			Class<?> returnType = ClassUtils.forName(returnTypeName, classLoader);
			return type.isAssignableFrom(returnType);
		}
		catch (Throwable ex) {
			return false;
		}
	}

}
