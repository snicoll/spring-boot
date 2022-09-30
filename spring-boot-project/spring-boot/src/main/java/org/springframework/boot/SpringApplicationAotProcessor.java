/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import org.springframework.boot.SpringApplication.AbandonedRunException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.aot.AotProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.function.ThrowingSupplier;

/**
 * Entry point for AOT processing of a {@link SpringApplication}.
 * <p>
 * <strong>For internal use only.</strong>
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.0.0
 */
public class SpringApplicationAotProcessor extends AotProcessor {

	private final String[] applicationArgs;

	/**
	 * Create a new processor for the specified application and settings.
	 * @param application the application main class
	 * @param applicationArgs the arguments to provide to the main method
	 * @param sourceOutput the location of generated sources
	 * @param resourceOutput the location of generated resources
	 * @param classOutput the location of generated classes
	 * @param groupId the group ID of the application, used to locate
	 * native-image.properties
	 * @param artifactId the artifact ID of the application, used to locate
	 * native-image.properties
	 */
	public SpringApplicationAotProcessor(Class<?> application, String[] applicationArgs, Path sourceOutput,
			Path resourceOutput, Path classOutput, String groupId, String artifactId) {
		super(application, sourceOutput, resourceOutput, classOutput, groupId, artifactId);
		this.applicationArgs = applicationArgs;
	}

	@Override
	protected GenericApplicationContext prepareApplicationContext(Class<?> application) {
		return new AotProcessorHook(application).run(() -> {
			Method mainMethod = application.getMethod("main", String[].class);
			return ReflectionUtils.invokeMethod(mainMethod, null, new Object[] { this.applicationArgs });
		});
	}

	public static void main(String[] args) throws Exception {
		int requiredArgs = 6;
		Assert.isTrue(args.length >= requiredArgs, () -> "Usage: " + SpringApplicationAotProcessor.class.getName()
				+ " <applicationName> <sourceOutput> <resourceOutput> <classOutput> <groupId> <artifactId> <originalArgs...>");
		String applicationName = args[0];
		Path sourceOutput = Paths.get(args[1]);
		Path resourceOutput = Paths.get(args[2]);
		Path classOutput = Paths.get(args[3]);
		String groupId = args[4];
		String artifactId = args[5];
		String[] applicationArgs = (args.length > requiredArgs) ? Arrays.copyOfRange(args, requiredArgs, args.length)
				: new String[0];
		Class<?> application = Class.forName(applicationName);
		new SpringApplicationAotProcessor(application, applicationArgs, sourceOutput, resourceOutput, classOutput,
				groupId, artifactId).process();
	}

	/**
	 * {@link SpringApplicationHook} used to capture the {@link ApplicationContext} and
	 * trigger early exit of main method.
	 */
	private static final class AotProcessorHook implements SpringApplicationHook {

		private final Class<?> application;

		private AotProcessorHook(Class<?> application) {
			this.application = application;
		}

		@Override
		public SpringApplicationRunListener getRunListener(SpringApplication application) {
			return new SpringApplicationRunListener() {

				@Override
				public void contextLoaded(ConfigurableApplicationContext context) {
					throw new AbandonedRunException(context);
				}

			};
		}

		private <T> GenericApplicationContext run(ThrowingSupplier<T> action) {
			try {
				SpringApplication.withHook(this, action);
			}
			catch (AbandonedRunException ex) {
				ApplicationContext context = ex.getApplicationContext();
				Assert.isInstanceOf(GenericApplicationContext.class, context,
						() -> "AOT processing requires a GenericApplicationContext but got a "
								+ context.getClass().getName());
				return (GenericApplicationContext) context;
			}
			throw new IllegalStateException(
					"No application context available after calling main method of '%s'. Does it run a SpringApplication?"
							.formatted(this.application.getName()));
		}

	}

}
