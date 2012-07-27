package org.callimachusproject.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.rel;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.callimachusproject.server.concepts.HTTPFileObject;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.URI;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class ContentNegotiationTest extends MetadataServerTestCase {

	public static class Alternate {
		@rel("alternate")
		@query("boolean")
		@type("application/sparql-results+xml")
		public boolean getBoolean() {
			return true;
		}

		@rel("alternate")
		@query("rdf")
		@type("application/rdf+xml")
		public Model getModel() {
			return new LinkedHashModel();
		}

		@query("my")
		@type({"application/rdf+xml", "text/turtle", "application/x-turtle"})
		public Model getMyModel() {
			LinkedHashModel model = new LinkedHashModel();
			URI uri = new URIImpl("urn:root");
			Literal lit = new LiteralImpl(new Date().toString());
			model.add(uri, uri, lit);
			return model;
		}

		@query("my")
		public void setMyModel(@type("application/rdf+xml") Model model) {
		}

		@query("my")
		@type({"application/sparql-results+xml", "text/plain"})
		public boolean getMyBoolean() {
			return true;
		}

		@query("my")
		public void setMyBoolean(@type("*/*") boolean bool) {
		}

		@query("my")
		public String postRDF(@type("application/rdf+xml") InputStream in) {
			return "rdf+xml";
		}

		@query("my")
		@type("text/plain")
		public String postSPARQL(
				@type("application/sparql-results+xml") InputStream in) {
			return "sparql-results+xml";
		}
	}

	public static abstract class RDFXMLFile implements HTTPFileObject {
		@method("GET")
		@query({})
		@type("application/rdf+xml")
		public InputStream getInputStream() throws IOException {
			return openInputStream();
		}
	}

	public void setUp() throws Exception {
		config.addBehaviour(RDFXMLFile.class, "urn:mimetype:application/rdf+xml");
		config.addBehaviour(Alternate.class, RDFS.RESOURCE);
		config.addBehaviour(PUTSupport.class);
		super.setUp();
	}

	public void testAlternate() throws Exception {
		WebResource web = client.path("/");
		web.accept("application/rdf+xml").get(Model.class);
		web.accept("application/sparql-results+xml").get(String.class);
	}

	public void testGetOperation() throws Exception {
		WebResource web = client.path("/").queryParam("my", "");
		web.accept("application/rdf+xml").get(Model.class);
		web.accept("application/sparql-results+xml").get(String.class);
	}

	public void testPutOperation() throws Exception {
		WebResource web = client.path("/").queryParam("my", "");
		web.type("application/rdf+xml").put(new LinkedHashModel());
		String str = web.accept("application/sparql-results+xml").get(String.class);
		web.type("application/sparql-results+xml").put(str);
	}

	public void testEntityTag() throws Exception {
		WebResource root = client.path("/");
		root.put("resource");
		WebResource web = root.queryParam("my", "");
		String rdf = web.accept("application/rdf+xml").get(ClientResponse.class).getEntityTag().toString();
		String ttl = web.accept("application/x-turtle").get(ClientResponse.class).getEntityTag().toString();
		assertFalse(rdf.equals(ttl));
		assertFalse(web.accept("application/rdf+xml").get(ClientResponse.class).getHeaders().getFirst("ETag").contains(","));
		assertFalse(web.accept("application/x-turtle").get(ClientResponse.class).getHeaders().getFirst("ETag").contains(","));
	}

	public void testPutEntityTag() throws Exception {
		WebResource web = client.path("/");
		ClientResponse resp = web.type("application/rdf+xml").put(ClientResponse.class, new LinkedHashModel());
		String put = resp.getEntityTag().toString();
		String head = web.accept("application/rdf+xml").head().getEntityTag().toString();
		assertEquals(put, head);
		assertFalse(web.accept("application/rdf+xml").head().getHeaders().getFirst("ETag").contains(","));
	}

	public void testIfNoneMatch() throws Exception {
		WebResource root = client.path("/");
		root.put("resource");
		WebResource web = root.queryParam("my", "");
		Model rdf = web.accept("application/rdf+xml").get(Model.class);
		Thread.sleep(1000);
		Model ttl = web.accept("application/rdf+xml,application/x-turtle;q=.2").get(Model.class);
		assertEquals(rdf, ttl);
	}

	public void testRequestBody() throws Exception {
		WebResource web = client.path("/").queryParam("my", "");
		assertEquals("rdf+xml", web.type("application/rdf+xml").post(String.class, "<RDF/>"));
		assertEquals("sparql-results+xml", web.type("application/sparql-results+xml").post(String.class, "<sparql/>"));
	}
}
