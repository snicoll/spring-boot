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

package org.springframework.boot.config.processor.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

/**
 * Provide model-related utilities based on a {@link ProcessingEnvironment}.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
public class ModelHelper {

	private final ProcessingEnvironment env;

	private final Types typeUtils;

	private final TypeMirror collectionType;

	private final TypeMirror mapType;

	public ModelHelper(ProcessingEnvironment env) {
		this.env = env;
		this.typeUtils = env.getTypeUtils();
		this.collectionType = getRawTypeMirror(Collection.class, 1);
		this.mapType = getRawTypeMirror(Map.class, 2);

	}

	/**
	 * Specify if the {@link TypeMirror} represents to a concrete class. If the
	 * type is an interface or any other type (primitive, etc), this returns
	 * {@code false}.
	 */
	public boolean isConcreteClass(TypeMirror typeMirror) {
		TypeElement typeElement = safeToTypeElement(typeMirror);
		return (typeElement != null && typeElement.getKind() == ElementKind.CLASS);
	}

	/**
	 * Specify if the {@link TypeMirror} represents to an interface. If the
	 * type is a class or any other type (primitive, etc), this returns
	 * {@code false}.
	 */
	public boolean isInterface(TypeMirror typeMirror) {
		TypeElement typeElement = safeToTypeElement(typeMirror);
		return (typeElement != null && typeElement.getKind() == ElementKind.INTERFACE);
	}

	/**
	 * Specify if the {@link TypeMirror} represents a container (i.e. a Map
	 * or a Collection).
	 */
	public boolean isElementContainerType(TypeMirror propertyType) {
		return isMapType(propertyType) || isCollectionType(propertyType);
	}

	/**
	 * Specify if the {@link TypeMirror} represents a {@link Collection}.
	 */
	public boolean isCollectionType(TypeMirror typeMirror) {
		return typeUtils.isAssignable(typeMirror, collectionType);
	}

	/**
	 * Specify if the {@link TypeMirror} represents a {@link Map}.
	 */
	public boolean isMapType(TypeMirror typeMirror) {
		return typeUtils.isAssignable(typeMirror, mapType);
	}

	/**
	 * Specify if the {@link TypeMirror} is an inner element of the specified parent.
	 */
	public boolean isNestedType(TypeMirror typeMirror, TypeElement parentCandidate) {
		TypeElement typeElement = safeToTypeElement(typeMirror);
		if (typeElement != null) {
			Element enclosingElement = typeElement.getEnclosingElement();
			return (enclosingElement != null && enclosingElement.equals(parentCandidate));
		}
		return false;
	}

	/**
	 * Return the parent class of the specified {@link TypeMirror} or {@code null} if
	 * this type has no parent. Interfaces are not taken into account.
	 */
	public TypeMirror getFirstSuperType(TypeMirror type) {
		final List<? extends TypeMirror> directSuperTypes = env.getTypeUtils().directSupertypes(type);
		if (directSuperTypes.isEmpty()) {
			return null; // reached java.lang.Object
		}
		return directSuperTypes.get(0); // The first is the super class
	}

	/**
	 * Return the {@link TypeMirror} of the {@link Map} interface that
	 * this type implements or {@code null} if such type is not a map.
	 */
	public TypeMirror getMapType(TypeMirror type) {
		TypeElement typeElement = safeToTypeElement(type);
		if (typeElement != null && typeElement.getQualifiedName().contentEquals(Map.class.getName())) {
			return type;
		}
		for (TypeMirror superType : env.getTypeUtils().directSupertypes(type)) {
			if (isMapType(superType)) {
				return getMapType(superType);
			}
		}
		return null;
	}

	/**
	 * Return the {@link TypeMirror} of the {@link Collection} interface that
	 * this type implements or {@code null} if such type is not a collection.
	 */
	public TypeMirror getCollectionType(TypeMirror type) {
		TypeElement typeElement = safeToTypeElement(type);
		if (typeElement != null && typeElement.getQualifiedName().contentEquals(Collection.class.getName())) {
			return type;
		}
		for (TypeMirror superType : env.getTypeUtils().directSupertypes(type)) {
			if (isCollectionType(superType)) {
				return getCollectionType(superType);
			}
		}
		return null;
	}

	/**
	 * Return the {@link TypeElement} corresponding to the specified type or {@code null}
	 * if the {@link TypeMirror} does not represent a class/interface.
	 */
	public TypeElement safeToTypeElement(TypeMirror typeMirror) {
		Element element = env.getTypeUtils().asElement(typeMirror);
		if (element != null && element instanceof TypeElement) {
			return (TypeElement) element;
		}
		return null;
	}

	private TypeMirror getRawTypeMirror(Class<?> clazz, int genericParameters) {
		final TypeElement collectionClassElement = env.getElementUtils().getTypeElement(clazz.getName());
		TypeMirror[] genericArgs = new TypeMirror[genericParameters];
		for (int i = 0; i < genericParameters; i++) {
			genericArgs[i] = typeUtils.getWildcardType(null, null);
		}
		return typeUtils.getDeclaredType(collectionClassElement, genericArgs);
	}

}
