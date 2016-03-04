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

package org.springframework.boot.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Generate a build.info file based the content of the {@link MavenProject}.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Mojo(name = "generate-build-info", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class GenerateBuildInfoMojo extends AbstractMojo {

	/**
	 * The Maven project.
	 */
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	/**
	 * The location of the generated build.info.
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/boot/build.properties")
	private File outputFile;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		Properties properties = createBuildInfo();
		FileOutputStream fos = null;
		try {
			createFileIfNecessary(this.outputFile);
			fos = new FileOutputStream(this.outputFile);
			properties.store(fos, "Properties");
		}
		catch (FileNotFoundException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		finally {
			try {
				if (fos != null) {
					fos.close();
				}
			}
			catch (IOException e) {
				getLog().error("Error closing FileOutputStream: " + fos);
			}
		}
	}

	protected Properties createBuildInfo() {
		Properties properties = new Properties();
		properties.put("build.group", this.project.getGroupId());
		properties.put("build.artifact", this.project.getArtifactId());
		properties.put("build.version", this.project.getVersion());
		properties.put("build.timestamp", new Date().toString());
		return properties;
	}

	private void createFileIfNecessary(File file) throws MojoExecutionException, IOException {
		if (file.exists()) {
			return;
		}
		File parent = file.getParentFile();
		if (!parent.exists() && !parent.mkdirs()) {
			throw new MojoExecutionException("Cannot create parent directory for '"
					+ this.outputFile.getAbsolutePath() + "'");
		}
		if (!file.createNewFile()) {
			throw new MojoExecutionException("Cannot create target file '"
					+ this.outputFile.getAbsolutePath() + "'");
		}
	}

}
