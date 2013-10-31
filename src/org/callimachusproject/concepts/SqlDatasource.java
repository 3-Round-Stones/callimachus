package org.callimachusproject.concepts;

import java.util.Set;

import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.RDFObject;

/** JDBC database connection information */
@Iri("http://callimachusproject.org/rdf/2009/framework#SqlDatasource")
public interface SqlDatasource {
	/** Java class name of the driver that should be used */
	@Iri("http://callimachusproject.org/rdf/2009/framework#driverClassName")
	Set<String> getCtrlDriverClassName();
	/** Java class name of the driver that should be used */
	@Iri("http://callimachusproject.org/rdf/2009/framework#driverClassName")
	void setCtrlDriverClassName(Set<? extends String> ctrlDriverClassName);

	/** JAR that contains the driver class or one of its dependencies */
	@Iri("http://callimachusproject.org/rdf/2009/framework#driverJar")
	Set<RDFObject> getCtrlDriverJar();
	/** JAR that contains the driver class or one of its dependencies */
	@Iri("http://callimachusproject.org/rdf/2009/framework#driverJar")
	void setCtrlDriverJar(Set<?> ctrlDriverJar);

	/** "The JDBC connection url for connecting to the database. */
	@Iri("http://callimachusproject.org/rdf/2009/framework#jdbcUrl")
	String getCtrlJdbcUrl();
	/** "The JDBC connection url for connecting to the database. */
	@Iri("http://callimachusproject.org/rdf/2009/framework#jdbcUrl")
	void setCtrlJdbcUrl(String ctrlJdbcUrl);

	/** Maximum number of database connections in pool */
	@Iri("http://callimachusproject.org/rdf/2009/framework#maxActive")
	Number getCtrlMaxActive();
	/** Maximum number of database connections in pool */
	@Iri("http://callimachusproject.org/rdf/2009/framework#maxActive")
	void setCtrlMaxActive(Number ctrlMaxActive);

	/** Maximum number of idle database connections to retain in pool */
	@Iri("http://callimachusproject.org/rdf/2009/framework#maxIdle")
	Number getCtrlMaxIdle();
	/** Maximum number of idle database connections to retain in pool */
	@Iri("http://callimachusproject.org/rdf/2009/framework#maxIdle")
	void setCtrlMaxIdle(Number ctrlMaxIdle);

	/** Maximum time to wait for a database connection to become available in ms */
	@Iri("http://callimachusproject.org/rdf/2009/framework#maxWait")
	Number getCtrlMaxWait();
	/** Maximum time to wait for a database connection to become available in ms */
	@Iri("http://callimachusproject.org/rdf/2009/framework#maxWait")
	void setCtrlMaxWait(Number ctrlMaxWait);

}