package org.callimachusproject.repository;

import java.io.IOException;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;

public interface CalliRepositoryMXBean {

	int getMaxQueryTime();

	void setMaxQueryTime(int maxQueryTime);

	boolean isIncludeInferred();

	void setIncludeInferred(boolean includeInferred);

	String getChangeFolder() throws OpenRDFException;

	void setChangeFolder(String uriSpace) throws OpenRDFException;

	void addSchemaGraphType(String rdfType) throws RepositoryException;

	void setSchemaGraphType(String rdfType) throws RepositoryException;

	boolean isCompileRepository();

	void setCompileRepository(boolean compileRepository)
			throws ObjectStoreConfigException, RepositoryException;

	/**
	 * Resolves the relative path to the callimachus webapp context installed at
	 * the origin.
	 * 
	 * @param origin
	 *            scheme and authority
	 * @param path
	 *            relative path from the Callimachus webapp context
	 * @return absolute URL of the root + webapp context + path (or null)
	 */
	String getCallimachusUrl(String origin, String path)
			throws OpenRDFException;

	boolean isTracingCalls();

	void setTracingCalls(boolean trace);

	boolean isLoggingCalls();

	void setLoggingCalls(boolean trace);

	String[] showActiveCalls();

	String[] showTraceSummary() throws IOException;

	void resetTraceAnalysis();

	String[] sparqlQuery(String query) throws OpenRDFException, IOException;

	void sparqlUpdate(String update) throws OpenRDFException, IOException;

	String getBlob(String uri) throws OpenRDFException, IOException;

	void storeBlob(String uri, String content) throws OpenRDFException, IOException;

}