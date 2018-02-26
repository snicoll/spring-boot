/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.kafka;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.ConfigResource.Type;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.util.Assert;

/**
 * {@link HealthIndicator} for Kafka cluster.
 *
 * @author Juan Rada
 * @author Gary Russell
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class KafkaHealthIndicator extends AbstractHealthIndicator
		implements DisposableBean {

	static final String REPLICATION_PROPERTY = "transaction.state.log.replication.factor";

	private final AdminClient adminClient;

	private final DescribeClusterOptions describeOptions;

	private final Function<String, Boolean> considerReplicationFactor;

	/**
	 * Create a new {@link KafkaHealthIndicator} instance.
	 *
	 * @param kafkaAdmin the kafka admin
	 * @param requestTimeout the request timeout in milliseconds
	 * @param considerReplicationFactor function to determine if the replication factor
	 * for a given broker should be considered
	 */
	public KafkaHealthIndicator(KafkaAdmin kafkaAdmin, long requestTimeout,
			Function<String, Boolean> considerReplicationFactor) {
		Assert.notNull(kafkaAdmin, "KafkaAdmin must not be null");
		this.adminClient = AdminClient.create(kafkaAdmin.getConfig());
		this.describeOptions = new DescribeClusterOptions()
				.timeoutMs((int) requestTimeout);
		this.considerReplicationFactor = considerReplicationFactor;
	}

	/**
	 * Create a new {@link KafkaHealthIndicator} instance.
	 *
	 * @param kafkaAdmin the kafka admin
	 * @param requestTimeout the request timeout in milliseconds
	 */
	public KafkaHealthIndicator(KafkaAdmin kafkaAdmin, long requestTimeout) {
		this(kafkaAdmin, requestTimeout, (brokerId) -> true);
	}

	@Override
	protected void doHealthCheck(Builder builder) throws Exception {
		DescribeClusterResult result = this.adminClient.describeCluster(
				this.describeOptions);
		String brokerId = result.controller().get().idString();
		int nodes = result.nodes().get().size();
		if (this.considerReplicationFactor.apply(brokerId)) {
			int replicationFactor = getReplicationFactor(brokerId);
			Status status = nodes >= replicationFactor ? Status.UP : Status.DOWN;
			builder.status(status).withDetail("requiredNodes", replicationFactor);
		}
		else {
			builder.up();
		}
		builder.withDetail("clusterId", result.clusterId().get())
				.withDetail("brokerId", brokerId).withDetail("nodes", nodes);
	}

	private int getReplicationFactor(String brokerId) throws Exception {
		ConfigResource configResource = new ConfigResource(Type.BROKER, brokerId);
		Map<ConfigResource, Config> kafkaConfig = this.adminClient
				.describeConfigs(Collections.singletonList(configResource)).all().get();
		Config brokerConfig = kafkaConfig.get(configResource);
		return Integer.parseInt(brokerConfig.get(REPLICATION_PROPERTY).value());
	}

	@Override
	public void destroy() {
		this.adminClient.close(30, TimeUnit.SECONDS);
	}

}
