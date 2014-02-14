/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
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
