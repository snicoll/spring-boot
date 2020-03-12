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

package org.springframework.boot.loader.tools.layer.classes;

import java.util.List;
import java.util.regex.Pattern;

/**
 * An implementation of {@link ResourceFilter} based on the resource name.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public class NameFilter extends AbstractResourceFilter {

	public NameFilter(List<String> includes, List<String> excludes) {
		super(includes, excludes);
	}

	@Override
	protected boolean isMatch(String resourceName, List<String> toMatch) {
		for (String patternString : toMatch) {
			Pattern pattern = buildPatternForString(patternString);
			if (pattern.matcher(resourceName).matches()) {
				return true;
			}
		}
		return false;
	}

	private Pattern buildPatternForString(String pattern) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			if (c == '.') {
				builder.append("\\.");
			}
			else if (c == '*') {
				builder.append(".*");
			}
			else {
				builder.append(c);
			}
		}
		return Pattern.compile(builder.toString());
	}

}
