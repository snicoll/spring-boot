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

package org.springframework.boot.endpoint;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.MethodIntrospector.MetadataLookup;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

/**
 * An {@link EndpointDiscoverer} is used to discover endpoint beans in an application
 * context.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class EndpointDiscoverer {

	private final ApplicationContext applicationContext;

	/**
	 * Creates a new {@link EndpointDiscoverer} that will discover endpoint beans in the
	 * given {@code applicationContext}.
	 * @param applicationContext the application context to examine
	 */
	public EndpointDiscoverer(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	/**
	 * Perform endpoint discovery.
	 * @param annotationType the class-level annotation to look for
	 * @param operationFactory the factory for creating {@link EndpointOperation endpoint
	 * operations}
	 * @param <T> the concrete endpoint operation type
	 * @return the list of {@link EndpointInfo EndpointInfos} that describes the
	 * discovered endpoints
	 */
	public <T extends EndpointOperation> Collection<EndpointInfo<T>> discoverEndpoints(
			Class<? extends Annotation> annotationType,
			EndpointOperationFactory<T> operationFactory) {
		String[] endpointBeanNames = this.applicationContext
				.getBeanNamesForAnnotation(annotationType);
		Map<String, EndpointInfo<T>> endpointsById = new HashMap<>();
		for (String beanName : endpointBeanNames) {
			Class<?> beanType = this.applicationContext.getType(beanName);
			AnnotationAttributes endpointAttributes = AnnotatedElementUtils
					.getMergedAnnotationAttributes(beanType, annotationType);
			Map<Method, T> operationMethods = MethodIntrospector.selectMethods(beanType,
					(MetadataLookup<T>) (method) -> {
						T readOperation = createReadOperationIfPossible(beanName,
								endpointAttributes, method, operationFactory);
						if (readOperation != null) {
							return readOperation;
						}
						else {
							return createWriteOperationIfPossible(beanName,
									endpointAttributes, method, operationFactory);
						}
					});
			EndpointInfo<T> endpointInfo = new EndpointInfo<T>(
					endpointAttributes.getString("id"), operationMethods.values());
			EndpointInfo<T> previous = endpointsById.putIfAbsent(endpointInfo.getId(),
					endpointInfo);
			if (previous != null) {
				throw new IllegalStateException(
						"Found two endpoints with the id '" + endpointInfo.getId() + "': "
								+ endpointInfo + " and " + previous);
			}
		}
		return Collections.unmodifiableCollection(endpointsById.values());
	}

	private <T extends EndpointOperation> T createReadOperationIfPossible(String beanName,
			AnnotationAttributes endpointAttributes, Method method,
			EndpointOperationFactory<T> operationFactory) {
		return createOperationIfPossible(beanName, endpointAttributes, method,
				ReadOperation.class, EndpointOperationType.READ, operationFactory);
	}

	private <T extends EndpointOperation> T createWriteOperationIfPossible(
			String beanName, AnnotationAttributes endpointAttributes, Method method,
			EndpointOperationFactory<T> operationFactory) {
		return createOperationIfPossible(beanName, endpointAttributes, method,
				WriteOperation.class, EndpointOperationType.WRITE, operationFactory);
	}

	private <T extends EndpointOperation> T createOperationIfPossible(String beanName,
			AnnotationAttributes endpointAttributes, Method method,
			Class<? extends Annotation> operationAnnotation,
			EndpointOperationType operationType,
			EndpointOperationFactory<T> operationFactory) {
		AnnotationAttributes operationAttributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(method, operationAnnotation);
		if (operationAttributes == null) {
			return null;
		}
		return operationFactory.createOperation(endpointAttributes, operationAttributes,
				this.applicationContext.getBean(beanName), method, operationType);
	}

	/**
	 * An {@code EndpointOperationFactory} creates an {@link EndpointOperation} for an
	 * operation on an endpoint.
	 *
	 * @param <T> the {@link EndpointOperation} type
	 */
	@FunctionalInterface
	public interface EndpointOperationFactory<T extends EndpointOperation> {

		/**
		 * Creates an {@code EndpointOperation} for an operation on an endpoint.
		 * @param endpointAttributes the annotation attributes for the endpoint
		 * @param operationAttributes the annotation attributes for the operation
		 * @param target the target that implements the operation
		 * @param operationMethod the method on the bean that implements the operation
		 * @param operationType the type of the operation
		 * @return the operation info that describes the operation
		 */
		T createOperation(AnnotationAttributes endpointAttributes,
				AnnotationAttributes operationAttributes, Object target,
				Method operationMethod, EndpointOperationType operationType);

	}

}
