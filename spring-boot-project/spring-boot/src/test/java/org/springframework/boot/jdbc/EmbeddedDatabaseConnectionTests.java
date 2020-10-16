/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.jdbc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link EmbeddedDatabaseConnection}.
 *
 * @author Stephane Nicoll
 * @author Nidhi Desai
 */
class EmbeddedDatabaseConnectionTests {

	@Test
	void h2CustomDatabaseName() {
		assertThat(EmbeddedDatabaseConnection.H2.getUrl("mydb"))
				.isEqualTo("jdbc:h2:mem:mydb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
	}

	@Test
	void derbyCustomDatabaseName() {
		assertThat(EmbeddedDatabaseConnection.DERBY.getUrl("myderbydb"))
				.isEqualTo("jdbc:derby:memory:myderbydb;create=true");
	}

	@Test
	@Deprecated
	void hsqlCustomDatabaseName() {
		assertThat(EmbeddedDatabaseConnection.HSQL.getUrl("myhsql")).isEqualTo("jdbc:hsqldb:mem:myhsql");
	}

	@Test
	@Deprecated
	void getUrlWithNullDatabaseName() {
		assertThatIllegalArgumentException().isThrownBy(() -> EmbeddedDatabaseConnection.HSQL.getUrl(null))
				.withMessageContaining("DatabaseName must not be empty");
	}

	@Test
	@Deprecated
	void getUrlWithEmptyDatabaseName() {
		assertThatIllegalArgumentException().isThrownBy(() -> EmbeddedDatabaseConnection.HSQL.getUrl("  "))
				.withMessageContaining("DatabaseName must not be empty");
	}

	@Test
	void hsqldbCustomDatabaseName() {
		assertThat(EmbeddedDatabaseConnection.HSQLDB.getUrl("myhsqldb")).isEqualTo("jdbc:hsqldb:mem:myhsqldb");
	}

	@Test
	void getUrlWithNullDatabaseNameForHsqldb() {
		assertThatIllegalArgumentException().isThrownBy(() -> EmbeddedDatabaseConnection.HSQLDB.getUrl(null))
				.withMessageContaining("DatabaseName must not be empty");
	}

	@Test
	void getUrlWithEmptyDatabaseNameForHsqldb() {
		assertThatIllegalArgumentException().isThrownBy(() -> EmbeddedDatabaseConnection.HSQLDB.getUrl("  "))
				.withMessageContaining("DatabaseName must not be empty");
	}

	@Test
	void isEmbeddedForh2CustomDatabaseName() {
		assertThat(EmbeddedDatabaseConnection.isEmbedded("org.h2.Driver", "jdbc:h2:~/test")).isFalse();
	}

	@Test
	void isEmbeddedForh2EmbeddedDatabaseName() {
		assertThat(EmbeddedDatabaseConnection.isEmbedded("org.h2.Driver",
				"jdbc:h2:mem:b3c7d078-1362-4be7-a088-e25dcc3aee32;DB_CLOSE_DELAY=-1")).isTrue();
	}

}
