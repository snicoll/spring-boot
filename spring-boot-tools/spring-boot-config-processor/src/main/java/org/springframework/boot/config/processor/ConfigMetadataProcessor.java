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

package org.springframework.boot.config.processor;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

import org.springframework.boot.config.processor.util.ModelUtils;

/**
 * Parse the configuration items defined in the current source code and generate
 * a metadata repository with them.
 *
 * <p>This metadata repository can be loaded in external tools to offer configuration
 * related features
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
@SupportedAnnotationTypes({ModelUtils.CONFIGURATION_PROPERTIES_FQN})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ConfigMetadataProcessor extends AbstractProcessor {

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		final Processor processor = new Processor(processingEnv, roundEnv);
		processor.process();
		return false;
	}

}
