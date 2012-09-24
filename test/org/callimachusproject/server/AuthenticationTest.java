package org.callimachusproject.server;

import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.ObjectConnection;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class AuthenticationTest extends MetadataServerTestCase {
	private static final int PORT = 59322;
	private static final String ORIGIN = "http://localhost:" + PORT;

	@Override
	protected int getPort() {
		return PORT;
	}

	@Override
	protected String getOrigin() {
		return ORIGIN;
	}

	@Iri("urn:test:MyProtectedResource")
	public static class MyProtectedResource {
		public static String body = "body";

		@method("GET")
		@requires("http://callimachusproject.org/rdf/2009/framework#reader")
		@type("text/plain")
		@header("Cache-Control:max-age=5")
		public String getResponse() {
			return body;
		}
	}

	@Iri("urn:test:MyResource")
	public static class MyResource {

		@method("GET")
		@type("text/plain")
		public String get(@header("Authorization") String auth) {
			return auth;
		}

		@method("POST")
		@type("text/plain")
		public String post(@type("text/plain") String input,
				@header("Authorization") String auth) {
			return auth;
		}
	}

	public void setUp() throws Exception {
		config.addConcept(MyResource.class);
		config.addConcept(MyProtectedResource.class);
		super.setUp();
		ObjectConnection con = repository.getConnection();
		try {
			String uri = client.path("/protected").getURI().toASCIIString();
			con.addDesignation(con.getObject(uri), MyProtectedResource.class);
			uri = client.path("/resource").getURI().toASCIIString();
			con.addDesignation(con.getObject(uri), MyResource.class);
		} finally {
			con.close();
		}
	}

	public void testOnce() throws Exception {
		ClientResponse resp;
		WebResource web = client.path("/protected");
		MyProtectedResource.body = "first";
		resp = web.header("Authorization", "one").get(ClientResponse.class);
		assertNotNull(resp.getHeaders().get("ETag"));
		assertEquals("one", resp.getHeaders().get("Authentication-Info").get(0));
		assertEquals("first", resp.getEntity(String.class));
	}

	public void testTwice() throws Exception {
		ClientResponse resp;
		WebResource web = client.path("/protected");
		MyProtectedResource.body = "first";
		resp = web.header("Authorization", "one").get(ClientResponse.class);
		MyProtectedResource.body = "second";
		resp = web.header("Authorization", "two").get(ClientResponse.class);
		assertNotNull(resp.getHeaders().get("ETag"));
		// body should be cached
		assertEquals("first", resp.getEntity(String.class));
		assertEquals("two", resp.getHeaders().get("Authentication-Info").get(0));
	}

	public void testBadAuth() throws Exception {
		ClientResponse resp;
		WebResource web = client.path("/protected");
		MyProtectedResource.body = "first";
		resp = web.header("Authorization", "one").get(ClientResponse.class);
		resp = web.header("Authorization", "bad").get(ClientResponse.class);
		assertFalse("first".equals(resp.getEntity(String.class)));
		assertNull(resp.getHeaders().get("Authentication-Info"));
	}

	public void testBadAndGoddAuth() throws Exception {
		ClientResponse resp;
		WebResource web = client.path("/protected");
		MyProtectedResource.body = "first";
		resp = web.header("Authorization", "one").get(ClientResponse.class);
		resp = web.header("Authorization", "bad").get(ClientResponse.class);
		MyProtectedResource.body = "second";
		resp = web.header("Authorization", "two").get(ClientResponse.class);
		assertNotNull(resp.getHeaders().get("ETag"));
		// body should still be cached
		assertEquals("first", resp.getEntity(String.class));
		assertEquals("two", resp.getHeaders().get("Authentication-Info").get(0));
	}

	public void testPostVaryAuth() throws Exception {
		ClientResponse resp;
		WebResource web = client.path("/resource");
		resp = web.header("Authorization", "test").post(ClientResponse.class, "input");
		assertEquals("test", resp.getEntity(String.class));
		assertEquals("private", resp.getHeaders().getFirst("Cache-Control"));
		assertNotNull(resp.getHeaders().get("ETag"));
		assertFalse(resp.getHeaders().get("Vary").toString().contains("Authorization"));
	}

	public void testGetVaryAuth() throws Exception {
		ClientResponse resp;
		WebResource web = client.path("/resource");
		resp = web.header("Authorization", "test").get(ClientResponse.class);
		assertEquals("test", resp.getEntity(String.class));
		assertTrue(resp.getHeaders().get("Cache-Control").get(0).contains("private"));
		assertNotNull(resp.getHeaders().get("ETag"));
		assertFalse(resp.getHeaders().get("Vary").toString().contains("Authorization"));
	}

}
