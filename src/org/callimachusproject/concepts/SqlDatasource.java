/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.concepts;

import java.util.Set;

import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.RDFObject;

/** JDBC database connection information */
@Iri("http://callimachusproject.org/rdf/2009/framework#SqlDatasource")
public interface SqlDatasource {
	/** Java class name of the driver that should be used */
	@Iri("http://callimachusproject.org/rdf/2009/framework#driverClassName")
	Set<String> getCalliDriverClassName();
	/** Java class name of the driver that should be used */
	@Iri("http://callimachusproject.org/rdf/2009/framework#driverClassName")
	void setCalliDriverClassName(Set<? extends String> calliDriverClassName);

	/** JAR that contains the driver class or one of its dependencies */
	@Iri("http://callimachusproject.org/rdf/2009/framework#driverJar")
	Set<RDFObject> getCalliDriverJar();
	/** JAR that contains the driver class or one of its dependencies */
	@Iri("http://callimachusproject.org/rdf/2009/framework#driverJar")
	void setCalliDriverJar(Set<?> calliDriverJar);

	/** "The JDBC connection url for connecting to the database. */
	@Iri("http://callimachusproject.org/rdf/2009/framework#jdbcUrl")
	String getCalliJdbcUrl();
	/** "The JDBC connection url for connecting to the database. */
	@Iri("http://callimachusproject.org/rdf/2009/framework#jdbcUrl")
	void setCalliJdbcUrl(String calliJdbcUrl);

	/** Maximum number of database connections in pool */
	@Iri("http://callimachusproject.org/rdf/2009/framework#maxActive")
	Number getCalliMaxActive();
	/** Maximum number of database connections in pool */
	@Iri("http://callimachusproject.org/rdf/2009/framework#maxActive")
	void setCalliMaxActive(Number calliMaxActive);

	/** Maximum number of idle database connections to retain in pool */
	@Iri("http://callimachusproject.org/rdf/2009/framework#maxIdle")
	Number getCalliMaxIdle();
	/** Maximum number of idle database connections to retain in pool */
	@Iri("http://callimachusproject.org/rdf/2009/framework#maxIdle")
	void setCalliMaxIdle(Number calliMaxIdle);

	/** Maximum time to wait for a database connection to become available in ms */
	@Iri("http://callimachusproject.org/rdf/2009/framework#maxWait")
	Number getCalliMaxWait();
	/** Maximum time to wait for a database connection to become available in ms */
	@Iri("http://callimachusproject.org/rdf/2009/framework#maxWait")
	void setCalliMaxWait(Number calliMaxWait);

	/** SQL SELECT query to validate connections */
	@Iri("http://callimachusproject.org/rdf/2009/framework#validationQuery")
	String getCalliValidationQuery();
	/** SQL SELECT query to validate connections */
	@Iri("http://callimachusproject.org/rdf/2009/framework#validationQuery")
	void setCalliValidationQuery(String calliValidationQuery);

}
