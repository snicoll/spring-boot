/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.domain;

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.util.ClassUtils;

/**
 * A simple {@link EntityProvider} implementation that provides the entities to use based
 * on a pre-computed list of fully qualified names.
 *
 * @author Stephane Nicoll
 * @since 3.0
 */
public class SimpleEntityProvider implements EntityProvider {

	private final ClassLoader classLoader;

	private final String[] entityClassNames;

	private Set<Class<?>> entities;

	public SimpleEntityProvider(ClassLoader classLoader, String... entityClassNames) {
		this.classLoader = classLoader;
		this.entityClassNames = entityClassNames;
	}

	@Override
	public Set<Class<?>> provideEntities() throws ClassNotFoundException {
		if (this.entities == null) {
			this.entities = loadEntities();
		}
		return this.entities;
	}

	private Set<Class<?>> loadEntities() throws ClassNotFoundException {
		Set<Class<?>> entities = new LinkedHashSet<>();
		for (String entityClassName : this.entityClassNames) {
			entities.add(ClassUtils.forName(entityClassName, this.classLoader));
		}
		return entities;
	}

}
