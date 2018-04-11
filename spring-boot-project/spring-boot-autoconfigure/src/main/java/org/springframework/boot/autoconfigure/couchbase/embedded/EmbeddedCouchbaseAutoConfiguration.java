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

package org.springframework.boot.autoconfigure.couchbase.embedded;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import com.couchbase.client.java.CouchbaseBucket;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.mock.Bucket;
import com.couchbase.mock.BucketConfiguration;
import com.couchbase.mock.CouchbaseMock;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseAutoConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseConfiguration;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Embedded Couchbase.
 *
 * @author Alex Derkach
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@Configuration
@AutoConfigureBefore(CouchbaseAutoConfiguration.class)
@ConditionalOnClass({ CouchbaseBucket.class, CouchbaseMock.class })
@EnableConfigurationProperties({ CouchbaseProperties.class })
public class EmbeddedCouchbaseAutoConfiguration {

	@Bean(destroyMethod = "stop")
	@ConditionalOnMissingBean
	public CouchbaseMock couchbaseMock(CouchbaseProperties properties)
			throws IOException, InterruptedException {
		CouchbaseProperties.Bucket bucketProperties = properties.getBucket();
		BucketConfiguration bucketConfiguration = new BucketConfiguration();
		bucketConfiguration.name = bucketProperties.getName();
		bucketConfiguration.password = bucketProperties.getPassword();
		bucketConfiguration.type = Bucket.BucketType.COUCHBASE;
		bucketConfiguration.numReplicas = 1;
		bucketConfiguration.numNodes = 1;

		CouchbaseMock couchbaseMock = new CouchbaseMock(0,
				Collections.singletonList(bucketConfiguration));
		couchbaseMock.start();
		couchbaseMock.waitForStartup();
		return couchbaseMock;
	}

	@Configuration
	static class EmbeddedCouchbaseConfiguration extends CouchbaseConfiguration {

		private final CouchbaseMock couchbaseMock;

		EmbeddedCouchbaseConfiguration(CouchbaseProperties properties,
				CouchbaseMock couchbaseMock) {
			super(properties);
			this.couchbaseMock = couchbaseMock;
		}

		@Override
		protected List<String> determineBootstrapHosts() {
			return Collections.singletonList(this.couchbaseMock.getHttpHost());
		}

		@Override
		protected DefaultCouchbaseEnvironment.Builder initializeEnvironmentBuilder(
				CouchbaseProperties properties) {
			DefaultCouchbaseEnvironment.Builder builder = super.initializeEnvironmentBuilder(properties);

			int httpPort = this.couchbaseMock.getHttpPort();
			int bootstrapCarrierDirectPort = this.couchbaseMock.getBuckets().get(
					properties.getBucket().getName()).getServers()[0].getPort();
			return builder.bootstrapHttpDirectPort(httpPort)
					.bootstrapCarrierDirectPort(bootstrapCarrierDirectPort);
		}

	}

}
