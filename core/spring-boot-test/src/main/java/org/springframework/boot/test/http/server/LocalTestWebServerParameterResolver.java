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

package org.springframework.boot.test.http.server;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;

import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringParameterResolver;

/**
 * {@link SpringParameterResolver} implementation that resolves the
 * {@link LocalTestWebServer} associated with the context started in the scope of the
 * current test class.
 *
 * @author Stephane Nicoll
 */
final class LocalTestWebServerParameterResolver implements SpringParameterResolver {

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		return LocalTestWebServer.class.isAssignableFrom(parameterContext.getParameter().getType());
	}

	@Override
	public @Nullable Object resolveParameter(ApplicationContext applicationContext, ParameterContext parameterContext,
			ExtensionContext extensionContext) throws ParameterResolutionException {
		LocalTestWebServer localTestWebServer = LocalTestWebServer.get(applicationContext);
		if (localTestWebServer != null) {
			return localTestWebServer;
		}
		throw new ParameterResolutionException("No local test web server available");
	}

}
