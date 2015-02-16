/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.configurationprocessor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.WildcardType;
import javax.lang.model.util.Types;

import org.springframework.boot.configurationprocessor.metadata.TypeDescriptor;

/**
 * Type Utilities.
 *
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 1.2.0
 */
class TypeUtils {

	private static final Map<TypeKind, Class<?>> PRIMITIVE_WRAPPERS;
	static {
		Map<TypeKind, Class<?>> wrappers = new HashMap<TypeKind, Class<?>>();
		wrappers.put(TypeKind.BOOLEAN, Boolean.class);
		wrappers.put(TypeKind.BYTE, Byte.class);
		wrappers.put(TypeKind.CHAR, Character.class);
		wrappers.put(TypeKind.DOUBLE, Double.class);
		wrappers.put(TypeKind.FLOAT, Float.class);
		wrappers.put(TypeKind.INT, Integer.class);
		wrappers.put(TypeKind.LONG, Long.class);
		wrappers.put(TypeKind.SHORT, Short.class);
		PRIMITIVE_WRAPPERS = Collections.unmodifiableMap(wrappers);
	}

	private final ProcessingEnvironment env;

	private final TypeMirror collectionType;

	private final TypeMirror mapType;

	private final TypeMirror objectType;

	public TypeUtils(ProcessingEnvironment env) {
		this.env = env;
		Types types = env.getTypeUtils();
		WildcardType wc = types.getWildcardType(null, null);
		this.collectionType = types.getDeclaredType(this.env.getElementUtils()
				.getTypeElement(Collection.class.getName()), wc);
		this.mapType = types.getDeclaredType(
				this.env.getElementUtils().getTypeElement(Map.class.getName()), wc, wc);
		this.objectType = types.getDeclaredType(this.env.getElementUtils()
						.getTypeElement(Object.class.getName()));

	}

	public TypeDescriptor getTypeDescriptor(TypeMirror type) {
		if (type == null) {
			return null;
		}
		if (isMap(type)) {
			TypeMirror[] entryType = getMapEntryType(type);
			TypeDescriptor keyTypeDescriptor = getTypeDescriptor(entryType[0]);
			TypeDescriptor valueTypeDescriptor = getTypeDescriptor(entryType[1]);
			return new TypeDescriptor(getType(type), valueTypeDescriptor, keyTypeDescriptor.getType());
		}
		else if (isCollection(type)) {
			TypeMirror elementType = getCollectionElementType(type);
			TypeDescriptor elementTypeDescriptor = getTypeDescriptor(elementType);
			return new TypeDescriptor(getType(type), elementTypeDescriptor, null);
		}
		else {
			return new TypeDescriptor(getType(type));
		}
	}

	public String getType(Element element) {
		return getType(element == null ? null : element.asType());
	}

	public String getType(TypeMirror type) {
		if (type == null) {
			return null;
		}
		Class<?> wrapper = PRIMITIVE_WRAPPERS.get(type.getKind());
		if (wrapper != null) {
			return wrapper.getName();
		}
		if (type instanceof DeclaredType) {
			DeclaredType declaredType = (DeclaredType) type;
			Element enclosingElement = declaredType.asElement().getEnclosingElement();
			if (enclosingElement != null && enclosingElement instanceof TypeElement) {
				return getType(enclosingElement) + "$"
						+ declaredType.asElement().getSimpleName().toString();
			}
		}
		return type.toString();
	}

	public boolean isCollectionOrMap(TypeMirror type) {
		return isCollection(type) || isMap(type);
	}

	public boolean isEnclosedIn(Element candidate, TypeElement element) {
		if (candidate == null || element == null) {
			return false;
		}
		if (candidate.equals(element)) {
			return true;
		}
		return isEnclosedIn(candidate.getEnclosingElement(), element);
	}

	public String getJavaDoc(Element element) {
		String javadoc = (element == null ? null : this.env.getElementUtils()
				.getDocComment(element));
		if (javadoc != null) {
			javadoc = javadoc.trim();
		}
		return ("".equals(javadoc) ? null : javadoc);
	}

	private boolean isCollection(TypeMirror type) {
		return this.env.getTypeUtils().isAssignable(type, this.collectionType);
	}

	private boolean isMap(TypeMirror type) {
		return this.env.getTypeUtils().isAssignable(type, this.mapType);
	}

	private TypeMirror getCollectionElementType(TypeMirror type) {
		TypeElement typeElement = (TypeElement) this.env.getTypeUtils().asElement(type);
		if (typeElement.getQualifiedName().contentEquals(Collection.class.getName())) {
			DeclaredType declaredType = (DeclaredType) type;
			if (declaredType.getTypeArguments().size() == 0) { // raw type, just "Collection"
				return this.objectType;
			} else {
				return declaredType.getTypeArguments().get(0);
			}
		}
		for (TypeMirror superType : this.env.getTypeUtils().directSupertypes(type)) {
			if (isCollection(superType)) {
				return getCollectionElementType(superType);
			}
		}
		throw new IllegalStateException("Not a collection type " + type.toString());
	}

	private TypeMirror[] getMapEntryType(TypeMirror type) {
		TypeElement typeElement = (TypeElement) this.env.getTypeUtils().asElement(type);
		if (typeElement.getQualifiedName().contentEquals(Map.class.getName())) {
			DeclaredType declaredType = (DeclaredType) type;
			if (declaredType.getTypeArguments().size() == 0) { // raw type, just "Map"
				return new TypeMirror[]{this.objectType, this.objectType};
			} else {
				TypeMirror keyType = declaredType.getTypeArguments().get(0);
				TypeMirror valueType = declaredType.getTypeArguments().get(1);
				return new TypeMirror[]{keyType, valueType};
			}
		}
		for (TypeMirror superType : this.env.getTypeUtils().directSupertypes(type)) {
			if (isMap(superType)) {
				return getMapEntryType(superType);
			}
		}
		throw new IllegalStateException("Not a map type " + type.toString());
	}

}
