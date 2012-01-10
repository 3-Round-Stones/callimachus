package org.openrdf.http.object;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Matching;
import org.openrdf.http.object.annotations.header;
import org.openrdf.http.object.annotations.method;
import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.http.object.behaviours.PUTSupport;
import org.openrdf.http.object.concepts.HTTPFileObject;
import org.openrdf.http.object.exceptions.MethodNotAllowed;
import org.openrdf.http.object.traits.ProxyObject;
import org.openrdf.model.Resource;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

public class RemoteWebObjectTest extends MetadataServerTestCase {

	private ObjectConnection con;

	@Matching("file:///*")
	public static abstract class MyFile implements ProxyObject {
		private static InetSocketAddress addr;

		public InetSocketAddress getProxyObjectInetAddress() {
			String auth = getLocalAuthority();
			if (auth != null)
				return null;
			return addr;
		}
	}

	@Iri("urn:test:WebInterface")
	public interface WebInterface {
		String getWorld();

		void setWorld(String world);

		@method("GET")
		String hello();

		@query("post")
		String postPlain(@type("text/plain") String plain);

		@query("post")
		String postXML(@type("text/xml") String xml);

		@query("head")
		String head(@header("Content-Type") String type, @type("*/*") String in,
				@header("X-Forward") String forward);

		@query("set")
		Set<URI> set(@query("uri") Set<String> uris);

		@query("array")
		URI[] array(@query("uri") String[] uris);

		@query("star")
		String star(@query("*") String star, @query("one") String one);

		@query("post-query")
		String postQuery(@query("q") String q, @type("*/*") String body);

		@query("mapArray")
		Map<String, String[]> mapArray(@query("*") Map<String, String[]> map);

		@query("map")
		Map<String, String> map(@query("*") Map<String, String> map);

		@query("uris")
		Map<String, URI> uris(@query("*") Map<String, URI> map);

		@query("binary")
		byte[] binary(@type("*/*") byte[] binary);
	}

	public static abstract class WebInterfaceSupport implements WebInterface {
		private String world = "World";

		public String getWorld() {
			return world;
		}

		public void setWorld(String world) {
			this.world = world;
		}

		public String hello() {
			return "Hello " + world + "!";
		}

		public String postPlain(String plain) {
			return "plain";
		}

		public String postXML(String xml) {
			return "xml";
		}

		public String head(String type, String in, String forward) {
			if (forward == null)
				return type;
			return null;
		}

		public Set<URI> set(Set<String> uris) {
			Set<URI> result = new HashSet<URI>(uris.size());
			for (String uri : uris) {
				result.add(URI.create(uri));
			}
			return result;
		}

		public URI[] array(String[] uris) {
			URI[] result = new URI[uris.length];
			for (int i = 0; i < uris.length; i++) {
				result[i] = URI.create(uris[i]);
			}
			return result;
		}

		public String star(String star, String one) {
			assert "one".equals(one);
			return star;
		}

		public String postQuery(String q, String body) {
			assert q != null;
			assert body != null;
			return hello();
		}

		public Map<String, String[]> mapArray(Map<String, String[]> map) {
			return map;
		}

		public Map<String, String> map(Map<String, String> map) {
			return map;
		}

		public Map<String, URI> uris(Map<String, URI> map) {
			return map;
		}

		public byte[] binary(byte[] binary) {
			return binary;
		}
	}

	@Iri("urn:test:Chocolate")
	public static abstract class Chocolate implements HTTPFileObject, RDFObject {
		@method("DELETE")
		public void consume() throws RepositoryException {
			getObjectConnection().removeDesignation(this, Chocolate.class);
			delete();
		}

		@query("mix")
		public HotChocolate mix(@type("application/rdf+xml") Milk milk) throws RepositoryException {
			ObjectConnection con = getObjectConnection();
			HotChocolate hot = con.addDesignation(this, HotChocolate.class);
			hot.add(milk);
			return hot;
		}
	}

	@Iri("urn:test:Milk")
	public static class Milk {
		@query("pour")
		public void pourInto(@type("application/rdf+xml") HotChocolate drink) {
			drink.add(this);
		}
	}

	@Iri("urn:test:HotChocolate")
	public static abstract class HotChocolate extends Chocolate {
		public static int count;
		@Iri("urn:test:amountOfMilk")
		private int milk;

		@query("milk")
		public int getAmountOfMilk() {
			return milk;
		}

		public void add(Milk milk) {
			count++;
			this.milk++;
		}

		@method("DELETE")
		public void consume() throws RepositoryException {
			getObjectConnection().removeDesignation(this, HotChocolate.class);
			super.consume();
		}

	}

	public void setUp() throws Exception {
		config.addConcept(WebInterface.class);
		config.addBehaviour(WebInterfaceSupport.class);
		config.addConcept(Chocolate.class);
		config.addConcept(Milk.class);
		config.addConcept(HotChocolate.class);
		config.addConcept(MyFile.class);
		config.addBehaviour(PUTSupport.class);
		super.setUp();
		con = repository.getConnection();
	}

	public void tearDown() throws Exception {
		con.close();
		super.tearDown();
	}

	public void testLocal() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		File file = File.createTempFile("obj", "tmp");
		file.delete();
		String auth = URI.create(uri).getAuthority();
		((ProxyObject) obj).setLocalAuthority(auth);
		assertEquals("Hello World!", obj.hello());
		obj.setWorld("Toronto");
		assertEquals("Hello Toronto!", obj.hello());
	}

	public void testBNode() throws Exception {
		Resource id = con.getValueFactory().createBNode();
		WebInterface obj = con.addDesignation(con.getObject(id),
				WebInterface.class);
		assertEquals("Hello World!", obj.hello());
		obj.setWorld("Toronto");
		assertEquals("Hello Toronto!", obj.hello());
	}

	public void testURN() throws Exception {
		String uri = "urn:test:object";
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		assertEquals("Hello World!", obj.hello());
		obj.setWorld("Toronto");
		assertEquals("Hello Toronto!", obj.hello());
	}

	public void testRemote() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		assertEquals("Hello World!", obj.hello());
		obj.setWorld("Toronto"); // local in-memory property
		assertEquals("Hello World!", obj.hello());
	}

	public void testBodyType() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		assertEquals("plain", obj.postPlain("plain text"));
		assertEquals("xml", obj.postXML("xml text"));
	}

	public void testHeaders() throws Exception {
		String uri = client.path("/object").toString();
		con.addDesignation(con.getObject(uri), WebInterface.class);
		WebResource req = client.path("/object").queryParam("head", "");
		assertEquals("text/plain", req.type("text/plain").post(String.class,
				"txt"));
		MultivaluedMap<String, String> md;
		md = req.type("text/plain").post(ClientResponse.class, "txt")
				.getMetadata();
		assertTrue(md.get("Vary").toString().contains("X-Forward"));
		md = req.options(ClientResponse.class).getMetadata();
		assertTrue(md.get("Access-Control-Allow-Headers").toString().contains(
				"X-Forward"));
	}

	public void testRemoteHeaders() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		assertEquals("text/txt", obj.head("text/txt", "txt", null));
	}

	public void testSet() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		Set<String> input = new HashSet<String>();
		input.add("urn:urn1");
		input.add("urn:urn2");
		Set<URI> output = new HashSet<URI>();
		output.add(URI.create("urn:urn1"));
		output.add(URI.create("urn:urn2"));
		assertEquals(output, obj.set(input));
	}

	public void testArray() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		String[] input = new String[2];
		input[0] = "urn:urn1";
		input[1] = "urn:urn2";
		URI[] output = new URI[2];
		output[0] = URI.create("urn:urn1");
		output[1] = URI.create("urn:urn2");
		assertEquals(Arrays.asList(output), Arrays.asList(obj.array(input)));
	}

	public void testStar() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		assertEquals("star&one=one", obj.star("star", "one"));
	}

	public void testPostQuery() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		assertEquals("Hello World!", obj.postQuery("q", "body"));
		obj.setWorld("Toronto"); // local in-memory property
		assertEquals("Hello World!", obj.postQuery("q", "body"));
	}

	public void testMapArray() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		Map<String, String[]> map = new LinkedHashMap<String, String[]>();
		map.put("first", new String[] { "urn:urn1" });
		map.put("second", new String[] { "urn:urn2" });
		Map<String, String[]> output = obj.mapArray(map);
		assertEquals(Arrays.asList(map.get("first")), Arrays.asList(output
				.get("first")));
		assertEquals(Arrays.asList(map.get("second")), Arrays.asList(output
				.get("second")));
	}

	public void testMap() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("first", "urn:urn1");
		map.put("second", "urn:urn2");
		Map<String, String> output = obj.map(map);
		assertEquals(map.get("first"), output.get("first"));
		assertEquals(map.get("second"), output.get("second"));
	}

	public void testMapOfURI() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		Map<String, URI> map = new LinkedHashMap<String, URI>();
		map.put("first", URI.create("urn:urn1"));
		map.put("second", URI.create("urn:urn2"));
		Map<String, URI> output = obj.uris(map);
		assertEquals(map.get("first"), output.get("first"));
		assertEquals(map.get("second"), output.get("second"));
	}

	public void testBinary() throws Exception {
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		byte[] binary = "binary string".getBytes();
		assertTrue(Arrays.equals(binary, obj.binary(binary)));
	}

	public void testBigBinary() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (int i = 0; i < 100; i++) {
			out.write("All work and no play makes Jack a dull boy.\n"
					.getBytes("UTF-8"));
		}
		String uri = client.path("/object").toString();
		WebInterface obj = con.addDesignation(con.getObject(uri),
				WebInterface.class);
		byte[] binary = out.toByteArray();
		assertTrue(Arrays.equals(binary, obj.binary(binary)));
	}

	public void testGET() throws Exception {
		String uri1 = client.path("/cup1").toString();
		String uri2 = client.path("/cup2").toString();
		HotChocolate chocolate = con.addDesignation(con.getObject(uri1),
				HotChocolate.class);
		Milk milk = con.addDesignation(con.getObject(uri2), Milk.class);
		chocolate.add(milk);
		assertEquals(1, chocolate.getAmountOfMilk());
	}

	public void testPUT() throws Exception {
		HotChocolate.count = 0;
		String uri1 = client.path("/cup1").toString();
		String uri2 = client.path("/cup2").toString();
		HotChocolate chocolate = con.addDesignation(con.getObject(uri1),
				HotChocolate.class);
		Milk milk = con.addDesignation(con.getObject(uri2), Milk.class);
		milk.pourInto(chocolate);
		assertEquals(1, HotChocolate.count);
		assertEquals(1, chocolate.getAmountOfMilk());
	}

	public void testDELETE() throws Exception {
		String uri1 = client.path("/cup1").toString();
		String uri2 = client.path("/cup2").toString();
		HotChocolate chocolate = con.addDesignation(con.getObject(uri1),
				HotChocolate.class);
		Milk milk = con.addDesignation(con.getObject(uri2), Milk.class);
		milk.pourInto(chocolate);
		chocolate.consume();
		try {
			chocolate.getAmountOfMilk();
			fail();
		} catch (MethodNotAllowed e) {
			// chocolate has already been eaten in another transaction
		}
	}

	public void testPOST() throws Exception {
		String uri1 = client.path("/cup1").toString();
		String uri2 = client.path("/cup2").toString();
		Chocolate chocolate = con.addDesignation(con.getObject(uri1),
				Chocolate.class);
		Milk milk = con.addDesignation(con.getObject(uri2), Milk.class);
		HotChocolate hot = chocolate.mix(milk);
		assertEquals(1, hot.getAmountOfMilk());
	}

	public void testProxy() throws Exception {
		MyFile.addr = new InetSocketAddress("localhost", port);
		WebInterface obj = con.addDesignation(con.getObject("file:///object"),
				WebInterface.class);
		assertEquals("Hello World!", obj.hello());
		obj.setWorld("Toronto"); // local in-memory property
		assertEquals("Hello World!", obj.hello());
	}
}
