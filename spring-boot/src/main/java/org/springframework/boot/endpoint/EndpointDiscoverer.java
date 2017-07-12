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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;

/**
 * An {@link EndpointDiscoverer} is used to discover endpoint beans in an application
 * context.
 *
 * @param <T> the type of the operation
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public abstract class EndpointDiscoverer<T extends EndpointOperation> {

	private final ApplicationContext applicationContext;
	private final EndpointOperationFactory<T> operationFactory;

	protected EndpointDiscoverer(ApplicationContext applicationContext,
			EndpointOperationFactory<T> operationFactory) {
		this.applicationContext = applicationContext;
		this.operationFactory = operationFactory;
	}

	/**
	 * Perform endpoint discovery.
	 * @return the discovered endpoints
	 */
	public abstract Collection<EndpointInfo<T>> discoverEndpoints();

	/**
	 * Perform endpoint discovery, including discovery and merging of extensions.
	 * @param extensionType the annotation type of the extension
	 * @param extensionFactory the factory to use the extension
	 * @return the list of {@link EndpointInfo EndpointInfos} that describes the
	 * discovered endpoints
	 * @see #discoverGenericEndpoints()
	 */
	public Collection<EndpointInfo<T>> doDiscoverEndpoints(
			Class<? extends Annotation> extensionType,
			EndpointExtensionFactory<T> extensionFactory) {
		Map<Class<?>, EndpointInfo<T>> endpoints = discoverGenericEndpoints();
		Map<Class<?>, EndpointExtensionInfo<T>> extensions = discoverExtensions(
				endpoints, extensionType, extensionFactory);
		Collection<EndpointInfo<T>> result = new ArrayList<>();
		endpoints.forEach((endpointType, endpointInfo) -> {
			EndpointExtensionInfo<T> extension = extensions.remove(endpointType);
			if (extension != null) {
				result.add(extension.merge(endpointInfo));
			}
			else {
				result.add(endpointInfo);
			}

		});
		return result;
	}

	/**
	 * Discover endpoints with no handling of an extension.
	 * @return the list of generic {@link EndpointInfo EndpointInfos} that describes the
	 * discovered endpoints
	 * @see #doDiscoverEndpoints(Class, EndpointExtensionFactory)
	 */
	protected Map<Class<?>, EndpointInfo<T>> discoverGenericEndpoints() {
		String[] endpointBeanNames = this.applicationContext
				.getBeanNamesForAnnotation(Endpoint.class);
		Map<String, EndpointInfo<T>> endpointsById = new HashMap<>();
		Map<Class<?>, EndpointInfo<T>> endpointsByClass = new HashMap<>();
		for (String beanName : endpointBeanNames) {
			Class<?> beanType = this.applicationContext.getType(beanName);
			AnnotationAttributes endpointAttributes = AnnotatedElementUtils
					.getMergedAnnotationAttributes(beanType, Endpoint.class);
			String endpointId = endpointAttributes.getString("id");
			Map<Method, T> operationMethods = discoverOperations(endpointId, beanName,
					beanType);

			EndpointInfo<T> endpointInfo = new EndpointInfo<>(
					endpointId,
					endpointAttributes.getBoolean("enabledByDefault"),
					operationMethods.values());

			EndpointInfo<T> previous = endpointsById.putIfAbsent(endpointInfo.getId(),
					endpointInfo);
			if (previous != null) {
				throw new IllegalStateException(
						"Found two endpoints with the id '" + endpointInfo.getId() + "': "
								+ endpointInfo + " and " + previous);
			}
			endpointsByClass.put(beanType, endpointInfo);
		}
		return endpointsByClass;
	}

	protected Map<Class<?>, EndpointExtensionInfo<T>> discoverExtensions(
			Map<Class<?>, EndpointInfo<T>> endpoints,
			Class<? extends Annotation> extensionType,
			EndpointExtensionFactory<T> extensionFactory) {
		String[] extensionBeanNames = this.applicationContext
				.getBeanNamesForAnnotation(extensionType);
		Map<Class<?>, EndpointExtensionInfo<T>> extensionsByEndpoint = new HashMap<>();
		for (String beanName : extensionBeanNames) {
			Class<?> beanType = this.applicationContext.getType(beanName);
			AnnotationAttributes endpointAttributes = AnnotatedElementUtils
					.getMergedAnnotationAttributes(beanType, extensionType);
			Class<?> endpointType = (Class<?>) endpointAttributes.get("endpoint");
			EndpointInfo<T> endpoint = endpoints.get(endpointType);
			if (endpoint == null) {
				throw new IllegalStateException(String.format(
						"Invalid extension '%s': no endpoint found with type '%s'",
						beanType.getName(), endpointType.getName()));
			}
			Map<Method, T> operationMethods = discoverOperations(endpoint.getId(),
					beanName, beanType);
			EndpointExtensionInfo<T> extension = extensionFactory.createExtension(
					endpointType, beanType, operationMethods.values());

			EndpointExtensionInfo<T> previous = extensionsByEndpoint.putIfAbsent(endpointType,
					extension);
			if (previous != null) {
				throw new IllegalStateException(
						"Found two extensions for the same endpoint '" + endpointType.getName() + "': "
								+ extension.getEndpointExtensionType().getName() + " and " + previous.getEndpointExtensionType().getName());
			}
		}
		return extensionsByEndpoint;
	}

	private Map<Method, T> discoverOperations(String endpointId, String beanName,
			Class<?> beanType) {
		return MethodIntrospector.selectMethods(beanType,
				(MethodIntrospector.MetadataLookup<T>) (method) -> {
					T readOperation = createReadOperationIfPossible(endpointId,
							beanName, method);
					if (readOperation != null) {
						return readOperation;
					}
					else {
						return createWriteOperationIfPossible(endpointId, beanName,
								method);
					}
				});
	}

	private T createReadOperationIfPossible(String endpointId, String beanName,
			Method method) {
		return createOperationIfPossible(endpointId, beanName, method,
				ReadOperation.class, EndpointOperationType.READ);
	}

	private T createWriteOperationIfPossible(String endpointId, String beanName,
			Method method) {
		return createOperationIfPossible(endpointId, beanName, method,
				WriteOperation.class, EndpointOperationType.WRITE);
	}

	private T createOperationIfPossible(String endpointId, String beanName,
			Method method, Class<? extends Annotation> operationAnnotation,
			EndpointOperationType operationType) {
		AnnotationAttributes operationAttributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(method, operationAnnotation);
		if (operationAttributes == null) {
			return null;
		}
		return this.operationFactory.createOperation(endpointId, operationAttributes,
				this.applicationContext.getBean(beanName), method, operationType);
	}


	/**
	 * An {@code EndpointOperationFactory} creates an {@link EndpointOperation} for an
	 * operation on an endpoint.
	 *
	 * @param <T> the {@link EndpointOperation} type
	 */
	@FunctionalInterface
	protected interface EndpointOperationFactory<T extends EndpointOperation> {

		/**
		 * Creates an {@code EndpointOperation} for an operation on an endpoint.
		 * @param endpointId the id of the endpoint
		 * @param operationAttributes the annotation attributes for the operation
		 * @param target the target that implements the operation
		 * @param operationMethod the method on the bean that implements the operation
		 * @param operationType the type of the operation
		 * @return the operation info that describes the operation
		 */
		T createOperation(String endpointId, AnnotationAttributes operationAttributes,
				Object target, Method operationMethod,
				EndpointOperationType operationType);

	}

	/**
	 * Describes a tech specific extension of an endpoint.
	 * @param <T> the type of the operation
	 */
	protected static abstract class EndpointExtensionInfo<T extends EndpointOperation> {

		private final Class<?> endpointType;

		private final Class<?> endpointExtensionType;

		private final Collection<T> operations;

		protected EndpointExtensionInfo(Class<?> endpointType, Class<?> endpointExtensionType,
				Collection<T> operations) {
			this.endpointType = endpointType;
			this.endpointExtensionType = endpointExtensionType;
			this.operations = operations;
		}

		public abstract EndpointInfo<T> merge(EndpointInfo<T> existing);

		protected Class<?> getEndpointType() {
			return this.endpointType;
		}

		protected Class<?> getEndpointExtensionType() {
			return this.endpointExtensionType;
		}

		protected Collection<T> getOperations() {
			return this.operations;
		}

	}

	/**
	 * Creates an {@link EndpointExtensionInfo} for a given extension.
	 * @param <T> the type of the operation
	 */
	protected interface EndpointExtensionFactory<T extends EndpointOperation> {

		/**
		 * Create an {@link EndpointExtensionInfo} for an extension of an endpoint.
		 * @param endpointType the type of the endpoint
		 * @param endpointExtensionType the type of the extension
		 * @param operations the discovered operations on the extension
		 * @return an {@link EndpointExtensionInfo} describing the extension
		 */
		EndpointExtensionInfo<T> createExtension(Class<?> endpointType,
				Class<?> endpointExtensionType, Collection<T> operations);

	}



}
