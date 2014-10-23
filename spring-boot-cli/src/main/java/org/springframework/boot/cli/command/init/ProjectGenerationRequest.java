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
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.utils.URIBuilder;

/**
 * Represent the settings to apply to generating the project.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class ProjectGenerationRequest {

	public static final String DEFAULT_SERVICE_URL = "https://start.spring.io";

	private String serviceUrl = DEFAULT_SERVICE_URL;

	private String output;

	private String bootVersion;

	private List<String> dependencies = new ArrayList<String>();

	private String javaVersion;

	private String packaging;

	private String type;

	/**
	 * The url of the service to use.
	 * @see #DEFAULT_SERVICE_URL
	 */
	public String getServiceUrl() {
		return serviceUrl;
	}

	public void setServiceUrl(String serviceUrl) {
		this.serviceUrl = serviceUrl;
	}

	/**
	 * The location of the  generated project.
	 */
	public String getOutput() {
		return output;
	}

	public void setOutput(String output) {
		this.output = output;
	}

	/**
	 * The Spring Boot version to use or {@code null} if it should not be customized.
	 */
	public String getBootVersion() {
		return bootVersion;
	}

	public void setBootVersion(String bootVersion) {
		this.bootVersion = bootVersion;
	}

	/**
	 * The identifiers of the dependencies to include in the project.
	 */
	public List<String> getDependencies() {
		return dependencies;
	}

	/**
	 * The Java version to use or {@code null} if it should not be customized.
	 */
	public String getJavaVersion() {
		return javaVersion;
	}

	public void setJavaVersion(String javaVersion) {
		this.javaVersion = javaVersion;
	}

	/**
	 * The packaging type or {@code null} if it should not be customized.
	 */
	public String getPackaging() {
		return packaging;
	}

	public void setPackaging(String packaging) {
		this.packaging = packaging;
	}

	/**
	 * The type of project to generate. Should match one of the advertized type
	 * that the service supports. If not set, the default is retrieved from
	 * the service metadata.
	 */
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	/**
	 * Generates the URL to use to generate a project represented
	 * by this request
	 */
	URI generateUrl(InitializrServiceMetadata metadata) {
		try {
			URIBuilder builder = new URIBuilder(serviceUrl);
			StringBuilder sb = new StringBuilder();
			if (builder.getPath() != null) {
				sb.append(builder.getPath());
			}

			ProjectType projectType = this.type != null ?
					metadata.getProjectTypes().get(type) : metadata.getDefaultType();
			if (projectType == null) {
				throw new IllegalStateException("No project type with id '" + this.type +
						"' - check the service capabilities");
			}
			sb.append(projectType.getAction());
			builder.setPath(sb.toString());

			if (this.bootVersion != null) {
				builder.setParameter("bootVersion", this.bootVersion);
			}
			for (String dependency : dependencies) {
				builder.addParameter("style", dependency);
			}
			if (this.javaVersion != null) {
				builder.setParameter("javaVersion", this.javaVersion);
			}
			if (this.packaging != null) {
				builder.setParameter("packaging", this.packaging);
			}
			if (this.type != null) {
				builder.setParameter("type", projectType.getId());
			}

			return builder.build();
		}
		catch (URISyntaxException e) {
			throw new ProjectGenerationException("Invalid service URL (" + e.getMessage() + ")");
		}
	}

}
