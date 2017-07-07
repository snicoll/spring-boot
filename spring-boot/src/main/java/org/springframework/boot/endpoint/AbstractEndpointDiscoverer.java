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
 * Base class for endpoint discoverer implementations.
 *
 * @param <T> the {@link EndpointOperationInfo} type
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public abstract class AbstractEndpointDiscoverer<T extends EndpointOperationInfo> {

	private final Class<? extends Annotation> annotationType;

	private final ApplicationContext applicationContext;

	private final OperationInfoFactory<T> operationInfoFactory;

	/**
	 * Creates a new {@link AbstractEndpointDiscoverer} that will discover endpoints
	 * annotated with the given {@code annotationType} in the given
	 * {@code applicationContext}.
	 * @param annotationType the type of the annotation used to identify endpoint beans
	 * @param applicationContext the application context to examine
	 * @param operationInfoFactory the factory used to create the descriptions of the
	 * endpoint's operations
	 */
	protected AbstractEndpointDiscoverer(Class<? extends Annotation> annotationType,
			ApplicationContext applicationContext,
			OperationInfoFactory<T> operationInfoFactory) {
		this.annotationType = annotationType;
		this.applicationContext = applicationContext;
		this.operationInfoFactory = operationInfoFactory;
	}

	/**
	 * Perform endpoint discovery.
	 * @return the list of {@link EndpointInfo EndpointInfos} that describes the
	 * discovered endpoints
	 */
	public Collection<EndpointInfo<T>> discoverEndpoints() {
		String[] endpointBeanNames = this.applicationContext
				.getBeanNamesForAnnotation(this.annotationType);
		Map<String, EndpointInfo<T>> endpointsById = new HashMap<>();
		for (String beanName : endpointBeanNames) {
			Class<?> beanType = this.applicationContext.getType(beanName);
			AnnotationAttributes endpointAttributes = AnnotatedElementUtils
					.getMergedAnnotationAttributes(beanType, this.annotationType);
			Map<Method, T> operationMethods = MethodIntrospector.selectMethods(beanType,
					(MetadataLookup<T>) (method) -> {
						T readOperation = createReadOperationIfPossible(beanName,
								endpointAttributes, method);
						if (readOperation != null) {
							return readOperation;
						}
						else {
							return createWriteOperationIfPossible(beanName,
									endpointAttributes, method);
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

	private T createReadOperationIfPossible(String beanName,
			AnnotationAttributes endpointAttributes, Method method) {
		return createOperationIfPossible(beanName, endpointAttributes, method,
				ReadOperation.class, EndpointOperationType.READ);
	}

	private T createWriteOperationIfPossible(String beanName,
			AnnotationAttributes endpointAttributes, Method method) {
		return createOperationIfPossible(beanName, endpointAttributes, method,
				WriteOperation.class, EndpointOperationType.WRITE);
	}

	private T createOperationIfPossible(String beanName,
			AnnotationAttributes endpointAttributes, Method method,
			Class<? extends Annotation> operationAnnotation,
			EndpointOperationType operationType) {
		AnnotationAttributes operationAttributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(method, operationAnnotation);
		if (operationAttributes == null) {
			return null;
		}
		return this.operationInfoFactory.createOperationInfo(endpointAttributes,
				operationAttributes, beanName, method, operationType);
	}

	/**
	 * An {@code OperationInfoFactory} creates an {@link EndpointOperationInfo} that
	 * describes an operation on an endpoint.
	 *
	 * @param <T> the {@link EndpointOperationInfo} type
	 */
	@FunctionalInterface
	protected interface OperationInfoFactory<T extends EndpointOperationInfo> {

		/**
		 * Creates an operation info to describe an operation on an endpoint.
		 * @param endpointAttributes the annotation attributes for the endpoint
		 * @param operationAttributes the annotation attributes for the operation
		 * @param beanName the name of the endpoint bean
		 * @param operationMethod the method on the bean that implements the operation
		 * @param operationType the type of the operation
		 * @return the operation info that describes the operation
		 */
		T createOperationInfo(AnnotationAttributes endpointAttributes,
				AnnotationAttributes operationAttributes, String beanName,
				Method operationMethod, EndpointOperationType operationType);

	}

}
