package org.callimachusproject.server;

import java.io.IOException;
import java.nio.CharBuffer;

import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.behaviours.PUTSupport;
import org.openrdf.model.vocabulary.RDFS;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class CharsetTest extends MetadataServerTestCase {

	public static class Resource {
		@query("string")
		@type("text/plain")
		public String hello() {
			return "Hello World!";
		}

		@query("stream")
		@type("text/plain; charset=UTF-8")
		public Readable stream() {
			return new Readable() {
				private boolean written;
				public int read(CharBuffer cb) throws IOException {
					if (written)
						return -1;
					written = true;
					cb.append("Hello World!");
					return "Hello World!".length();
				}
			};
		}
	}

	public void setUp() throws Exception {
		config.addConcept(Resource.class, RDFS.RESOURCE);
		config.addBehaviour(PUTSupport.class);
		super.setUp();
	}

	public void testCharsetStream() throws Exception {
		client.path("/hello").put("resource");
		WebResource web = client.path("/hello").queryParam("stream", "");
		ClientResponse get = web.header("Accept-Charset", "ISO-8859-1").get(
				ClientResponse.class);
		assertEquals("text/plain; charset=UTF-8", get.getHeaders().getFirst(
				"Content-Type"));
	}
}
