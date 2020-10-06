/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.batch;

import java.util.List;
import java.util.StringJoiner;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceInitializationMode;
import org.springframework.util.Assert;

/**
 * Configuration properties for Spring Batch.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Vedran Pavic
 * @since 1.2.0
 */
@ConfigurationProperties(prefix = "spring.batch")
public class BatchProperties {

	private static final String DEFAULT_SCHEMA_LOCATION = "classpath:org/springframework/"
			+ "batch/core/schema-@@platform@@.sql";

	/**
	 * Path to the SQL file to use to initialize the database schema.
	 */
	private String schema = DEFAULT_SCHEMA_LOCATION;

	/**
	 * Table prefix for all the batch meta-data tables.
	 */
	private String tablePrefix;

	/**
	 * Database schema initialization mode.
	 */
	private DataSourceInitializationMode initializeSchema = DataSourceInitializationMode.EMBEDDED;

	private final Job job = new Job();

	public String getSchema() {
		return this.schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getTablePrefix() {
		return this.tablePrefix;
	}

	public void setTablePrefix(String tablePrefix) {
		this.tablePrefix = tablePrefix;
	}

	public DataSourceInitializationMode getInitializeSchema() {
		return this.initializeSchema;
	}

	public void setInitializeSchema(DataSourceInitializationMode initializeSchema) {
		this.initializeSchema = initializeSchema;
	}

	public Job getJob() {
		return this.job;
	}

	public static class Job {

		/**
		 * Comma-separated list of job names to execute on startup (for instance,
		 * `job1,job2`). By default, all Jobs found in the context are executed.
		 */
		private String names = "";

		/**
		 * Job parameters to use for jobs that are executed on startup. Each parameter is
		 * of the form 'name=value'.
		 */
		private List<JobParameterDescriptor> parameters;

		public String getNames() {
			return this.names;
		}

		public void setNames(String names) {
			this.names = names;
		}

		public List<JobParameterDescriptor> getParameters() {
			return this.parameters;
		}

		public void setParameters(List<JobParameterDescriptor> parameters) {
			this.parameters = parameters;
		}

	}

	/**
	 * A descriptor of a {@code JobParameter}.
	 */
	public static class JobParameterDescriptor {

		private final String key;

		private final String value;

		public JobParameterDescriptor(String parameter) {
			Assert.notNull(parameter, () -> "Job parameter must not be null");
			int i = parameter.indexOf("=");
			if (i == -1) {
				throw new IllegalArgumentException("Invalid Job parameter " + parameter);
			}
			this.key = parameter.substring(0, i).trim();
			this.value = parameter.substring(i + 1).trim();
		}

		public String getKey() {
			return this.key;
		}

		public String getValue() {
			return this.value;
		}

		@Override
		public String toString() {
			return new StringJoiner(", ", "[", "]").add("key='" + this.key + "'").add("value='" + this.value + "'")
					.toString();
		}

	}

}
