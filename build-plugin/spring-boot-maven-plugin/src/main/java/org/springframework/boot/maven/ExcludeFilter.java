/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.maven;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;

/**
 * A {DependencyFilter} implementation that filters out any artifact matching an
 * {@link Exclude}.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 * @since 1.1.0
 */
public class ExcludeFilter extends DependencyFilter {

	private List<Exclude> excludes = new ArrayList<>();

	public static ExcludeFilter of(Exclude... excludes) {
		ExcludeFilter filter = new ExcludeFilter();
		filter.setExcludes(List.of(excludes));
		return filter;
	}

	public List<Exclude> getExcludes() {
		return this.excludes;
	}

	public void setExcludes(List<Exclude> excludes) {
		this.excludes = excludes;
	}

	@Override
	protected boolean filter(Artifact artifact) {
		for (FilterableDependency dependency : this.excludes) {
			if (equals(artifact, dependency)) {
				return true;
			}
		}
		return false;
	}

}
