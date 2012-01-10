package org.openrdf.http.object;

import java.io.ByteArrayOutputStream;

import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.http.object.behaviours.DescribeSupport;
import org.openrdf.http.object.behaviours.PUTSupport;
import org.openrdf.http.object.behaviours.TextFile;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class ConditionalDataRequestTest extends MetadataServerTestCase {

	public void setUp() throws Exception {
		config.addBehaviour(TextFile.class, "urn:mimetype:text/plain");
		config.addBehaviour(PUTSupport.class);
		super.setUp();
	}

	protected void addContentEncoding(WebResource client) {
		// seems to be a bug in this filter wrt Content-Length
		// client.addFilter(new GZIPContentEncodingFilter());
	}

	public void testRefresh() throws Exception {
		WebResource web = client.path("/hello");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		web.put("server");
		assertEquals("server", web.header("If-None-Match", tag).get(String.class));
	}

	public void testRefreshFail() throws Exception {
		WebResource web = client.path("/hello");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		try {
			web.header("If-None-Match", tag).get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(304, e.getResponse().getStatus());
		}
		assertEquals("world", web.get(String.class));
	}

	public void testValidate() throws Exception {
		WebResource web = client.path("/hello");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		assertEquals("world", web.header("If-Match", tag).get(String.class));
	}

	public void testValidateFail() throws Exception {
		WebResource web = client.path("/hello");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		web.put("server");
		try {
			web.header("If-Match", tag).get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("server", web.get(String.class));
	}

	public void testCreate() throws Exception {
		WebResource web = client.path("/hello");
		web.header("If-None-Match", "*").put("world");
		assertEquals("world", web.get(String.class));
	}

	public void testCreateFail() throws Exception {
		WebResource web = client.path("/hello");
		web.put("world");
		try {
			web.header("If-None-Match", "*").put("server");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("world", web.get(String.class));
	}

	public void testUpdate() throws Exception {
		WebResource web = client.path("/hello");
		web.put("world");
		web.header("If-Match", "*").put("server");
		assertEquals("server", web.get(String.class));
	}

	public void testUpdateFail() throws Exception {
		WebResource web = client.path("/hello");
		try {
			web.header("If-Match", "*").put("world");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		try {
			web.get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testUpdateMatch() throws Exception {
		WebResource web = client.path("/hello");
		ClientResponse res = web.put(ClientResponse.class, "world");
		web.header("If-Match", res.getEntityTag().toString()).put("server");
		assertEquals("server", web.get(String.class));
	}

	public void testUpdateMatchFail() throws Exception {
		WebResource web = client.path("/hello");
		web.put("world");
		try {
			web.header("If-Match", "\"balloons\"").put("server");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("world", web.get(String.class));
	}

	public void testBigUpdateMatchFail() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (int i=0;i<1000;i++) {
			out.write("server".getBytes());
		}
		WebResource web = client.path("/hello");
		web.put("world");
		try {
			web.header("If-Match", "\"balloons\"").put(out.toByteArray());
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("world", web.get(String.class));
	}

	public void testDelete() throws Exception {
		WebResource web = client.path("/hello");
		web.put("world");
		web.header("If-Match", "*").delete();
		try {
			web.get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testDeleteFail() throws Exception {
		WebResource web = client.path("/hello");
		try {
			web.header("If-Match", "*").delete();
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		try {
			web.get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testDeleteMatch() throws Exception {
		WebResource web = client.path("/hello");
		ClientResponse res = web.put(ClientResponse.class, "world");
		web.header("If-Match", res.getEntityTag().toString()).delete();
		try {
			web.get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testDeleteMatchFail() throws Exception {
		WebResource web = client.path("/hello");
		web.put("world");
		try {
			web.header("If-Match", "\"balloons\"").delete();
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("world", web.get(String.class));
	}
}
