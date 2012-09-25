package org.callimachusproject.server;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.commons.codec.digest.DigestUtils;
import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.annotations.Iri;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.ObjectConnection;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPDigestAuthFilter;

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

	@Retention(RetentionPolicy.RUNTIME)
	@Target( { ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER })
	public @interface reader {
		@Iri("urn:test:reader")
		String[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target( { ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER })
	public @interface writer {
		@Iri("urn:test:writer")
		String[] value();
	}

	@reader("urn:test:my-group")
	@Iri("urn:test:MyProtectedResource")
	public static class MyProtectedResource {
		public static String body = "body";

		@method("GET")
		@requires("urn:test:reader")
		@type("text/plain")
		@header("Cache-Control:max-age=5")
		public String getResponse() {
			return body;
		}
	}

	@reader("urn:test:my-group")
	@writer("urn:test:my-group")
	@Iri("urn:test:MyResource")
	public static class MyResource {

		@method("GET")
		@requires("urn:test:reader")
		@type("text/plain")
		public String get(@header("Authorization") String auth) {
			return auth;
		}

		@method("POST")
		@requires("urn:test:writer")
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
			ValueFactory vf = con.getValueFactory();
			con.add(vf.createURI("urn:test:my-group"), vf.createURI("http://callimachusproject.org/rdf/2009/framework#membersFrom"), vf.createLiteral("."));
			con.add(vf.createURI("urn:test:my-group"), vf.createURI("http://callimachusproject.org/rdf/2009/framework#member"), vf.createURI("urn:test:user:bob"));
			con.add(vf.createURI("urn:test:my-group"), vf.createURI("http://callimachusproject.org/rdf/2009/framework#member"), vf.createURI("urn:test:user:jim"));
			con.add(vf.createURI(ORIGIN), RDF.TYPE, vf.createURI("http://callimachusproject.org/rdf/2009/framework#Origin"));
			con.add(vf.createURI(ORIGIN), vf.createURI("http://callimachusproject.org/rdf/2009/framework#authentication"), vf.createURI("urn:test:auth"));
			con.add(vf.createURI("urn:test:auth"), vf.createURI("http://callimachusproject.org/rdf/2009/framework#authName"), vf.createLiteral("test"));
			con.add(vf.createURI("urn:test:auth"), vf.createURI("http://callimachusproject.org/rdf/2009/framework#authNamespace"), vf.createURI("urn:test:user:"));
			con.add(vf.createURI("urn:test:user:"), vf.createURI("http://callimachusproject.org/rdf/2009/framework#hasComponent"), vf.createURI("urn:test:user:bob"));
			con.add(vf.createURI("urn:test:user:bob"), vf.createURI("http://callimachusproject.org/rdf/2009/framework#name"), vf.createLiteral("bob"));
			con.add(vf.createURI("urn:test:user:bob"), vf.createURI("http://callimachusproject.org/rdf/2009/framework#passwordDigest"), vf.createURI("urn:test:passwordDigest1"));
			Writer writer = new OutputStreamWriter(con.getBlobObject("urn:test:passwordDigest1").openOutputStream(), "UTF-8");
			try {
				writer.write(DigestUtils.md5Hex("bob:test:pass"));
			} finally {
				writer.close();
			}
			con.add(vf.createURI("urn:test:user:"), vf.createURI("http://callimachusproject.org/rdf/2009/framework#hasComponent"), vf.createURI("urn:test:user:jim"));
			con.add(vf.createURI("urn:test:user:jim"), vf.createURI("http://callimachusproject.org/rdf/2009/framework#name"), vf.createLiteral("jim"));
			con.add(vf.createURI("urn:test:user:jim"), vf.createURI("http://callimachusproject.org/rdf/2009/framework#passwordDigest"), vf.createURI("urn:test:passwordDigest2"));
			writer = new OutputStreamWriter(con.getBlobObject("urn:test:passwordDigest2").openOutputStream(), "UTF-8");
			try {
				writer.write(DigestUtils.md5Hex("jim:test:pass"));
			} finally {
				writer.close();
			}
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
		MyProtectedResource.body = "first";
		resp = web("/protected", "bob").get(ClientResponse.class);
		assertNotNull(resp.getHeaders().get("ETag"));
		assertNotNull(resp.getHeaders().get("Authentication-Info").get(0));
		assertEquals("first", resp.getEntity(String.class));
	}

	public void testTwice() throws Exception {
		ClientResponse resp;
		MyProtectedResource.body = "first";
		resp = web("/protected", "bob").get(ClientResponse.class);
		MyProtectedResource.body = "second";
		resp = web("/protected", "jim").get(ClientResponse.class);
		assertNotNull(resp.getHeaders().get("ETag"));
		// body should be cached
		assertEquals("first", resp.getEntity(String.class));
		assertNotNull(resp.getHeaders().get("Authentication-Info").get(0));
	}

	public void testBadAuth() throws Exception {
		ClientResponse resp;
		MyProtectedResource.body = "first";
		resp = web("/protected", "bob").get(ClientResponse.class);
		resp = web("/protected", "nobody").get(ClientResponse.class);
		assertFalse("first".equals(resp.getEntity(String.class)));
		assertNull(resp.getHeaders().get("Authentication-Info"));
	}

	public void testBadAndGoddAuth() throws Exception {
		ClientResponse resp;
		MyProtectedResource.body = "first";
		resp = web("/protected", "bob").get(ClientResponse.class);
		resp = web("/protected", "nobody").get(ClientResponse.class);
		MyProtectedResource.body = "second";
		resp = web("/protected", "jim").get(ClientResponse.class);
		assertNotNull(resp.getHeaders().get("ETag"));
		// body should still be cached
		assertEquals("first", resp.getEntity(String.class));
		assertNotNull(resp.getHeaders().get("Authentication-Info").get(0));
	}

	public void testPostVaryAuth() throws Exception {
		ClientResponse resp;
		resp = web("/resource", "bob").post(ClientResponse.class, "input");
		assertTrue(resp.getEntity(String.class).contains("username=\"bob\""));
		assertEquals("private", resp.getHeaders().getFirst("Cache-Control"));
		assertNotNull(resp.getHeaders().get("ETag"));
		assertFalse(resp.getHeaders().get("Vary").toString().contains("Authorization"));
	}

	public void testGetVaryAuth() throws Exception {
		ClientResponse resp;
		resp = web("/resource", "bob").get(ClientResponse.class);
		assertTrue(resp.getEntity(String.class).contains("username=\"bob\""));
		assertTrue(resp.getHeaders().get("Cache-Control").get(0).contains("private"));
		assertNotNull(resp.getHeaders().get("ETag"));
		assertFalse(resp.getHeaders().get("Vary").toString().contains("Authorization"));
		resp = web("/resource", "jim").get(ClientResponse.class);
		assertTrue(resp.getEntity(String.class).contains("username=\"jim\""));
		assertTrue(resp.getHeaders().get("Cache-Control").get(0).contains("private"));
		assertNotNull(resp.getHeaders().get("ETag"));
		assertFalse(resp.getHeaders().get("Vary").toString().contains("Authorization"));
	}

	protected void addContentEncoding(WebResource client) {
		// its broken
	}

	private WebResource web(String path, String user) {
		WebResource web = web(path);
		web.addFilter(new HTTPDigestAuthFilter(user, "pass"));
		return web;
	}

	private WebResource web(String path) {
		return client.path(path);
	}

}
