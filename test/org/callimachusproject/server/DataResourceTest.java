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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Set;

import org.callimachusproject.annotations.requires;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.AliasSupport;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.callimachusproject.server.behaviours.TextFile;
import org.callimachusproject.server.concepts.Alias;
import org.callimachusproject.server.concepts.HTTPFileObject;
import org.openrdf.annotations.Method;
import org.openrdf.annotations.Path;
import org.openrdf.annotations.Type;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class DataResourceTest extends MetadataServerTestCase {

	public static abstract class WorldFile implements HTTPFileObject {
		@Method("GET")
		@Type("text/world")
		@requires("urn:test:grant")
		public InputStream getInputStream() throws IOException {
			return openInputStream();
		}

		@Method("POST")
		@Path("?set")
		@requires("urn:test:grant")
		@Type("application/octet-stream")
		public byte[] postInputStream(@Type("*/*") Set<InputStream> in) throws IOException {
			byte[] buf = new byte[1024];
			int read = in.iterator().next().read(buf);
			byte[] result = new byte[read];
			System.arraycopy(buf, 0, result, 0, read);
			return result;
		}
	}

	public void setUp() throws Exception {
		config.addBehaviour(TextFile.class, "urn:mimetype:text/plain");
		config.addBehaviour(WorldFile.class, "urn:mimetype:text/world");
		config.addBehaviour(PUTSupport.class);
		config.addConcept(Alias.class);
		config.addBehaviour(AliasSupport.class);
		super.setUp();
	}

	@Override
	protected void addContentEncoding(WebResource client) {
		// bug in gzip and Content-Length
	}

	public void testPUT() throws Exception {
		client.path("hello").put("world");
		assertEquals("world", client.path("hello").get(String.class));
	}

	public void testPUTRoot() throws Exception {
		client.put("world");
		assertEquals("world", client.get(String.class));
	}

	public void testDELETE() throws Exception {
		client.path("hello").put("world");
		client.path("hello").delete();
		try {
			client.path("hello").get(String.class);
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}

	public void testPUTIfUnmodifiedSince() throws Exception {
		WebResource hello = client.path("hello");
		hello.put("world");
		Date lastModified = hello.head().getLastModified();
		Thread.sleep(2000);
		hello.put("new world");
		try {
			hello.header("If-Unmodified-Since", lastModified).put("bad world");
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("new world", hello.get(String.class));
	}

	public void testDELETEIfUnmodifiedSince() throws Exception {
		WebResource hello = client.path("hello");
		hello.put("world");
		Date lastModified = hello.head().getLastModified();
		Thread.sleep(2000);
		hello.put("new world");
		try {
			hello.header("If-Unmodified-Since", lastModified).delete();
			fail();
		} catch (UniformInterfaceException e) {
			assertEquals(412, e.getResponse().getStatus());
		}
		assertEquals("new world", hello.get(String.class));
	}

	public void testPUTContentType() throws Exception {
		WebResource hello = client.path("hello.txt");
		hello.type("text/world").put("world");
		assertEquals("text/world", hello.head().getMetadata().getFirst("Content-Type"));
	}

	public void testNoOptions() throws Exception {
		ClientResponse options = client.path("hello").options(ClientResponse.class);
		String allows = options.getMetadata().getFirst("Allow");
		assertEquals("OPTIONS, PUT", allows);
	}

	public void testOPTIONS() throws Exception {
		client.path("hello").put("world");
		ClientResponse options = client.path("hello").options(ClientResponse.class);
		String allows = options.getMetadata().getFirst("Allow");
		assertEquals("OPTIONS, DELETE, GET, HEAD, PUT", allows);
	}

	public void testSetOfInputStream() throws Exception {
		WebResource hello = client.path("hello.txt");
		hello.type("text/world").put("world");
		byte[] bytes = "world".getBytes();
		InputStream out = new ByteArrayInputStream(bytes);
		InputStream in = hello.queryParam("set", "").post(InputStream.class, out);
		byte[] buf = new byte[bytes.length];
		in.read(buf);
		assertEquals(new String(bytes), new String(buf));
	}
}
