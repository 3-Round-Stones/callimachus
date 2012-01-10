package org.openrdf.http.object;

import java.io.IOException;
import java.io.InputStream;

import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.http.object.behaviours.PUTSupport;
import org.openrdf.http.object.behaviours.TextFile;
import org.openrdf.http.object.concepts.HTTPFileObject;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class RangeTest extends MetadataServerTestCase {

	public static abstract class StringFile implements HTTPFileObject {
		@method("GET")
		@type("application/string")
		public InputStream getInputStream() throws IOException {
			return openInputStream();
		}
	}

	public void setUp() throws Exception {
		config.addBehaviour(TextFile.class, "urn:mimetype:text/plain");
		config.addBehaviour(StringFile.class, "urn:mimetype:application/string");
		config.addBehaviour(PUTSupport.class);
		super.setUp();
	}

	public void testUptoRange() throws Exception {
		WebResource web = client.path("/hello");
		web.type("application/string").put("Hello World!");
		assertEquals("Hello World!", web.header("Range", "bytes=0-100").get(String.class));
	}

	public void testStartRange() throws Exception {
		WebResource web = client.path("/hello");
		web.type("application/string").put("Hello World!");
		assertEquals("Hello", web.header("Range", "bytes=0-4").get(String.class));
	}

	public void testEndRange() throws Exception {
		WebResource web = client.path("/hello");
		web.type("application/string").put("Hello World!");
		assertEquals("World!", web.header("Range", "bytes=-6").get(String.class));
	}

	public void testLastRange() throws Exception {
		WebResource web = client.path("/hello");
		web.type("application/string").put("Hello World!");
		assertEquals("Hello World!", web.header("Range", "bytes=-100").get(String.class));
	}

	public void testIfRangeTag() throws Exception {
		WebResource web = client.path("/hello");
		ClientResponse resp = web.type("application/string").put(ClientResponse.class, "Hello World!");
		String tag = resp.getEntityTag().toString();
		assertEquals("Hello", web.header("If-Range", tag).header("Range", "bytes=0-4").get(String.class));
	}

	public void testIfRangetagFail() throws Exception {
		WebResource web = client.path("/hello");
		ClientResponse resp = web.type("application/string").put(ClientResponse.class, "Hello World!");
		String tag = resp.getEntityTag().toString();
		web.type("application/string").put("Hey World!");
		assertEquals("Hey World!", web.header("If-Range", tag).header("Range", "bytes=0-4").get(String.class));
	}

	public void testIfRangeDate() throws Exception {
		WebResource web = client.path("/hello");
		ClientResponse resp = web.type("application/string").put(ClientResponse.class, "Hello World!");
		String m = resp.getHeaders().getFirst("Last-Modified");
		assertNotNull(m);
		assertEquals("Hello", web.header("If-Range", m).header("Range", "bytes=0-4").get(String.class));
	}

	public void testIfRangeDateFail() throws Exception {
		WebResource web = client.path("/hello");
		ClientResponse resp = web.type("application/string").put(ClientResponse.class, "Hello World!");
		String m = resp.getHeaders().getFirst("Last-Modified");
		assertNotNull(m);
		Thread.sleep(1000);
		web.type("application/string").put("Hey World!");
		assertEquals("Hey World!", web.header("If-Range", m).header("Range", "bytes=0-4").get(String.class));
	}
}
