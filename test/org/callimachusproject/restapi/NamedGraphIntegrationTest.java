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

import java.net.MalformedURLException;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.callimachusproject.test.WebResource;

public class NamedGraphIntegrationTest extends TemporaryServerIntegrationTestCase {

	private String requestSlug = "namedGraph.rdf";
	private String requestContentType = "application/rdf+xml";
	private String outputString = "<rdf:RDF \n"
			+ "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n"
			+ "xmlns:dcterms=\"http://purl.org/dc/terms/\"> \n"
			+ "<rdf:Description rdf:about=\"urn:x-states:New%20York\"> \n"
			+ "<dcterms:alternative>NY</dcterms:alternative> \n"
			+ "</rdf:Description> \n" + "</rdf:RDF>";
	private String updateOutputString = "<rdf:RDF \n"
			+ "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n"
			+ "xmlns:dcterms=\"http://purl.org/dc/terms/\"> \n"
			+ "<rdf:Description rdf:about=\"urn:x-states:New%20York\"> \n"
			+ "<dcterms:alternative>New York (UPDATED)</dcterms:alternative> \n"
			+ "</rdf:Description> \n" + "</rdf:RDF>";

	public void testCreate() throws MalformedURLException, Exception {
		getHomeFolder()
				.rel("contents", "application/atom+xml")
				.getAppCollection()
				.create(requestSlug, requestContentType,
						outputString.getBytes())
				.rel("edit-media", requestContentType).delete();
	}

	public void testRetrieve() throws Exception {
		WebResource resource = getHomeFolder()
				.rel("contents", "application/atom+xml")
				.getAppCollection()
				.create(requestSlug, requestContentType,
						outputString.getBytes());
		String text = new String(resource
				.rel("edit-media", requestContentType).get(requestContentType));
		resource.rel("edit-media", requestContentType).delete();
		assertTrue(text.contains("urn:x-states:New%20York"));
	}

	public void testUpdate() throws Exception {
		WebResource resource = getHomeFolder()
				.rel("contents", "application/atom+xml")
				.getAppCollection()
				.create(requestSlug, requestContentType,
						outputString.getBytes());
		resource.rel("edit-media", requestContentType).put(requestContentType,
				updateOutputString.getBytes());
		resource.rel("edit-media", requestContentType).delete();
	}

	public void testDelete() throws Exception {
		getHomeFolder()
				.rel("contents", "application/atom+xml")
				.getAppCollection()
				.create(requestSlug, requestContentType,
						outputString.getBytes())
				.rel("edit-media", requestContentType).delete();
	}
}
