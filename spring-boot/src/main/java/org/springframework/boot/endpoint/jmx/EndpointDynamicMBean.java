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

package org.springframework.boot.endpoint.jmx;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.Function;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;

import reactor.core.publisher.Mono;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.EndpointOperationInfo;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A {@link DynamicMBean} that invokes an {@link EndpointInfo endpoint}. Convert known
 * input parameters such as enums automatically.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 2.0.0
 * @see EndpointMBeanInfoAssembler
 */
public class EndpointDynamicMBean implements DynamicMBean {

	private static boolean REACTOR_PRESENT = ClassUtils.isPresent(
			"reactor.core.publisher.Mono", EndpointDynamicMBean.class.getClassLoader());

	private final String endpointId;

	private final BeanFactory beanFactory;

	private final Function<Object, Object> operationResponseConverter;

	private final EndpointMBeanInfo endpointInfo;

	EndpointDynamicMBean(String endpointId, BeanFactory beanFactory,
			Function<Object, Object> operationResponseConverter,
			EndpointMBeanInfo endpointInfo) {
		this.endpointId = endpointId;
		this.beanFactory = beanFactory;
		this.operationResponseConverter = operationResponseConverter;
		this.endpointInfo = endpointInfo;
	}

	/**
	 * Returns the id of the endpoint that is exposed by this MBean.
	 * @return the endpoint id
	 */
	public String getEndpointId() {
		return this.endpointId;
	}

	@Override
	public MBeanInfo getMBeanInfo() {
		return this.endpointInfo.getMbeanInfo();
	}

	@Override
	public Object invoke(String actionName, Object[] params, String[] signature)
			throws MBeanException, ReflectionException {
		EndpointOperationInfo operationInfo = this.endpointInfo.getOperations()
				.get(actionName);
		if (operationInfo != null) {
			Object bean = this.beanFactory.getBean(operationInfo.getBeanName());
			Method method = operationInfo.getOperationMethod();
			Object result = ReflectionUtils.invokeMethod(method, bean,
					convertParameters(params, method.getParameters()));
			if (REACTOR_PRESENT) {
				result = ReactiveHandler.handle(result);
			}
			return this.operationResponseConverter.apply(result);
		}
		return null; // TODO?
	}

	private Object[] convertParameters(Object[] inbound, Parameter[] parameters) {
		Object[] mapped = new Object[inbound.length];
		for (int i = 0; i < inbound.length; i++) {
			mapped[i] = convertParameter(inbound[i], parameters[i]);
		}
		return mapped;
	}

	// TODO: use conversion service or something
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object convertParameter(Object inbound, Parameter parameter) {
		if (inbound == null) {
			return null;
		}
		if (parameter.getType().isEnum() && inbound instanceof String) {
			if (StringUtils.hasText((String) inbound)) {
				return Enum.valueOf((Class<? extends Enum>) parameter.getType(),
						String.valueOf(inbound).toUpperCase());
			}
			return null;
		}
		return inbound;
	}

	@Override
	public Object getAttribute(String attribute)
			throws AttributeNotFoundException, MBeanException, ReflectionException {
		throw new AttributeNotFoundException();
	}

	@Override
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
			InvalidAttributeValueException, MBeanException, ReflectionException {
		throw new AttributeNotFoundException();
	}

	@Override
	public AttributeList getAttributes(String[] attributes) {
		return new AttributeList();
	}

	@Override
	public AttributeList setAttributes(AttributeList attributes) {
		return new AttributeList();
	}

	private static class ReactiveHandler {

		public static Object handle(Object result) {
			if (result instanceof Mono) {
				return ((Mono<?>) result).block();
			}
			return result;
		}

	}

}
