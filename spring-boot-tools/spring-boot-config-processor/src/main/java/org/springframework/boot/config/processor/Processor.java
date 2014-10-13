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

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.springframework.boot.config.ConfigMetadataGroup;
import org.springframework.boot.config.ConfigMetadataItem;
import org.springframework.boot.config.ConfigMetadataRepository;
import org.springframework.boot.config.SimpleConfigMetadataRepository;
import org.springframework.boot.config.processor.mapper.ConfigMetadataRepositoryJsonMapper;
import org.springframework.boot.config.processor.mapper.ConfigMetadataRepositoryMapper;
import org.springframework.boot.config.processor.model.EntityModel;
import org.springframework.boot.config.processor.model.EntityModelFactory;
import org.springframework.boot.config.processor.model.PropertyModel;
import org.springframework.boot.config.processor.util.ModelHelper;
import org.springframework.boot.config.processor.util.ModelUtils;

/**
 *
 * @author Stephane Nicoll
 */
class Processor {

	static final String JSON_METADATA_LOCATION = "META-INF/spring-harvested-config.metadata";

	private final ProcessingEnvironment env;

	private final ModelHelper modelHelper;

	private final Messager msg;

	private final Set<? extends Element> allConfigurationPropertiesElements;

	private final Set<? extends Element> allConditionElements;

	Processor(ProcessingEnvironment env, RoundEnvironment roundEnv) {
		this.env = env;
		this.modelHelper = new ModelHelper(env);
		this.msg = env.getMessager();
		this.allConfigurationPropertiesElements =
				roundEnv.getElementsAnnotatedWith(getConfigurationPropertiesType());
		this.allConditionElements =
				roundEnv.getElementsAnnotatedWith(getConditionalOnPropertyType());
	}

	/**
	 * Detect and parse the configuration entities defined in the current
	 * source code. Write the corresponding metadata to disk
	 */
	public void process() {
		if (!shouldRun()) { // Invalidates compilation round when we have nothing to do
			return;
		}

		Collection<EntityModel> model = parseEntityModel();
		msg.printMessage(Diagnostic.Kind.NOTE, "Processing '" + model.size() + "' entities.");

		// Build the initial repository
		SimpleConfigMetadataRepository repository = buildConfigMetadataRepository(model);

		// Post process the detect the conditions
		harvestConditions(repository);

		// Write the result
		try {
			writeConfigMetadataRepository(repository);
		}
		catch (IOException e) {
			msg.printMessage(Diagnostic.Kind.ERROR, "Failed to write config metadata file (" + e.getMessage() + ")");
		}
	}

	private boolean shouldRun() {
		return !this.allConfigurationPropertiesElements.isEmpty() || !this.allConditionElements.isEmpty();
	}

	private Collection<EntityModel> parseEntityModel() {
		EntityModelFactory factory = new EntityModelFactory(env);
		Collection<EntityModel> result = new ArrayList<EntityModel>();
		for (TypeElement element : getTypeElementCandidates()) {
			EntityModel entityModel = factory.create(element);
			result.add(entityModel);
		}
		for (ExecutableElement method : getExecutableElementCandidates()) {
			if (isMethodCandidate(method)) {
				TypeElement element = modelHelper.safeToTypeElement(method.getReturnType());
				if (element != null) {
					EntityModel entityModel = factory.create(element, method);
					result.add(entityModel);
				}
			}
		}
		return result;
	}

	private SimpleConfigMetadataRepository buildConfigMetadataRepository(Collection<EntityModel> content) {
		SimpleConfigMetadataRepository repository = new SimpleConfigMetadataRepository();
		for (EntityModel entityModel : content) {
			if (!entityModel.isEmpty()) { // Register group only if the model is not empty
				ConfigMetadataGroup rootGroup = ConfigMetadataGroup.root(entityModel.getPrefix());
				fillConfigMetadataGroup(rootGroup, entityModel);
				repository.registerRootGroup(rootGroup);
			}
		}
		return repository;
	}

	private void harvestConditions(SimpleConfigMetadataRepository repository) {
		ConditionHarvester harvester = new ConditionHarvester(repository);
		for (Element conditionElement : allConditionElements) {
			harvester.harvest(conditionElement);
		}
	}

	private void writeConfigMetadataRepository(ConfigMetadataRepository repository) throws IOException {
		ConfigMetadataRepositoryMapper mapper = new ConfigMetadataRepositoryJsonMapper();
		FileObject fileObject = env.getFiler().createResource(
				StandardLocation.CLASS_OUTPUT, "", JSON_METADATA_LOCATION);
		OutputStream out = fileObject.openOutputStream();
		try {
			mapper.writeRepository(repository, out);
		}
		finally {
			out.close();
		}
	}

	private Collection<TypeElement> getTypeElementCandidates() {
		Collection<TypeElement> candidates = new ArrayList<TypeElement>();
		for (Element element : allConfigurationPropertiesElements) {
			if (element instanceof TypeElement) {
				candidates.add((TypeElement) element);
			}
		}
		return candidates;
	}

	private Collection<ExecutableElement> getExecutableElementCandidates() {
		Collection<ExecutableElement> candidates = new ArrayList<ExecutableElement>();
		for (Element element : allConfigurationPropertiesElements) {
			if (element instanceof ExecutableElement) {
				candidates.add((ExecutableElement) element);
			}
		}
		return candidates;
	}

	private boolean isMethodCandidate(ExecutableElement method) {
		return method.getModifiers().contains(Modifier.PUBLIC)
				&& method.getParameters().isEmpty()
				&& (TypeKind.VOID != method.getReturnType().getKind());
	}

	private void fillConfigMetadataGroup(ConfigMetadataGroup group, EntityModel model) {
		StringBuilder sb = new StringBuilder();
		generateType(sb, model.getTypeElement());
		String groupType = sb.toString();
		group.addType(groupType);
		for (PropertyModel property : model.getProperties().values()) {
			group.registerItem(toConfigMetadataItem(property, groupType));
		}
		for (EntityModel nestedEntity : model.getNestedEntities().values()) {
			ConfigMetadataGroup nestedGroup = group.registerGroup(nestedEntity.getPrefix());
			fillConfigMetadataGroup(nestedGroup, nestedEntity);
		}
	}

	private ConfigMetadataItem toConfigMetadataItem(PropertyModel propertyModel, String groupType) {
		ConfigMetadataItem item = new ConfigMetadataItem(propertyModel.getName());
		item.setValueType(propertyModel.getType());
		item.setDescription(propertyModel.getDescription());
		item.addGroupType(groupType);
		return item;
	}

	/**
	 * Generate a type that is similar to what {@code java.lang.Class#getName()}
	 * would return. Allows to identify inner classes with the '$' symbol.
	 */
	private String generateType(StringBuilder sb, Element typeElement) {
		Element enclosingElement = typeElement.getEnclosingElement();
		if (enclosingElement.getKind() == ElementKind.PACKAGE) {
			sb.append(typeElement.toString());
		}
		if (enclosingElement.getKind() == ElementKind.CLASS) {
			generateType(sb, enclosingElement);
			sb.append("$").append(typeElement.getSimpleName());
		}
		return sb.toString();
	}

	private TypeElement getConfigurationPropertiesType() {
		return env.getElementUtils().getTypeElement(ModelUtils.CONFIGURATION_PROPERTIES_FQN);
	}

	private TypeElement getConditionalOnPropertyType() {
		return env.getElementUtils().getTypeElement(ModelUtils.CONDITIONAL_ON_PROPERTY_FQN);
	}
}
