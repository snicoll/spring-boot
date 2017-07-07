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

import java.util.Map;

import javax.management.MBeanInfo;

import org.springframework.boot.endpoint.EndpointInfo;
import org.springframework.boot.endpoint.EndpointOperationInfo;

/**
 * The {@link MBeanInfo} for a particular {@link EndpointInfo endpoint}. Maps operation
 * names to a {@link EndpointOperationInfo}.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public final class EndpointMBeanInfo {

	private final MBeanInfo mBeanInfo;

	private final Map<String, EndpointOperationInfo> operations;

	public EndpointMBeanInfo(MBeanInfo mBeanInfo,
			Map<String, EndpointOperationInfo> operations) {
		this.mBeanInfo = mBeanInfo;
		this.operations = operations;
	}

	public MBeanInfo getMbeanInfo() {
		return this.mBeanInfo;
	}

	public Map<String, EndpointOperationInfo> getOperations() {
		return this.operations;
	}

}
