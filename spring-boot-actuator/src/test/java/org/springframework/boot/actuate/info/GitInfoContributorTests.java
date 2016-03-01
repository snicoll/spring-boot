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

import org.junit.Test;

import org.springframework.boot.actuate.info.GitInfoContributor.GitInfo;
import org.springframework.boot.actuate.info.GitInfoContributor.GitInfo.Commit;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link GitInfoContributor}
 */
public class GitInfoContributorTests {

	@Test
	public void extractGitInfo() {
		String gitProperties = "git.commit.id.abbrev=e02a4f3\r\n"
				+ "git.commit.user.email=dsyer@vmware.com\r\n"
				+ "git.commit.message.full=Update Spring\r\n"
				+ "git.commit.id=e02a4f3b6f452cdbf6dd311f1362679eb4c31ced\r\n"
				+ "git.commit.message.short=Update Spring\r\n"
				+ "git.commit.user.name=Dave Syer\r\n"
				+ "git.build.user.name=Dave Syer\r\n"
				+ "git.build.user.email=dsyer@vmware.com\r\n"
				+ "git.branch=develop\r\n"
				+ "git.commit.time=2013-04-24T08\\:42\\:13+0100\r\n"
				+ "git.build.time=2013-05-23T09\\:26\\:42+0100\r\n";

		Resource resource = new ByteArrayResource(gitProperties.getBytes());

		Info actual = contributeFrom(resource);
		assertThat(actual).isNotNull();
		GitInfo gitInfo = actual.get("git", GitInfo.class);
		assertThat(gitInfo.getBranch()).isEqualTo("develop");
		Commit actualCommit = gitInfo.getCommit();
		assertThat(actualCommit).isNotNull();
		assertThat(actualCommit.getId()).isEqualTo("e02a4f3");
		assertThat(actualCommit.getTime()).isEqualTo("2013-04-24T08:42:13+0100");
	}

	@Test
	public void extractGitInfoMissingData() {
		String gitProperties = "git.commit.id.abbrev=e02a4f3\r\n"
				+ "git.commit.user.email=dsyer@vmware.com\r\n"
				+ "git.commit.message.full=Update Spring\r\n"
				+ "git.commit.id=e02a4f3b6f452cdbf6dd311f1362679eb4c31ced\r\n"
				+ "git.commit.message.short=Update Spring\r\n"
				+ "git.commit.user.name=Dave Syer\r\n"
				+ "git.build.user.name=Dave Syer\r\n"
				+ "git.build.user.email=dsyer@vmware.com\r\n"
				+ "git.branch=develop\r\n"
				+ "git.build.time=2013-05-23T09\\:26\\:42+0100\r\n";

		Resource resource = new ByteArrayResource(gitProperties.getBytes());

		Info actual = contributeFrom(resource);
		assertThat(actual).isNotNull();
		GitInfo gitInfo = actual.get("git", GitInfo.class);
		assertThat(gitInfo.getBranch()).isEqualTo("develop");
		Commit actualCommit = gitInfo.getCommit();
		assertThat(actualCommit).isNotNull();
		assertThat(actualCommit.getId()).isEqualTo("e02a4f3");
		assertThat(actualCommit.getTime()).isNull();
	}

	@Test
	public void extractGitInfoWrongFormat() {
		Resource resource = new ByteArrayResource("GARBAGE".getBytes());

		Info actual = contributeFrom(resource);
		assertThat(actual).isNotNull();
		GitInfo gitInfo = actual.get("git", GitInfo.class);
		assertThat(gitInfo).isNotNull();
		assertThat((String) gitInfo.getBranch()).isNull();
		Commit actualCommit = gitInfo.getCommit();
		assertThat(actualCommit).isNotNull();
		assertThat(actualCommit.getId()).isEqualTo("");
		assertThat(actualCommit.getTime()).isNull();
	}

	@Test
	public void extractGitInfoResourceDoesNotExist() {
		Resource resource = mock(Resource.class);
		given(resource.exists()).willReturn(false);

		Info actual = contributeFrom(resource);
		assertThat(actual).isNotNull();
		assertThat(actual.get("git")).isNull();
	}

	private static Info contributeFrom(Resource resource) {
		try {
			GitInfoContributor contributor = new GitInfoContributor(resource);
			Info.Builder builder = new Info.Builder();
			contributor.contribute(builder);
			return builder.build();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
