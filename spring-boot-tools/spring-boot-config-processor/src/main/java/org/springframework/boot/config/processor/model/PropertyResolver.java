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

package org.springframework.boot.config.processor.model;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.springframework.boot.config.processor.util.ModelHelper;
import org.springframework.boot.config.processor.util.ModelUtils;
import org.springframework.boot.config.processor.util.ReadMethodUtils;
import org.springframework.boot.config.processor.util.WriteMethodUtils;

/**
 * Detect the configuration properties of a given entity. Use
 * heuristics and known features of the {@code DataBinder} to
 * detect properties automatically.
 *
 * <p>The following rules apply:
 * <ol>
 * <li>With the exception of {@link Map} and {@link List}, the
 * property type must be a concrete type</li>
 * <li>If the element type is a {@link Map} or a {@link List}, it
 * is detected as an element container</li>
 * <li>If the element type is defined as a nested class of
 * the current configuration class, it is detected as a group and
 * its properties are also harvested recursively</li>
 * <li>In all other cases, a "simple" property is assumed and it
 * must have a getter and a setter</li>
 * </ol>
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class PropertyResolver {

	private final ProcessingEnvironment env;

	private final ModelHelper modelHelper;

	private final PropertyHarvester harvester;

	private final Map<String, ExecutableElement> unresolvedReadMethods;

	private final Map<String, ExecutableElement> unresolvedWriteMethods;

	private final Map<String, VariableElement> unresolvedFields;

	private final EntityModel entityModel;

	public PropertyResolver(ProcessingEnvironment env, EntityModel entityModel) {
		this.env = env;
		this.modelHelper = new ModelHelper(env);
		this.entityModel = entityModel;
		this.harvester = new PropertyHarvester(this.modelHelper);
		this.unresolvedReadMethods = new HashMap<String, ExecutableElement>();
		this.unresolvedWriteMethods = new HashMap<String, ExecutableElement>();
		this.unresolvedFields = new HashMap<String, VariableElement>();
	}


	/**
	 * Resolve the properties.
	 */
	public void resolve() {
		initialize();

		excludeInvalidCandidates();

		handleSetters();

		handleGetters();

		// TODO: handle unresolved getters, fields?
	}

	private void excludeInvalidCandidates() {
		processMethods(unresolvedWriteMethods, new InvalidMethodProcessor(this.modelHelper, true));
		processMethods(unresolvedReadMethods, new InvalidMethodProcessor(this.modelHelper, false));

		// Remove any getter that happens to define a root group itself.
		processMethods(unresolvedReadMethods, new MethodProcessor() {
			@Override
			public boolean process(String name, ExecutableElement method) {
				return ModelUtils.isConfigurationPropertiesAnnotated(method);
			}
		});
	}

	private void handleSetters() {
		processMethods(unresolvedWriteMethods, new MethodProcessor() {
			@Override
			public boolean process(String name, ExecutableElement method) {
				String propertyName = WriteMethodUtils.toPropertyName(name);
				TypeMirror propertyType = ModelUtils.getPropertyType(method, true);
				if (modelHelper.isElementContainerType(propertyType)) {
					handleProperty(resolveGetter(propertyName), method, propertyName);
					return true;
				}
				if (isNestedEntity(propertyType)) {
					handleNestedEntity(modelHelper.safeToTypeElement(propertyType), propertyName);
					return true;
				}
				else {
					ExecutableElement getter = resolveGetter(propertyName);
					if (getter != null) {
						handleProperty(getter, method, propertyName);
						return true;
					}
				}
				return false;
			}
		});

	}

	private void handleGetters() {
		processMethods(unresolvedReadMethods, new MethodProcessor() {
			@Override
			public boolean process(String name, ExecutableElement method) {
				String propertyName = ReadMethodUtils.toPropertyName(name);
				TypeMirror propertyType = method.getReturnType();
				if (modelHelper.isElementContainerType(propertyType)) {
					handleProperty(method, null, propertyName);
					return true;
				}
				if (isNestedEntity(propertyType)) {
					handleNestedEntity(modelHelper.safeToTypeElement(propertyType), propertyName);
					return true;
				}
				return false;
			}
		});
	}

	private boolean isNestedEntity(TypeMirror propertyType) {
		return modelHelper.isConcreteClass(propertyType)
				&& modelHelper.isNestedType(propertyType, entityModel.getTypeElement());
	}

	private void handleProperty(ExecutableElement getter, ExecutableElement setter, String propertyName) {
		PropertyModel property = PropertyModelBuilder.forName(propertyName)
				.withGetter(getter)
				.withSetter(setter)
				.withField(resolveField(propertyName)).build(this.env);
		entityModel.addProperty(property);
	}

	/**
	 * Handle a property that defines a nested entity. Such property is managed
	 * as an {@link EntityModel} on its own.
	 * @param typeElement the type of the element
	 * @param name the name of the property that is used as the local prefix
	 */
	private void handleNestedEntity(TypeElement typeElement, String name) {
		EntityModel childModel = new EntityModel(typeElement);
		childModel.setPrefix(name);
		PropertyResolver resolver = new PropertyResolver(this.env, childModel);
		resolver.resolve();
		entityModel.addNestedEntity(childModel);
	}

	private ExecutableElement resolveGetter(String propertyName) {
		List<String> getterNames = ReadMethodUtils.toMethodNames(propertyName);
		for (String getterName : getterNames) {
			ExecutableElement getter = this.harvester.getReadMethods().get(getterName);
			if (getter != null) {
				this.unresolvedReadMethods.remove(getterName);
				return getter;
			}
		}
		return null;
	}

	private VariableElement resolveField(String propertyName) {
		VariableElement field = this.harvester.getFields().get(propertyName);
		if (field != null) {
			this.unresolvedFields.remove(propertyName);
		}
		return field;
	}

	private void initialize() {
		this.harvester.harvest(this.entityModel.getTypeElement());

		this.unresolvedReadMethods.putAll(this.harvester.getReadMethods());
		this.unresolvedWriteMethods.putAll(this.harvester.getWriteMethods());
		this.unresolvedFields.putAll(this.harvester.getFields());
	}

	/**
	 * Process the specified methods. If the specified {@link MethodProcessor} indicates
	 * that the method has been processed, remove it from the map.
	 * <p>Once this method has  completed, the map contains only the methods that were
	 * not processed by the specified {@code processor}.
	 */
	private void processMethods(Map<String, ExecutableElement> methods, MethodProcessor processor) {
		for (Iterator<Map.Entry<String, ExecutableElement>> it = methods.entrySet().iterator();
			 it.hasNext(); ) {
			Map.Entry<String, ExecutableElement> entry = it.next();
			if (processor.process(entry.getKey(), entry.getValue())) {
				it.remove();
			}
		}
	}

	/**
	 * Filter out any property that is not a concrete type (except special cases for
	 * Map and List that are managed in a specific manner)
	 */
	private static class InvalidMethodProcessor implements MethodProcessor {
		private final ModelHelper modelHelper;

		private final boolean setter;

		private InvalidMethodProcessor(ModelHelper modelHelper, boolean setter) {
			this.modelHelper = modelHelper;
			this.setter = setter;
		}

		@Override
		public boolean process(String name, ExecutableElement method) {
			TypeMirror propertyType = ModelUtils.getPropertyType(method, setter);
			if (this.modelHelper.isElementContainerType(propertyType)) {
				return false;
			}
			if (this.modelHelper.isInterface(propertyType)) {
				return true;
			}
			return false;
		}
	}


	/**
	 * A simple callback to process a method.
	 */
	private static interface MethodProcessor {

		/**
		 * Process the specified method.
		 * @param name the name of the method
		 * @param method the method
		 * @return {@code true} if the method was processed
		 */
		boolean process(String name, ExecutableElement method);
	}

}
