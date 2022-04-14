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

/**
 * Represents the different modes that {@link SpringApplication#run(String...)} supports.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public enum ApplicationRunMode {

	/**
	 * Run the application without any prior optimizations. This represents the default
	 * mode of operation where the application determines what it at runtime.
	 */
	DEFAULT,

	/**
	 * Optimize the application by pre-processing it ahead-of-time. In this mode, no bean
	 * instance is created.
	 */
	OPTIMIZE,

	/**
	 * Run the application with prior optimizations. Can throw an exception if some of
	 * those optimizations cannot be located.
	 */
	RUN_OPTIMIZE;

}
