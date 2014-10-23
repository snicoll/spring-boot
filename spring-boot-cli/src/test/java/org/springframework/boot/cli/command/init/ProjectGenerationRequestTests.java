/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli.command.init;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * Tests for {@link ProjectGenerationRequest}
 *
 * @author Stephane Nicoll
 */
public class ProjectGenerationRequestTests {

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final ProjectGenerationRequest request = new ProjectGenerationRequest();

	@Test
	public void defaultSettings() {
		assertEquals(createDefaultUrl(), request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void customServer() throws URISyntaxException {
		String customServerUrl = "http://foo:8080/initializr";
		request.setServiceUrl(customServerUrl);
		request.getDependencies().add("security");
		assertEquals(new URI(customServerUrl + "/generate.zip?style=security"),
				request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void customBootVersion() {
		request.setBootVersion("1.2.0.RELEASE");
		assertEquals(createDefaultUrl("?bootVersion=1.2.0.RELEASE"),
				request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void singleDependency() {
		request.getDependencies().add("web");
		assertEquals(createDefaultUrl("?style=web"),
				request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void multipleDependencies() {
		request.getDependencies().add("web");
		request.getDependencies().add("data-jpa");
		assertEquals(createDefaultUrl("?style=web&style=data-jpa"),
				request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void customJavaVersion() {
		request.setJavaVersion("1.8");
		assertEquals(createDefaultUrl("?javaVersion=1.8"),
				request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void customPackaging() {
		request.setPackaging("war");
		assertEquals(createDefaultUrl("?packaging=war"),
				request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void customType() throws URISyntaxException {
		ProjectType projectType = new ProjectType("custom", "Custom Type", "/foo", true);
		InitializrServiceMetadata metadata = new InitializrServiceMetadata(projectType);

		request.setType("custom");
		request.getDependencies().add("data-rest");
		assertEquals(new URI(ProjectGenerationRequest.DEFAULT_SERVICE_URL + "/foo?style=data-rest&type=custom"),
				request.generateUrl(metadata));
	}

	@Test
	public void invalidType() throws URISyntaxException {
		request.setType("does-not-exist");

		thrown.expect(IllegalStateException.class);
		request.generateUrl(createDefaultMetadata());
	}


	private static URI createDefaultUrl(String param) {
		try {
			return new URI(ProjectGenerationRequest.DEFAULT_SERVICE_URL + "/generate.zip" + param);
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException(e);
		}
	}

	private static URI createDefaultUrl() {
		return createDefaultUrl("");
	}

	private static InitializrServiceMetadata createDefaultMetadata() {
		ProjectType projectType = new ProjectType("type", "The test type", "/generate.zip", true);
		return new InitializrServiceMetadata(projectType);
	}

}
