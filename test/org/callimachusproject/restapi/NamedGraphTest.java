package org.callimachusproject.restapi;

import java.net.MalformedURLException;

import org.callimachusproject.test.TemporaryServerTestCase;
import org.callimachusproject.test.WebResource;

public class NamedGraphTest extends TemporaryServerTestCase {

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
				.link("contents", "application/atom+xml")
				.getAppCollection()
				.create(requestSlug, requestContentType,
						outputString.getBytes())
				.link("edit-media", requestContentType).delete();
	}

	public void testRetrieve() throws Exception {
		WebResource resource = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection()
				.create(requestSlug, requestContentType,
						outputString.getBytes());
		String text = new String(resource
				.link("edit-media", requestContentType).get(requestContentType));
		resource.link("edit-media", requestContentType).delete();
		assertTrue(text.contains("urn:x-states:New%20York"));
	}

	public void testUpdate() throws Exception {
		WebResource resource = getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection()
				.create(requestSlug, requestContentType,
						outputString.getBytes());
		resource.link("edit-media", requestContentType).put(requestContentType,
				updateOutputString.getBytes());
		resource.link("edit-media", requestContentType).delete();
	}

	public void testDelete() throws Exception {
		getHomeFolder()
				.link("contents", "application/atom+xml")
				.getAppCollection()
				.create(requestSlug, requestContentType,
						outputString.getBytes())
				.link("edit-media", requestContentType).delete();
	}
}
