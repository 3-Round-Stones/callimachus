package org.callimachusproject.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Set;

import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.AliasSupport;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.callimachusproject.server.behaviours.TextFile;
import org.callimachusproject.server.concepts.Alias;
import org.callimachusproject.server.concepts.HTTPFileObject;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;

public class DataResourceTest extends MetadataServerTestCase {

	public static abstract class WorldFile implements HTTPFileObject {
		@method("GET")
		@type("text/world")
		public InputStream getInputStream() throws IOException {
			return openInputStream();
		}

		@query("set")
		public byte[] postInputStream(@type("*/*") Set<InputStream> in) throws IOException {
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
		assertEquals("OPTIONS, TRACE, PUT", allows);
	}

	public void testOPTIONS() throws Exception {
		client.path("hello").put("world");
		ClientResponse options = client.path("hello").options(ClientResponse.class);
		String allows = options.getMetadata().getFirst("Allow");
		assertEquals("OPTIONS, TRACE, GET, HEAD, PUT, DELETE", allows);
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
