package org.callimachusproject.server;

import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.annotations.Iri;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class ConditionalPropertyRequestTest extends MetadataServerTestCase {

	@Iri(RDFS.NAMESPACE + "Resource")
	public interface Resource {
		@query("property")
		@requires("urn:test:grant")
		@type("text/plain")
		@Iri("urn:test:property")
		String getProperty();
		@query("property")
		@requires("urn:test:grant")
		@Iri("urn:test:property")
		void setProperty(@type("text/plain") String property);
		@query("other")
		@requires("urn:test:grant")
		@type("text/plain")
		@header("Cache-Control:no-store")
		@Iri("urn:test:other")
		String getOtherProperty();
		@query("other")
		@requires("urn:test:grant")
		@Iri("urn:test:other")
		void setOtherProperty(@type("text/plain") String property);
	}

	public void setUp() throws Exception {
		config.addConcept(Resource.class);
		super.setUp();
	}

	public void testRefresh() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		web.put("server");
		assertEquals("server", web.header("If-None-Match", tag).get(String.class));
	}

	public void testRefreshFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
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
		WebResource web = client.path("/hello").queryParam("property", "");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		assertEquals("world", web.header("If-Match", tag).get(String.class));
	}

	public void testValidateFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
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

	public void testNoStoreRefresh() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		web.put("server");
		assertEquals("server", web.header("If-None-Match", tag).get(String.class));
	}

	public void testNoStoreRefreshFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("other", "");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		try {
			web.header("If-None-Match", tag).get(String.class);
			// no-store/private does not support cache validation/refresh
		} catch (UniformInterfaceException e) {
			assertEquals(304, e.getResponse().getStatus());
		}
		assertEquals("world", web.get(String.class));
	}

	public void testNoStoreValidate() throws Exception {
		WebResource web = client.path("/hello").queryParam("other", "");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		assertEquals("world", web.header("If-Match", tag).get(String.class));
	}

	public void testNoStoreValidateFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("other", "");
		String tag = web.put(ClientResponse.class, "world").getEntityTag().toString();
		web.put("server");
		try {
			web.header("If-Match", tag).get(String.class);
			// no-store/private does not support cache validation/refresh
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("server", web.get(String.class));
	}

	public void testCreateFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
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
		WebResource web = client.path("/hello").queryParam("property", "");
		web.put("world");
		web.header("If-Match", "*").put("server");
		assertEquals("server", web.get(String.class));
	}

	public void testUpdateMatch() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		ClientResponse res = web.put(ClientResponse.class, "world");
		web.header("If-Match", res.getEntityTag().toString()).put("server");
		assertEquals("server", web.get(String.class));
	}

	public void testUpdateMatchFail() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		web.put("world");
		try {
			web.header("If-Match", "\"balloons\"").put("server");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("world", web.get(String.class));
	}

	public void testDelete() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
		web.put("world");
		web.header("If-Match", "*").delete();
		try {
			web.get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testDeleteMatch() throws Exception {
		WebResource web = client.path("/hello").queryParam("property", "");
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
		WebResource web = client.path("/hello").queryParam("property", "");
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
