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
package org.callimachusproject.server;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Method;
import org.openrdf.annotations.Path;
import org.openrdf.annotations.Type;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class ConditionalPropertyRequestTest extends MetadataServerTestCase {

	@Iri(RDFS.NAMESPACE + "Resource")
	public abstract static class Resource {
		@Method("GET")
		@Path("?property")
		@requires("urn:test:grant")
		@Type("text/plain")
		@Iri("urn:test:property")
		public abstract String getProperty();
		@Method({"PUT", "DELETE"})
		@Path("?property")
		@requires("urn:test:grant")
		@Iri("urn:test:property")
		public abstract void setProperty(@Type("text/plain") String property);
		@Method("HEAD")
		@Path("?other")
		@requires("urn:test:grant")
		public HttpResponse head() {
			HttpResponse head = new BasicHttpResponse(HttpVersion.HTTP_1_1, 200, "OK");
			head.setHeader("Cache-Control", "no-store");
			head.setHeader("Content-Type", "text/plain");
			return head;
		}
		@Method("GET")
		@Path("?other")
		@requires("urn:test:grant")
		@Type("text/plain")
		@Iri("urn:test:other")
		public abstract String getOtherProperty();
		@Method({"PUT", "DELETE"})
		@Path("?other")
		@requires("urn:test:grant")
		@Iri("urn:test:other")
		public abstract void setOtherProperty(@Type("text/plain") String property);
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
