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
package org.callimachusproject.restapi;

import java.net.URLEncoder;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.junit.Test;

public class SparqlEndpointIntegrationTest extends TemporaryServerIntegrationTestCase {

	private static final String QUERY = "application/sparql-query";
	private static final String UPDATE = "application/sparql-update";
	private static final String URLENCODED = "application/x-www-form-urlencoded";
	private static final String RESULTS_XML = "application/sparql-results+xml";

	@Test
	public void testGetQueryParameter() throws Exception {
		String sparql = "ASK { ?s ?p ?o }";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String results = new String(getHomeFolder().sparqlEndpoint().ref("?query=" + encoded).get(RESULTS_XML));
		assertTrue(results.contains("true"));
	}

	@Test
	public void testPostQueryForm() throws Exception {
		String sparql = "ASK { ?s ?p ?o }";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String form = "query=" + encoded;
		String results = new String(getHomeFolder().sparqlEndpoint().post(URLENCODED, form.getBytes(), RESULTS_XML));
		assertTrue(results.contains("true"));
	}

	@Test
	public void testPostQueryDirectly() throws Exception {
		String sparql = "ASK { ?s ?p ?o }";
		String results = new String(getHomeFolder().sparqlEndpoint().post(QUERY, sparql.getBytes(), RESULTS_XML));
		assertTrue(results.contains("true"));
	}

	@Test
	public void testPostUpdateForm() throws Exception {
		String sparql = "INSERT DATA { </> a </> }";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String form = "update=" + encoded;
		getHomeFolder().sparqlEndpoint().post(URLENCODED, form.getBytes());
	}

	@Test
	public void testPostUpdateDirectly() throws Exception {
		String sparql = "INSERT DATA { </> a </> }";
		getHomeFolder().sparqlEndpoint().post(UPDATE, sparql.getBytes());
	}

}
