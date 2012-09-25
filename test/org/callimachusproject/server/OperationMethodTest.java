package org.callimachusproject.server;

import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class OperationMethodTest extends MetadataServerTestCase {

	public static class Resource1 {
		public static String operation;

		@query("op1")
		@requires("urn:test:grant")
		public String getOperation1() {
			return operation;
		}

		@query("op1")
		@method("PUT")
		@requires("urn:test:grant")
		public void setOperation1(@type("*/*") String value) {
			operation = String.valueOf(value);
		}

		@query("op1")
		@method("DELETE")
		@requires("urn:test:grant")
		public void delOperation1() {
			operation = null;
		}

		@query("op1")
		@requires("urn:test:grant")
		public String setAndGetOperation1(@type("*/*") String value) {
			String pre = operation;
			operation = value;
			return pre;
		}

		@method("PUT")
		@requires("urn:test:grant")
		public void putNothing(@type("*/*") byte[] data) {
			assertEquals(0, data.length);
		}
	}

	public static class Resource2 {
		public static String operation;

		@query("op2")
		@requires("urn:test:grant")
		public String getOperation2() {
			return operation;
		}

		@query("op2")
		@method("PUT")
		@requires("urn:test:grant")
		public void setOperation2(@type("text/plain") String value) {
			operation = String.valueOf(value);
		}

		@query("op2")
		@method("DELETE")
		@requires("urn:test:grant")
		public void delOperation2() {
			operation = null;
		}

		@query("op2")
		@requires("urn:test:grant")
		public String setAndGetOperation2(@type("text/plain") String value) {
			String pre = operation;
			operation = value;
			return pre;
		}
	}

	public void setUp() throws Exception {
		config.addBehaviour(Resource1.class, RDFS.RESOURCE);
		config.addBehaviour(Resource2.class, RDFS.RESOURCE);
		super.setUp();
		Resource1.operation = "op1";
		Resource2.operation = "op2";
	}

	@Override
	protected void addContentEncoding(WebResource client) {
		// don't add gzip support
	}

	public void testGetOperation() throws Exception {
		WebResource op1 = client.path("/").queryParam("op1", "");
		WebResource op2 = client.path("/").queryParam("op2", "");
		assertEquals("op1", op1.get(String.class));
		assertEquals("op2", op2.get(String.class));
	}

	public void testPutOperation() throws Exception {
		WebResource op1 = client.path("/").queryParam("op1", "");
		WebResource op2 = client.path("/").queryParam("op2", "");
		op1.put("put1");
		op2.put("put2");
		assertEquals("put1", Resource1.operation);
		assertEquals("put2", Resource2.operation);
	}

	public void testDeleteOperation() throws Exception {
		WebResource op1 = client.path("/").queryParam("op1", "");
		WebResource op2 = client.path("/").queryParam("op2", "");
		op1.delete();
		op2.delete();
		assertNull(Resource1.operation);
		assertNull(Resource2.operation);
	}

	public void testPostOperation() throws Exception {
		WebResource op1 = client.path("/").queryParam("op1", "");
		WebResource op2 = client.path("/").queryParam("op2", "");
		assertEquals("op1", op1.post(String.class, "post1"));
		assertEquals("op2", op2.post(String.class, "post2"));
		assertEquals("post1", op1.get(String.class));
		assertEquals("post2", op2.get(String.class));
	}

	public void testPutNothing() throws Exception {
		WebResource op1 = client.path("/");
		try {
			op1.put(String.class, "");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(204, e.getResponse().getStatus());
		}
	}
}
