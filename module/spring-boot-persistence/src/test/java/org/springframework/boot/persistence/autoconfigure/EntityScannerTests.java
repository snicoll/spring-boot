/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.persistence.autoconfigure;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.boot.persistence.EntityScan;
import org.springframework.boot.persistence.EntityScanner;
import org.springframework.boot.persistence.autoconfigure.scan.a.EmbeddableA;
import org.springframework.boot.persistence.autoconfigure.scan.a.EntityA;
import org.springframework.boot.persistence.autoconfigure.scan.b.EmbeddableB;
import org.springframework.boot.persistence.autoconfigure.scan.b.EntityB;
import org.springframework.boot.persistence.autoconfigure.scan.c.EmbeddableC;
import org.springframework.boot.persistence.autoconfigure.scan.c.EntityC;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EntityScanner}.
 *
 * @author Phillip Webb
 */
class EntityScannerTests {

	private final Function<BeanFactory, List<String>> defaultPackagesLocator = mock();

	@Test
	void createWhenContextIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new EntityScanner(null, this.defaultPackagesLocator))
			.withMessageContaining("'context' must not be null");
	}

	@Test
	void scanShouldScanFromSinglePackage() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanConfig.class);
		EntityScanner scanner = new EntityScanner(context, this.defaultPackagesLocator);
		Set<Class<?>> scanned = scanner.scan(Entity.class);
		assertThat(scanned).containsOnly(EntityA.class, EntityB.class, EntityC.class);
		then(this.defaultPackagesLocator).shouldHaveNoInteractions();
		context.close();
	}

	@Test
	void scanShouldScanFromResolvedPlaceholderPackage() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("com.example.entity-package=org.springframework.boot.persistence.autoconfigure.scan")
			.applyTo(context);
		context.register(ScanPlaceholderConfig.class);
		context.refresh();
		EntityScanner scanner = new EntityScanner(context, this.defaultPackagesLocator);
		Set<Class<?>> scanned = scanner.scan(Entity.class);
		assertThat(scanned).containsOnly(EntityA.class, EntityB.class, EntityC.class);
		context.close();
	}

	@Test
	void scanShouldScanFromMultiplePackages() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanAConfig.class,
				ScanBConfig.class);
		EntityScanner scanner = new EntityScanner(context, this.defaultPackagesLocator);
		Set<Class<?>> scanned = scanner.scan(Entity.class);
		assertThat(scanned).containsOnly(EntityA.class, EntityB.class);
		context.close();
	}

	@Test
	void scanShouldFilterOnAnnotation() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanConfig.class);
		EntityScanner scanner = new EntityScanner(context, this.defaultPackagesLocator);
		assertThat(scanner.scan(Entity.class)).containsOnly(EntityA.class, EntityB.class, EntityC.class);
		assertThat(scanner.scan(Embeddable.class)).containsOnly(EmbeddableA.class, EmbeddableB.class,
				EmbeddableC.class);
		assertThat(scanner.scan(Entity.class, Embeddable.class)).containsOnly(EntityA.class, EntityB.class,
				EntityC.class, EmbeddableA.class, EmbeddableB.class, EmbeddableC.class);
		context.close();
	}

	@Test
	void scanShouldDefaultToLocatorIfNoPackagesAreFound() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(EmptyConfig.class);
		given(this.defaultPackagesLocator.apply(context))
			.willReturn(List.of("org.springframework.boot.persistence.autoconfigure.scan.c"));
		EntityScanner scanner = new EntityScanner(context, this.defaultPackagesLocator);
		Set<Class<?>> scanned = scanner.scan(Entity.class);
		assertThat(scanned).containsOnly(EntityC.class);
		then(this.defaultPackagesLocator).should().apply(context);
		context.close();
	}

	@Test
	void scanShouldDefaultToEmptyPackagesIfNoLocatorIsSpecified() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(EmptyConfig.class);
		EntityScanner scanner = new EntityScanner(context, null);
		Set<Class<?>> scanned = scanner.scan(Entity.class);
		assertThat(scanned).isEmpty();
		context.close();
	}

	@Test
	void scanShouldUseCustomCandidateComponentProvider() throws ClassNotFoundException {
		ClassPathScanningCandidateComponentProvider candidateComponentProvider = mock(
				ClassPathScanningCandidateComponentProvider.class);
		given(candidateComponentProvider
			.findCandidateComponents("org.springframework.boot.persistence.autoconfigure.scan"))
			.willReturn(Collections.emptySet());
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ScanConfig.class);
		TestEntityScanner scanner = new TestEntityScanner(context, candidateComponentProvider);
		scanner.scan(Entity.class);
		then(candidateComponentProvider).should()
			.addIncludeFilter(
					assertArg((typeFilter) -> assertThat(typeFilter).isInstanceOfSatisfying(AnnotationTypeFilter.class,
							(filter) -> assertThat(filter.getAnnotationType()).isEqualTo(Entity.class))));
		then(candidateComponentProvider).should()
			.findCandidateComponents("org.springframework.boot.persistence.autoconfigure.scan");
		then(candidateComponentProvider).shouldHaveNoMoreInteractions();
	}

	@Test
	void scanShouldScanCommaSeparatedPackagesInPlaceholderPackage() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		TestPropertyValues
			.of("com.example.entity-package=org.springframework.boot.persistence.autoconfigure.scan.a,org.springframework.boot.persistence.autoconfigure.scan.b")
			.applyTo(context);
		context.register(ScanPlaceholderConfig.class);
		context.refresh();
		EntityScanner scanner = new EntityScanner(context, this.defaultPackagesLocator);
		Set<Class<?>> scanned = scanner.scan(Entity.class);
		assertThat(scanned).containsOnly(EntityA.class, EntityB.class);
		context.close();
	}

	private static class TestEntityScanner extends EntityScanner {

		private final ClassPathScanningCandidateComponentProvider candidateComponentProvider;

		TestEntityScanner(ApplicationContext context,
				ClassPathScanningCandidateComponentProvider candidateComponentProvider) {
			super(context, null);
			this.candidateComponentProvider = candidateComponentProvider;
		}

		@Override
		protected ClassPathScanningCandidateComponentProvider createClassPathScanningCandidateComponentProvider(
				ApplicationContext context) {
			return this.candidateComponentProvider;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan("org.springframework.boot.persistence.autoconfigure.scan")
	static class ScanConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackageClasses = EntityA.class)
	static class ScanAConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan(basePackageClasses = EntityB.class)
	static class ScanBConfig {

	}

	@Configuration(proxyBeanMethods = false)
	@EntityScan("${com.example.entity-package}")
	static class ScanPlaceholderConfig {

	}

}
