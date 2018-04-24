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

package org.springframework.boot.maven;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractDependencyFilterMojo}.
 *
 * @author Stephane Nicoll
 * @author Dmytro Nosan
 */
public class DependencyFilterMojoTests {

	private final TestableDependencyFilter filter = new TestableDependencyFilter();

	@Test
	public void excludeDependencies() {
		this.filter.excludeGroupIds = "com.foo";

		Artifact artifact = createArtifact("com.bar", "one");
		Set<Artifact> artifacts = this.filter.filterDependencies(
				createArtifact("com.foo", "one"), createArtifact("com.foo", "two"),
				artifact);
		assertThat(artifacts).hasSize(1);
		assertThat(artifacts.iterator().next()).isSameAs(artifact);
	}

	@Test
	public void includeDependencies() {
		this.filter.includeGroupIds = "com.foo";

		Artifact artifact = createArtifact("com.bar", "one");
		Set<Artifact> artifacts = this.filter.filterDependencies(
				createArtifact("com.foo", "one"), createArtifact("com.foo", "two"),
				artifact);
		assertThat(artifacts).hasSize(2);
	}

	@Test
	public void excludeGroupIdExactMatch() {
		this.filter.excludeGroupIds = "com.foo";

		Artifact artifact = createArtifact("com.foo.bar", "one");
		Set<Artifact> artifacts = this.filter.filterDependencies(
				createArtifact("com.foo", "one"), createArtifact("com.foo", "two"),
				artifact);
		assertThat(artifacts).hasSize(1);
		assertThat(artifacts.iterator().next()).isSameAs(artifact);
	}

	@Test
	public void includeGroupIdExactMatch() {
		this.filter.includeGroupIds = "com.foo";

		Artifact artifact1 = createArtifact("com.foo", "one");
		Artifact artifact2 = createArtifact("com.foo", "two");
		Set<Artifact> artifacts = this.filter.filterDependencies(
				artifact1, artifact2, createArtifact("com.foo.bar", "one"));
		assertThat(artifacts).hasSize(2);
		assertThat(artifacts).contains(artifact1).contains(artifact2);
	}

	@Test
	public void filterScopeKeepOrder() {
		this.filter.setAdditionalFilters(new ScopeFilter(null, Artifact.SCOPE_SYSTEM));
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.foo", "two", Artifact.SCOPE_SYSTEM);
		Artifact three = createArtifact("com.foo", "three", Artifact.SCOPE_RUNTIME);
		Set<Artifact> artifacts = this.filter.filterDependencies(one, two, three);
		assertThat(artifacts).containsExactly(one, three);
	}

	@Test
	public void filterExcludeGroupIdKeepOrder() {
		this.filter.excludeGroupIds = "com.foo";
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.bar", "two");
		Artifact three = createArtifact("com.bar", "three");
		Artifact four = createArtifact("com.foo", "four");
		Set<Artifact> artifacts = this.filter.filterDependencies(one, two, three, four);
		assertThat(artifacts).containsExactly(two, three);
	}

	@Test
	public void filterIncludeGroupIdKeepOrder() {
		this.filter.includeGroupIds = "com.foo";
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.bar", "two");
		Artifact three = createArtifact("com.bar", "three");
		Artifact four = createArtifact("com.foo", "four");
		Set<Artifact> artifacts = this.filter.filterDependencies(one, two, three, four);
		assertThat(artifacts).containsExactly(one, four);
	}

	@Test
	public void filterExcludeKeepOrder() {
		Exclude exclude = new Exclude();
		exclude.setGroupId("com.bar");
		exclude.setArtifactId("two");
		this.filter.excludes.add(exclude);
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.bar", "two");
		Artifact three = createArtifact("com.bar", "three");
		Artifact four = createArtifact("com.foo", "four");
		Set<Artifact> artifacts = this.filter.filterDependencies(one, two, three, four);
		assertThat(artifacts).containsExactly(one, three, four);
	}

	@Test
	public void filterIncludeKeepOrder() {
		Include include1 = new Include();
		include1.setGroupId("com.bar");
		include1.setArtifactId("two");

		Include include2 = new Include();
		include2.setGroupId("com.bar");
		include2.setArtifactId("three");

		this.filter.includes.addAll(Arrays.asList(include1, include2));
		Artifact one = createArtifact("com.foo", "one");
		Artifact two = createArtifact("com.bar", "two");
		Artifact three = createArtifact("com.bar", "three");
		Artifact four = createArtifact("com.foo", "four");
		Set<Artifact> artifacts = this.filter.filterDependencies(one, two, three, four);
		assertThat(artifacts).containsExactly(two, three);
	}

	private static Artifact createArtifact(String groupId, String artifactId) {
		return createArtifact(groupId, artifactId, null);
	}

	private static Artifact createArtifact(String groupId, String artifactId,
			String scope) {
		Artifact a = mock(Artifact.class);
		given(a.getGroupId()).willReturn(groupId);
		given(a.getArtifactId()).willReturn(artifactId);
		if (scope != null) {
			given(a.getScope()).willReturn(scope);
		}
		return a;
	}

	private static class TestableDependencyFilter {

		private final List<Include> includes = new ArrayList<>();

		private final List<Exclude> excludes = new ArrayList<>();

		private String includeGroupIds;

		private String excludeGroupIds;

		private ArtifactsFilter[] additionalFilters;

		public Set<Artifact> filterDependencies(Artifact... artifacts) {
			TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo();
			mojo.setIncludes(this.includes);
			mojo.setExcludes(this.excludes);
			if (this.includeGroupIds != null) {
				mojo.setIncludeGroupIds(this.includeGroupIds);
			}
			if (this.excludeGroupIds != null) {
				mojo.setExcludeGroupIds(this.excludeGroupIds);
			}
			Set<Artifact> input = new LinkedHashSet<>(Arrays.asList(artifacts));
			try {
				FilterArtifacts filters = mojo.getFilters(this.additionalFilters != null
						? this.additionalFilters : new ArtifactsFilter[0]);
				return mojo.filterDependencies(input, filters);
			}
			catch (Exception ex) {
				throw new IllegalStateException(ex);
			}
		}

		private void setAdditionalFilters(ArtifactsFilter... artifactsFilters) {
			this.additionalFilters = artifactsFilters;
		}

	}

	private static final class TestableDependencyFilterMojo
			extends AbstractDependencyFilterMojo {

		@Override
		public void execute() {

		}

	}

}
