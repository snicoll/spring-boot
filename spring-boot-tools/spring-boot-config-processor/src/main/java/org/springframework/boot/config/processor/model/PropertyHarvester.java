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

import static org.springframework.boot.config.processor.util.ModelUtils.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import org.springframework.boot.config.processor.util.ModelHelper;
import org.springframework.boot.config.processor.util.ReadMethodUtils;
import org.springframework.boot.config.processor.util.WriteMethodUtils;

/**
 * Harvests an type to build the list of candidate methods and fields used
 * to manage its properties.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class PropertyHarvester {

	private final ModelHelper modelHelper;

	private final Map<String, ExecutableElement> readMethods;

	private final Map<String, ExecutableElement> writeMethods;

	private final Map<String, VariableElement> fields;


	public PropertyHarvester(ModelHelper modelHelper) {
		this.modelHelper = modelHelper;
		this.readMethods = new HashMap<String, ExecutableElement>();
		this.writeMethods = new HashMap<String, ExecutableElement>();
		this.fields = new HashMap<String, VariableElement>();
	}

	/**
	 * Harvests the specified element and its super type(s), if any.
	 * @see #getReadMethods()
	 * @see #getWriteMethods()
	 * @see #getFields()
	 */
	public void harvest(TypeElement typeElement) {
		// Harvest the base fields
		doHarvest(typeElement);

		// Harvest the parent types
		TypeMirror superType = this.modelHelper.getFirstSuperType(typeElement.asType());
		if (superType != null) {
			TypeElement superTypeElement = modelHelper.safeToTypeElement(superType);
			harvest(superTypeElement);
		}
	}

	/**
	 * Return the harvested getters.
	 */
	public Map<String, ExecutableElement> getReadMethods() {
		return readMethods;
	}

	/**
	 * Return the harvested setters.
	 */
	public Map<String, ExecutableElement> getWriteMethods() {
		return writeMethods;
	}

	/**
	 * Return the harvested fields.
	 */
	public Map<String, VariableElement> getFields() {
		return fields;
	}

	private void doHarvest(TypeElement typeElement) {
		for (ExecutableElement method : getPublicMethods(typeElement)) {
			String methodName = getName(method);
			if (ReadMethodUtils.isGetter(method) && !readMethods.containsKey(methodName)) {
				readMethods.put(methodName, method);
			}
			else if (WriteMethodUtils.isSetter(method) && !writeMethods.containsKey(methodName)) {
				writeMethods.put(methodName, method);
			}
		}
		for (VariableElement field : getFields(typeElement)) {
			String fieldName = getName(field);
			if (!fields.containsKey(fieldName)) {
				fields.put(fieldName, field);
			}
		}
	}

	private List<ExecutableElement> getPublicMethods(TypeElement typeElement) {
		final List<ExecutableElement> methods =
				new ArrayList<ExecutableElement>();

		for (ExecutableElement method : ElementFilter
				.methodsIn(typeElement.getEnclosedElements())) {
			if (method.getModifiers().contains(Modifier.PUBLIC)) {
				methods.add(method);
			}
		}
		return methods;
	}

	private List<VariableElement> getFields(TypeElement typeElement) {
		final List<VariableElement> fields = new ArrayList<VariableElement>();

		for (VariableElement variableElement : ElementFilter
				.fieldsIn(typeElement.getEnclosedElements())) {
			fields.add(variableElement);
		}
		return fields;
	}

}
