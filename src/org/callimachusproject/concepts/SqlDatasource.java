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

}