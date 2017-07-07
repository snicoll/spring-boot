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

import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.modelmbean.ModelMBeanAttributeInfo;
import javax.management.modelmbean.ModelMBeanConstructorInfo;
import javax.management.modelmbean.ModelMBeanInfoSupport;
import javax.management.modelmbean.ModelMBeanNotificationInfo;
import javax.management.modelmbean.ModelMBeanOperationInfo;

import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.EndpointOperationInfo;
import org.springframework.boot.endpoint.EndpointOperationType;

/**
 * Gather the management operations of a particular {@link EndpointInfo endpoint}.
 *
 * @author Stephane Nicoll
 */
class EndpointMBeanInfoAssembler {

	/**
	 * Creates the {@link EndpointMBeanInfo} for the specified {@link EndpointInfo
	 * endpoint}.
	 * @param endpointInfo the endpoint to handle
	 * @return the mbean info for the endpoint
	 */
	EndpointMBeanInfo createEndpointMBeanInfo(EndpointInfo<?> endpointInfo) {
		Map<String, OperationInfos> operationsMapping = getOperationInfo(endpointInfo);
		ModelMBeanOperationInfo[] operationsMBeanInfo = operationsMapping.values()
				.stream().map(t -> t.mBeanOperationInfo).collect(Collectors.toList())
				.toArray(new ModelMBeanOperationInfo[] {});
		Map<String, EndpointOperationInfo> operationsInfo = new LinkedHashMap<>();
		operationsMapping.forEach((name, t) -> operationsInfo.put(name, t.operationInfo));

		MBeanInfo info = new ModelMBeanInfoSupport(getClassName(endpointInfo),
				getDescription(endpointInfo), new ModelMBeanAttributeInfo[0],
				new ModelMBeanConstructorInfo[0], operationsMBeanInfo,
				new ModelMBeanNotificationInfo[0]);
		return new EndpointMBeanInfo(info, operationsInfo);
	}

	private String getClassName(EndpointInfo<?> endpointInfo) {
		return endpointInfo.getId();
	}

	private String getDescription(EndpointInfo<?> endpointInfo) {
		return "MBean operations for endpoint " + endpointInfo.getId();
	}

	private Map<String, OperationInfos> getOperationInfo(EndpointInfo<?> endpointInfo) {
		Map<String, OperationInfos> operationInfos = new HashMap<>();
		endpointInfo.getOperations().forEach((operationInfo) -> {
			// TODO two methods with the same name may exist
			String name = operationInfo.getOperationMethod().getName();
			ModelMBeanOperationInfo mBeanOperationInfo = new ModelMBeanOperationInfo(name,
					"Invoke " + name + " for endpoint " + endpointInfo.getId(),
					getMBeanParameterInfos(operationInfo),
					mapParameterType(operationInfo.getOperationMethod().getReturnType()),
					mapOperationType(operationInfo.getType()));
			operationInfos.put(name,
					new OperationInfos(mBeanOperationInfo, operationInfo));
		});
		return operationInfos;
	}

	private MBeanParameterInfo[] getMBeanParameterInfos(EndpointOperationInfo info) {
		List<MBeanParameterInfo> parameters = new ArrayList<>();
		for (Parameter parameter : info.getOperationMethod().getParameters()) {
			parameters.add(new MBeanParameterInfo(parameter.getName(),
					mapParameterType(parameter.getType()), null));
		}
		return parameters.toArray(new MBeanParameterInfo[0]);
	}

	private String mapParameterType(Class<?> parameter) {
		if (parameter.isEnum()) {
			return String.class.getName();
		}
		if (parameter.equals(Void.TYPE)) {
			return parameter.getName();
		}
		if (!parameter.getName().startsWith("java.")) {
			return Object.class.getName();
		}
		return parameter.getName();
	}

	private int mapOperationType(EndpointOperationType type) {
		if (type == EndpointOperationType.READ) {
			return MBeanOperationInfo.INFO;
		}
		if (type == EndpointOperationType.WRITE) {
			return MBeanOperationInfo.ACTION;
		}
		return MBeanOperationInfo.UNKNOWN;
	}

	private static class OperationInfos {
		private final ModelMBeanOperationInfo mBeanOperationInfo;

		private final EndpointOperationInfo operationInfo;

		OperationInfos(ModelMBeanOperationInfo mBeanOperationInfo,
				EndpointOperationInfo operationInfo) {
			this.mBeanOperationInfo = mBeanOperationInfo;
			this.operationInfo = operationInfo;
		}
	}

}
