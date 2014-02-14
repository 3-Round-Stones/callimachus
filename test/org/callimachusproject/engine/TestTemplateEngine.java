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
package org.callimachusproject.engine;

import java.io.Reader;
import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.http.client.HttpClient;
import org.callimachusproject.client.HttpClientFactory;

public class TestTemplateEngine extends TestCase {
	private static final String SYSTEM_ID = "http://example.com/";
	private final HttpClient client = HttpClientFactory.getInstance()
			.createHttpClient(SYSTEM_ID);

	public void setUp() throws Exception {
		super.setUp();
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testInferTrue() throws Exception {
		Reader in = new StringReader(
				"<ul xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'>\n"
						+ "<li resource='?url'><span property='rdfs:label'/></li></ul>");
		TemplateEngine eng = TemplateEngine.newInstance(client);
		Template tem = eng.getTemplate(in, SYSTEM_ID);
		String qry = tem.getQueryString("# @infer true\n"
				+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "SELECT ?url {[a ?url]}");
		assertTrue(qry, qry.contains("# @infer true\n"));
		ParameterizedQueryParser parser = ParameterizedQueryParser
				.newInstance();
		ParameterizedQuery query = parser.parseQuery(qry, SYSTEM_ID);
		assertTrue(query.getIncludeInferred());
	}

	public void testInferFalse() throws Exception {
		Reader in = new StringReader(
				"<ul xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'>\n"
						+ "<li resource='?url'><span property='rdfs:label'/></li></ul>");
		TemplateEngine eng = TemplateEngine.newInstance(client);
		Template tem = eng.getTemplate(in, SYSTEM_ID);
		String qry = tem.getQueryString("# @infer false\n"
				+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "SELECT ?url {[a ?url]}");
		assertTrue(qry, qry.contains("# @infer false\n"));
		ParameterizedQueryParser parser = ParameterizedQueryParser
				.newInstance();
		ParameterizedQuery query = parser.parseQuery(qry, SYSTEM_ID);
		assertFalse(query.getIncludeInferred());
	}

	public void testInferFalseBelow() throws Exception {
		Reader in = new StringReader(
				"<ul xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'>\n"
						+ "<li resource='?url'><span property='rdfs:label'/></li></ul>");
		TemplateEngine eng = TemplateEngine.newInstance(client);
		Template tem = eng.getTemplate(in, SYSTEM_ID);
		String qry = tem.getQueryString("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "# @infer false\n"
				+ "SELECT ?url {[a ?url]}");
		assertTrue(qry, qry.contains("# @infer false\n"));
		ParameterizedQueryParser parser = ParameterizedQueryParser
				.newInstance();
		ParameterizedQuery query = parser.parseQuery(qry, SYSTEM_ID);
		assertFalse(query.getIncludeInferred());
	}

	public void testCanonicalReferenceQuery() throws Exception {
		Reader in = new StringReader(
				"<ul xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'>\n"
						+ "<li resource='?url'>\n" +
						"<span rel='rdfs:seeAlso' resource='http://example.com'/>\n" +
						"<span property='rdfs:label'/></li></ul>");
		TemplateEngine eng = TemplateEngine.newInstance(client);
		Template tem = eng.getTemplate(in, SYSTEM_ID);
		String qry = tem.getQueryString();
		assertTrue(qry, qry.contains("<http://example.com/>"));
	}

}
