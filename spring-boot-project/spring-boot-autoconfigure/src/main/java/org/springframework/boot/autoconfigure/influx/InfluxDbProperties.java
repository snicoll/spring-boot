/*
 * Copyright 2012-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.influx;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDB.LogLevel;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for InfluxDB.
 *
 * @author Sergey Kuptsov
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@ConfigurationProperties(prefix = "spring.influx")
public class InfluxDbProperties {

	/**
	 * URL of the InfluxDB instance to which to connect.
	 */
	private String url;

	/**
	 * Login user.
	 */
	private String user;

	/**
	 * Login password.
	 */
	private String password;

	/**
	 * Database name.
	 */
	private String database;

	/**
	 * Retention policy.
	 */
	private String retentionPolicy = "autogen";

	/**
	 * Consistency level.
	 */
	private InfluxDB.ConsistencyLevel consistencyLevel = ConsistencyLevel.ONE;

	/**
	 * Log level used for REST-related actions.
	 */
	private InfluxDB.LogLevel log;

	/**
	 * Whether to enable Gzip compression.
	 */
	private boolean gzipEnabled;

	private final Batch batch = new Batch();

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPassword() {
		return this.password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDatabase() {
		return this.database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getRetentionPolicy() {
		return this.retentionPolicy;
	}

	public void setRetentionPolicy(String retentionPolicy) {
		this.retentionPolicy = retentionPolicy;
	}

	public ConsistencyLevel getConsistencyLevel() {
		return this.consistencyLevel;
	}

	public void setConsistencyLevel(ConsistencyLevel consistencyLevel) {
		this.consistencyLevel = consistencyLevel;
	}

	public LogLevel getLog() {
		return this.log;
	}

	public void setLog(LogLevel log) {
		this.log = log;
	}

	public boolean isGzipEnabled() {
		return this.gzipEnabled;
	}

	public void setGzipEnabled(boolean gzipEnabled) {
		this.gzipEnabled = gzipEnabled;
	}

	public Batch getBatch() {
		return this.batch;
	}

	public static class Batch {

		/**
		 * Whether to send data points in batches.
		 */
		private boolean enabled;

		/**
		 * Maximum size of the action queue.
		 */
		private int actions = 1000;

		/**
		 * Maximum time to wait before submitting the queued data points.
		 */
		private Duration flushDuration = Duration.ofMillis(1000);

		/**
		 * Jitter the batch flush interval by a random amount. This is primarily to avoid
		 * large write spikes for users running a large number of client instances.
		 */
		private Duration jitterDuration = Duration.ofMillis(0);

		/**
		 * Number of data points stored in the retry buffer.
		 */
		private int bufferLimit = 10000;

		/**
		 * Precision to use for the whole batch.
		 */
		private TimeUnit precision = TimeUnit.NANOSECONDS;

		/**
		 * Whether to drop actions when the queue exhausts.
		 */
		private boolean dropActionsOnQueueExhaustion = false;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public int getActions() {
			return this.actions;
		}

		public void setActions(int actions) {
			this.actions = actions;
		}

		public Duration getFlushDuration() {
			return this.flushDuration;
		}

		public void setFlushDuration(Duration flushDuration) {
			this.flushDuration = flushDuration;
		}

		public Duration getJitterDuration() {
			return this.jitterDuration;
		}

		public void setJitterDuration(Duration jitterDuration) {
			this.jitterDuration = jitterDuration;
		}

		public int getBufferLimit() {
			return this.bufferLimit;
		}

		public void setBufferLimit(int bufferLimit) {
			this.bufferLimit = bufferLimit;
		}

		public TimeUnit getPrecision() {
			return this.precision;
		}

		public void setPrecision(TimeUnit precision) {
			this.precision = precision;
		}

		public boolean isDropActionsOnQueueExhaustion() {
			return this.dropActionsOnQueueExhaustion;
		}

		public void setDropActionsOnQueueExhaustion(boolean dropActionsOnQueueExhaustion) {
			this.dropActionsOnQueueExhaustion = dropActionsOnQueueExhaustion;
		}

	}

}
