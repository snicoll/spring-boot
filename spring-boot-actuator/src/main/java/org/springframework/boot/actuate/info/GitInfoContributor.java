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

package org.springframework.boot.actuate.info;

import java.io.IOException;
import java.util.Properties;

import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.validation.BindException;

/**
 * A {@link InfoContributor} that provides git information extracted from a
 * {@link Resource} compliant with the format of the {@code git-commit-id-plugin}.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class GitInfoContributor implements InfoContributor {

	private final GitInfo gitInfo;

	public GitInfoContributor(Resource resource) throws IOException {
		this.gitInfo = extractGitInfo(resource);
	}

	@Override
	public void contribute(Info.Builder builder) {
		if (this.gitInfo != null) {
			builder.withDetail("git", this.gitInfo);
		}
	}

	protected GitInfo extractGitInfo(Resource resource) throws IOException {
		if (!resource.exists()) {
			return null;
		}
		GitInfo gitInfo = new GitInfo();
		PropertiesConfigurationFactory<GitInfo> factory = new PropertiesConfigurationFactory<GitInfo>(
				gitInfo);
		factory.setTargetName("git");
		Properties properties = PropertiesLoaderUtils.loadProperties(resource);
		factory.setProperties(properties);
		try {
			factory.bindPropertiesToTarget();
		}
		catch (BindException ex) {
			throw new IllegalStateException("Cannot bind to GitInfo", ex);
		}
		return gitInfo;
	}


	/**
	 * Git info.
	 */
	public static class GitInfo {

		private String branch;

		private final Commit commit = new Commit();

		public String getBranch() {
			return this.branch;
		}

		public void setBranch(String branch) {
			this.branch = branch;
		}

		public Commit getCommit() {
			return this.commit;
		}

		/**
		 * Commit information.
		 */
		public static class Commit {

			private String id;

			private String time;

			public String getId() {
				return this.id == null ? ""
						: (this.id.length() > 7 ? this.id.substring(0, 7) : this.id);
			}

			public void setId(String id) {
				this.id = id;
			}

			public String getTime() {
				return this.time;
			}

			public void setTime(String time) {
				this.time = time;
			}

		}

	}

}
