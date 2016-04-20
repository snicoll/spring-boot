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

package org.springframework.boot.autoconfigure.web;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.ErrorPage;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.HttpStatus;

/**
 * Scan both templates and static resources to find relevant error pages. Static
 * resources are automatically registered as {@link ErrorPage} instances. Templates
 * are exposed via {@link ErrorPathResolver}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class ErrorPageCustomizer
		implements EmbeddedServletContainerCustomizer, ErrorPathResolver, Ordered {

	private String[] templatesLocations;

	private String[] staticLocations;

	private String prefix = "/error/";

	private final String globalPath;

	private final PathMatchingResourcePatternResolver patternResolver;

	private Map<HttpStatus, String> templateEntries = new LinkedHashMap<HttpStatus, String>();

	private String global4xxTemplate;

	private String global5xxTemplate;

	private Map<HttpStatus, String> staticEntries = new LinkedHashMap<HttpStatus, String>();

	public ErrorPageCustomizer(String[] templatesLocations, String[] staticLocations,
			String globalPath) {
		this.templatesLocations = (templatesLocations != null ? templatesLocations : new String[0]);
		this.staticLocations = (staticLocations != null ? staticLocations : new String[0]);
		this.globalPath = globalPath;
		this.patternResolver = new PathMatchingResourcePatternResolver(
				ErrorPageCustomizer.class.getClassLoader());
	}

	@Override
	public void customize(ConfigurableEmbeddedServletContainer container) {
		if (this.globalPath != null) {
			container.addErrorPages(new ErrorPage(this.globalPath));
		}
		for (Map.Entry<HttpStatus, String> entry : this.staticEntries.entrySet()) {
			if (resolveErrorPath(entry.getKey()) == null) {
				container.addErrorPages(new ErrorPage(entry.getKey(), entry.getValue()));
			}
		}
	}

	@Override
	public String resolveErrorPath(HttpStatus status) {
		String path = this.templateEntries.get(status);
		if (path != null) {
			return path;
		}
		if (status.is4xxClientError() && this.global4xxTemplate != null) {
			return this.global4xxTemplate;
		}
		if (status.is5xxServerError() && this.global5xxTemplate != null) {
			return this.global5xxTemplate;
		}
		return null;
	}

	@Override
	public int getOrder() {
		return 0;
	}

	/**
	 * Scan the registered templates and static locations for error pages.
	 */
	@PostConstruct
	public void scan() {
		try {
			for (String location : this.templatesLocations) {
				registerTemplateErrorPages(location);
			}
			for (String location : this.staticLocations) {
				registerStaticErrorPages(location);
			}
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to register custom error pages", ex);
		}
	}

	protected void registerTemplateErrorPages(String path) throws IOException {
		processResources(cleanTrailingSlash(path), searchFor("*"), new ResourceProcessor() {
			@Override
			public void process(Resource baseResource, Resource resource) throws IOException {
				String candidate = extractFileName(baseResource, resource);
				if (candidate != null) {
					int i = candidate.lastIndexOf(".");
					if (i > 0) {
						String fileName = candidate.substring(0, i);
						if (fileName.equals("4xx") && ErrorPageCustomizer.this.global4xxTemplate == null) {
							ErrorPageCustomizer.this.global4xxTemplate = createErrorPath("4xx");
						}
						else if (fileName.equals("5xx") && ErrorPageCustomizer.this.global5xxTemplate == null) {
							ErrorPageCustomizer.this.global5xxTemplate = createErrorPath("5xx");
						}
						else {
							HttpStatus status = determineHttpStatus(fileName);
							if (status != null && !ErrorPageCustomizer.this.templateEntries.containsKey(status)) {
								String path = createErrorPath(Integer.toString(status.value()));
								ErrorPageCustomizer.this.templateEntries.put(status, path);
							}
						}
					}
				}
			}
		});
	}

	protected void registerStaticErrorPages(String path)
			throws IOException {

		processResources(cleanTrailingSlash(path), searchFor("*.html"), new ResourceProcessor() {
			@Override
			public void process(Resource baseResource, Resource resource) throws IOException {
				String candidate = extractFileName(baseResource, resource);
				if (candidate != null) {
					String fileName = candidate.substring(0, candidate.length() - ".html".length());
					HttpStatus status = determineHttpStatus(fileName);
					if (status != null && !ErrorPageCustomizer.this.staticEntries.containsKey(status)) {
						String path = createErrorPath(status + ".html");
						ErrorPageCustomizer.this.staticEntries.put(status, path);
					}
				}
			}
		});
	}

	private String extractFileName(Resource baseResource, Resource resource)
			throws IOException {
		String basePath = baseResource.getURL().getPath();
		String resourcePath = resource.getURL().getPath();
		if (resourcePath.startsWith(basePath)) {
			return resourcePath.substring(basePath.length() + this.prefix.length());
		}
		return null;
	}

	private HttpStatus determineHttpStatus(String fileName) {
		try {
			return HttpStatus.valueOf(Integer.parseInt(fileName));
		}
		catch (NumberFormatException ex) {
			return null; // Not an integer
		}
		catch (IllegalArgumentException ex) {
			return null; // Not a valid status code
		}
	}

	private String createErrorPath(String path) {
		return this.prefix + path;
	}

	private void processResources(String path, String pattern, ResourceProcessor processor)
			throws IOException {
		Resource baseResource = this.patternResolver.getResource(path);
		if (!baseResource.exists()) {
			return;
		}
		try {
			Resource[] resources = this.patternResolver.getResources(path + pattern);
			for (Resource resource : resources) {
				processor.process(baseResource, resource);
			}
		}
		catch (IOException ex) {
			// Ignore, resources not found
		}
	}

	private String cleanTrailingSlash(String path) {
		if (path.endsWith("/")) {
			return path.substring(0, path.length() - 1);
		}
		return path;
	}

	private String searchFor(String pattern) {
		return this.prefix + pattern;
	}

	private interface ResourceProcessor {

		void process(Resource baseResource, Resource resource) throws IOException;

	}

}
