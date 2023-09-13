/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.transaction;

import java.util.Collection;
import java.util.function.Supplier;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Abstracts how a {@link PlatformTransactionManager} is auto-configured based on an
 * initial instance.
 *
 * @author Stephane Nicoll
 * @since 3.2.0
 */
public final class PlatformTransactionManagerFactory {

	private final Supplier<PlatformTransactionManager> supplier;

	private PlatformTransactionManagerFactory(Supplier<PlatformTransactionManager> supplier) {
		this.supplier = supplier;
	}

	/**
	 * Create a factory based on the specified supplier.
	 * @param transactionManager a supplier to the initial state of the
	 * {@link PlatformTransactionManager} to configure.
	 * @return a factory for the specified transaction manager
	 */
	public static PlatformTransactionManagerFactory using(Supplier<PlatformTransactionManager> transactionManager) {
		return new PlatformTransactionManagerFactory(transactionManager);

	}

	@SuppressWarnings("unchecked")
	PlatformTransactionManager create(Collection<? extends PlatformTransactionManagerCustomizer<?>> customizers) {
		PlatformTransactionManager transactionManager = this.supplier.get();
		LambdaSafe.callbacks(PlatformTransactionManagerCustomizer.class, customizers, transactionManager)
			.withLogger(PlatformTransactionManagerFactory.class)
			.invoke((customizer) -> customizer.customize(transactionManager));
		return transactionManager;
	}

}
