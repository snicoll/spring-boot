/*
 * Copyright 2012-2015 the original author or authors.
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

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Carries information from a specific info provider.
 *
 * @author Meang Akira Tanaka
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@JsonInclude(Include.NON_EMPTY)
public final class Info {

	private final Map<String, Object> details;

	private Info(Builder builder) {
		this.details = new LinkedHashMap<String, Object>();
		this.details.putAll(builder.content);
	}

	/**
	 * Return the content.
	 * @return the details of the info or an empty map.
	 */
	@JsonAnyGetter
	public Map<String, Object> getDetails() {
		return this.details;
	}

	public Object get(String id) {
		return this.details.get(id);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String id, Class<T> type) {
		Object value = get(id);
		if (value != null && type != null && !type.isInstance(value)) {
			throw new IllegalStateException("Info entry is not of required type [" + type.getName() + "]: " + value);
		}
		return (T) value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj != null && obj instanceof Info) {
			Info other = (Info) obj;
			return this.details.equals(other.details);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.details.hashCode();
	}

	@Override
	public String toString() {
		return getDetails().toString();
	}

	/**
	 * Builder for creating immutable {@link Info} instances.
	 */
	public static class Builder {

		private final Map<String, Object> content;

		public Builder() {
			this.content = new LinkedHashMap<String, Object>();
		}

		public Builder withDetail(String key, Object detail) {
			this.content.put(key, detail);
			return this;
		}

		public Builder withDetails(Map<String, Object> details) {
			this.content.putAll(details);
			return this;
		}

		public Info build() {
			return new Info(this);
		}

	}
}
