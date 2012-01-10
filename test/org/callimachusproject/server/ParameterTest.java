package org.callimachusproject.server;

import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.WebResource;

public class ParameterTest extends MetadataServerTestCase {

	public static abstract class Behaviour {
		@method("GET")
		@query("query")
		public String query(@query("q1") String q1) {
			return "hello " + q1;
		}

		@method("GET")
		@query("star")
		public String star(@query("*") String qs) {
			return "hello " + qs;
		}

		@method("GET")
		@query("header")
		public String header(@header("h1") String h1) {
			return "hello " + h1;
		}

		@method("GET")
		@query("either")
		public String either(@query("q1") @header("h1") String value) {
			return "hello " + value;
		}
	}

	public void setUp() throws Exception {
		config.addBehaviour(Behaviour.class, RDFS.RESOURCE);
		super.setUp();
	}

	@Override
	protected void addContentEncoding(WebResource client) {
		// don't add gzip support
	}

	public void testJustQuery() throws Exception {
		WebResource q1 = client.path("/").queryParam("query", "").queryParam("q1", "q1");
		assertEquals("hello q1", q1.get(String.class));
	}

	public void testStar() throws Exception {
		WebResource q1 = client.path("/").queryParam("star", "");
		assertEquals("hello star", q1.get(String.class));
	}

	public void testJustHeader() throws Exception {
		WebResource h1 = client.path("/").queryParam("header", "");
		assertEquals("hello h1", h1.header("h1", "h1").get(String.class));
	}

	public void testQuery() throws Exception {
		WebResource q1 = client.path("/").queryParam("either", "").queryParam("q1", "q1");
		assertEquals("hello q1", q1.get(String.class));
	}

	public void testQueryPriority() throws Exception {
		WebResource q1 = client.path("/").queryParam("either", "").queryParam("q1", "q1");
		assertEquals("hello q1", q1.header("h1", "h1").get(String.class));
	}

	public void testHeader() throws Exception {
		WebResource h1 = client.path("/").queryParam("either", "");
		assertEquals("hello h1", h1.header("h1", "h1").get(String.class));
	}

}
