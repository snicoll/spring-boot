/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.couchbase;

import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CouchbaseAutoConfiguration}
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 */
public class CouchbaseAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void bootstrapHostsIsRequired() {
		load(null);
		assertThat(this.context.getBeansOfType(CouchbaseEnvironment.class)).isEmpty();
		assertThat(this.context.getBeansOfType(ClusterInfo.class)).isEmpty();
		assertThat(this.context.getBeansOfType(Cluster.class)).isEmpty();
		assertThat(this.context.getBeansOfType(Bucket.class)).isEmpty();
	}

	@Test
	public void defaultConfiguration() {
		load(CouchbaseTestConfigurer.class, "spring.couchbase.bootstrapHosts:localhost");
		assertThat(this.context.getBeansOfType(CouchbaseEnvironment.class)).isEmpty();
		assertThat(this.context.getBeansOfType(ClusterInfo.class)).isEmpty();
		assertThat(this.context.getBeansOfType(Cluster.class)).isEmpty();
		assertThat(this.context.getBeansOfType(Bucket.class)).isEmpty();
	}

	private void load(Class<?> config, String... environment) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(context, environment);
		if (config != null) {
			context.register(config);
		}
		context.register(PropertyPlaceholderAutoConfiguration.class,
				CouchbaseAutoConfiguration.class);
		context.refresh();
		this.context = context;
	}

}
