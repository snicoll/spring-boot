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

package org.springframework.boot.configurationmetadata.diff;

import java.util.function.Predicate;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.Deprecation.Level;

/**
 * Useful {@link Predicate predicates} that can be used against
 * {@link ConfigurationMetadataDiffEntry metadata entries}.
 *
 * @author Stephane Nicoll
 * @since 2.6.0
 */
public abstract class DiffPredicates {

	/**
	 * A {@link Predicate} that matches new entries, i.e. not present on the left side and
	 * present on the right side.
	 */
	public static final Predicate<ConfigurationMetadataDiffEntry<?>> ADDED = new NewEntryPredicate();

	/**
	 * A {@link Predicate} that matches removed entries, i.e. present on the left side and
	 * removed on the right side, including with {@link Level#ERROR ERROR level} metadata.
	 */
	public static final Predicate<ConfigurationMetadataDiffEntry<?>> REMOVED = new RemovedEntryPredicate();

	/**
	 * A {@link Predicate} that matches deprecated entries, i.e. present on the left side
	 * in a non deprecated fashion and deprecated with {@link Level#WARNING WARNING level}
	 * on the right side.
	 */
	public static final Predicate<ConfigurationMetadataDiffEntry<?>> DEPRECATED = new DeprecatedEntryPredicate();

	private static class NewEntryPredicate implements Predicate<ConfigurationMetadataDiffEntry<?>> {

		@Override
		public boolean test(ConfigurationMetadataDiffEntry<?> entry) {
			return entry.getLeft() == null && entry.getRight() != null;
		}

	}

	private static class RemovedEntryPredicate implements Predicate<ConfigurationMetadataDiffEntry<?>> {

		@Override
		public boolean test(ConfigurationMetadataDiffEntry<?> entry) {
			if (entry.getLeft() == null) {
				return false;
			}
			if (entry.getRight() == null) {
				return true;
			}
			if (entry.getLeft() instanceof ConfigurationMetadataProperty
					&& entry.getRight() instanceof ConfigurationMetadataProperty) {
				return isRemovedProperty((ConfigurationMetadataProperty) entry.getLeft(),
						(ConfigurationMetadataProperty) entry.getRight());
			}
			return false;
		}

		private boolean isRemovedProperty(ConfigurationMetadataProperty leftProperty,
				ConfigurationMetadataProperty rightProperty) {
			if (leftProperty.isDeprecated() && leftProperty.getDeprecation().getLevel() == Level.ERROR) {
				return false;
			}
			return (rightProperty.getDeprecation() != null && rightProperty.getDeprecation().getLevel() == Level.ERROR);
		}

	}

	private static class DeprecatedEntryPredicate implements Predicate<ConfigurationMetadataDiffEntry<?>> {

		@Override
		public boolean test(ConfigurationMetadataDiffEntry<?> entry) {
			if (entry.getLeft() instanceof ConfigurationMetadataProperty
					&& entry.getRight() instanceof ConfigurationMetadataProperty) {
				ConfigurationMetadataProperty leftProperty = (ConfigurationMetadataProperty) entry.getLeft();
				ConfigurationMetadataProperty rightProperty = (ConfigurationMetadataProperty) entry.getRight();
				return !leftProperty.isDeprecated() && rightProperty.isDeprecated()
						&& rightProperty.getDeprecation().getLevel() == Level.WARNING;
			}
			return false;
		}

	}

}
