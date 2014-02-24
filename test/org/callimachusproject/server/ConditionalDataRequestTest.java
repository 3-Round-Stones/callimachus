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

import java.io.ByteArrayOutputStream;

import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.DescribeSupport;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.callimachusproject.server.behaviours.TextFile;
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
