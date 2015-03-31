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

import static java.util.Collections.EMPTY_MAP;
import static java.util.Collections.EMPTY_SET;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.BasicHttpResponse;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.server.helpers.XMLEventQueue;
import org.openrdf.annotations.Method;
import org.openrdf.annotations.Path;
import org.openrdf.annotations.Type;
import org.openrdf.http.object.io.ChannelUtil;
import org.openrdf.repository.object.ObjectConnection;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

public class RoundTripTest extends MetadataServerTestCase {

	public static class Trip {
		@Method("POST")@Path("?bool")
		public boolean bool(@Type("*/*") boolean param) {
			return param;
		}

		@Method("POST")@Path("?boolean")
		public Boolean boolObject(@Type("*/*") Boolean param) {
			return param;
		}

		@Method("POST")@Path("?setOfBoolean")
		public Set<Boolean> boolSet(@Type("*/*") Set<Boolean> param) {
			return param;
		}

		@Method("POST")@Path("?byteArray")
		public byte[] byteArray(@Type("*/*") byte[] param) {
			return param;
		}

		@Method("POST")@Path("?setOfbyteArray")
		public Set<byte[]> byteArraySet(@Type("*/*") Set<byte[]> param) {
			return param;
		}

		@Method("POST")@Path("?byteArrayStream")
		public ByteArrayOutputStream byteArrayStream(@Type("*/*") ByteArrayOutputStream param) {
			return param;
		}

		@Method("POST")@Path("?setOfbyteArrayStream")
		public Set<ByteArrayOutputStream> byteArrayStreamSet(
				@Type("*/*") Set<ByteArrayOutputStream> param) {
			return param;
		}

		@Method("POST")@Path("?int")
		public int integ(@Type("*/*") int param) {
			return param;
		}

		@Method("POST")@Path("?integer")
		public Integer integer(@Type("*/*") Integer param) {
			return param;
		}

		@Method("POST")@Path("?setOfInteger")
		public Set<Integer> setOfInteger(@Type("*/*") Set<Integer> param) {
			return param;
		}

		@Method("POST")@Path("?bigInteger")
		public BigInteger bigInteger(@Type("*/*") BigInteger param) {
			return param;
		}

		@Method("POST")@Path("?setOfBigInteger")
		public Set<BigInteger> setOfBigInteger(@Type("*/*") Set<BigInteger> param) {
			return param;
		}

		@Method("POST")@Path("?document")
		public Document document(@Type("*/*") Document param) {
			return param;
		}

		@Method("POST")@Path("?setOfDocument")
		public Set<Document> setOfDocument(@Type("*/*") Set<Document> param) {
			return param;
		}

		@Method("POST")@Path("?documentFragment")
		public DocumentFragment documentFragment(@Type("*/*") DocumentFragment param) {
			return param;
		}

		@Method("POST")@Path("?setOfDocumentFragment")
		public Set<DocumentFragment> setOfDocumentFragment(
				@Type("*/*") Set<DocumentFragment> param) {
			return param;
		}

		@Method("POST")@Path("?element")
		public Element element(@Type("*/*") Element param) {
			return param;
		}

		@Method("POST")@Path("?setOfElement")
		public Set<Element> setOfElement(@Type("*/*") Set<Element> param) {
			return param;
		}

		@Method("POST")@Path("?map")
		public Map<String, String> map(@Type("*/*") Map<String, String> param) {
			return param;
		}

		@Method("POST")@Path("?setOfMap")
		public Set<Map<String, String>> setOfMap(@Type("*/*") Set<Map<String, String>> param) {
			return param;
		}

		@Method("POST")@Path("?mapOfIntegerToSetOfInteger")
		public Map<Integer, Set<Integer>> mapOfIntegerToSetOfInteger(
				@Type("*/*") Map<Integer, Set<Integer>> param) {
			return param;
		}

		@Method("POST")@Path("?httpMessage")
		public HttpMessage httpMessage(@Type("*/*") HttpMessage param) {
			return param;
		}

		@Method("POST")@Path("?inputStream")
		public InputStream inputStream(@Type("*/*") InputStream param) throws IOException {
			if (param == null)
				return null;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChannelUtil.transfer(param, out);
			return new ByteArrayInputStream(out.toByteArray());
		}

		@Method("POST")@Path("?setOfInputStream")
		public Set<InputStream> setOfInputStream(@Type("*/*") Set<InputStream> param)
				throws IOException {
			if (param == null || param.isEmpty())
				return param;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChannelUtil.transfer(param.iterator().next(), out);
			return singleton((InputStream) new ByteArrayInputStream(out
					.toByteArray()));
		}

		@Method("POST")@Path("?readableByteChannel")
		public ReadableByteChannel readableByteChannel(@Type("*/*") ReadableByteChannel param)
				throws IOException {
			if (param == null)
				return null;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChannelUtil.transfer(param, out);
			return ChannelUtil.newChannel(out.toByteArray());
		}

		@Method("POST")@Path("?string")
		public String string(@Type("*/*") String param) throws IOException {
			return param;
		}

		@Method("POST")@Path("?xmlEventReader")
		public XMLEventReader xmlEventReader(@Type("*/*") XMLEventReader param)
				throws XMLStreamException {
			if (param == null)
				return null;
			try {
				XMLEventQueue queue = new XMLEventQueue();
				while (param.hasNext()) {
					queue.add(param.nextEvent());
				}
				return queue.getXMLEventReader();
			} finally {
				param.close();
			}
		}
	}

	private ObjectConnection con;
	private Trip trip;

	public void setUp() throws Exception {
		config.addConcept(Trip.class, "urn:test:Trip");
		super.setUp();
		con = repository.getConnection();
		trip = con.addDesignation(con.getObject(getOrigin() + "/trip"),
				Trip.class);
	}

	public void tearDown() throws Exception {
		con.close();
		super.tearDown();
	}

	public void testBool() throws Exception {
		assertEquals(true, trip.bool(true));
		assertEquals(false, trip.bool(false));
	}

	public void testBoolean() throws Exception {
		assertEquals(Boolean.TRUE, trip.boolObject(Boolean.TRUE));
		assertEquals(Boolean.FALSE, trip.boolObject(Boolean.FALSE));
		assertEquals(null, trip.boolObject(null));
	}

	public void testBooleanSet() throws Exception {
		assertEquals(singleton(Boolean.TRUE), trip
				.boolSet(singleton(Boolean.TRUE)));
		assertEquals(singleton(Boolean.FALSE), trip
				.boolSet(singleton(Boolean.FALSE)));
		assertEquals(EMPTY_SET, trip.boolSet(EMPTY_SET));
	}

	public void testByteArray() throws Exception {
		assertEquals("byte", new String(trip.byteArray("byte".getBytes())));
		assertEquals("", new String(trip.byteArray("".getBytes())));
		assertEquals(null, trip.byteArray(null));
	}

	public void testByteArraySet() throws Exception {
		assertEquals("byte", new String(trip.byteArraySet(
				singleton("byte".getBytes())).iterator().next()));
		assertEquals("", new String(trip.byteArraySet(singleton("".getBytes()))
				.iterator().next()));
		assertEquals(EMPTY_SET, trip.byteArraySet(EMPTY_SET));
	}

	public void testByteArrayStream() throws Exception {
		ByteArrayOutputStream param = new ByteArrayOutputStream();
		param.write("byte".getBytes());
		assertEquals("byte", trip.byteArrayStream(param).toString());
		assertEquals("", trip.byteArrayStream(new ByteArrayOutputStream())
				.toString());
		assertEquals(null, trip.byteArrayStream(null));
	}

	public void testByteArrayStreamSet() throws Exception {
		ByteArrayOutputStream param = new ByteArrayOutputStream();
		param.write("byte".getBytes());
		assertEquals("[byte]", trip.byteArrayStreamSet(singleton(param))
				.toString());
		assertEquals("[]", trip.byteArrayStreamSet(
				singleton(new ByteArrayOutputStream())).toString());
		assertEquals(EMPTY_SET, trip.byteArrayStreamSet(EMPTY_SET));
	}

	public void testInt() throws Exception {
		assertEquals(1, trip.integ(1));
		assertEquals(0, trip.integ(0));
	}

	public void testInteger() throws Exception {
		assertEquals(new Integer(1), trip.integer(1));
		assertEquals(new Integer(0), trip.integer(0));
		assertEquals(null, trip.bigInteger(null));
	}

	public void testSetOfInteger() throws Exception {
		assertEquals(singleton(new Integer("1")), trip
				.setOfInteger(singleton(1)));
		assertEquals(singleton(new Integer("0")), trip
				.setOfInteger(singleton(0)));
		assertEquals(EMPTY_SET, trip.setOfInteger(EMPTY_SET));
	}

	public void testBigInteger() throws Exception {
		assertEquals(new BigInteger("1"), trip.bigInteger(new BigInteger("1")));
		assertEquals(new BigInteger("0"), trip.bigInteger(new BigInteger("0")));
		assertEquals(null, trip.bigInteger(null));
	}

	public void testSetOfBigInteger() throws Exception {
		assertEquals(singleton(new BigInteger("1")), trip
				.setOfBigInteger(singleton(new BigInteger("1"))));
		assertEquals(singleton(new BigInteger("0")), trip
				.setOfBigInteger(singleton(new BigInteger("0"))));
		assertEquals(EMPTY_SET, trip.setOfBigInteger(EMPTY_SET));
	}

	public void testDocument() throws Exception {
		DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
		Document doc = builder.newDocumentBuilder().newDocument();
		doc.appendChild(doc.createElement("root"));
		trip.document(doc);
		assertEquals(null, trip.document(null));
	}

	public void testSetOfDocument() throws Exception {
		DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
		Document doc = builder.newDocumentBuilder().newDocument();
		doc.appendChild(doc.createElement("root"));
		trip.setOfDocument(singleton(doc));
		assertEquals(EMPTY_SET, trip.setOfDocument(EMPTY_SET));
	}

	public void testDocumentFragment() throws Exception {
		DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
		Document doc = builder.newDocumentBuilder().newDocument();
		DocumentFragment frag = doc.createDocumentFragment();
		frag.appendChild(doc.createElement("root1"));
		frag.appendChild(doc.createElement("root2"));
		trip.documentFragment(frag);
		assertEquals(null, trip.documentFragment(null));
	}

	public void testSetOfDocumentFragment() throws Exception {
		DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
		Document doc = builder.newDocumentBuilder().newDocument();
		DocumentFragment frag = doc.createDocumentFragment();
		frag.appendChild(doc.createElement("root1"));
		frag.appendChild(doc.createElement("root2"));
		trip.setOfDocumentFragment(singleton(frag));
		assertEquals(EMPTY_SET, trip.setOfDocumentFragment(EMPTY_SET));
	}

	public void testSingleDocumentFragment() throws Exception {
		DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
		Document doc = builder.newDocumentBuilder().newDocument();
		DocumentFragment frag = doc.createDocumentFragment();
		frag.appendChild(doc.createElement("root"));
		trip.documentFragment(frag);
		assertEquals(null, trip.documentFragment(null));
	}

	public void testSetOfSingleDocumentFragment() throws Exception {
		DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
		Document doc = builder.newDocumentBuilder().newDocument();
		DocumentFragment frag = doc.createDocumentFragment();
		frag.appendChild(doc.createElement("root"));
		trip.setOfDocumentFragment(singleton(frag));
		assertEquals(EMPTY_SET, trip.setOfDocumentFragment(EMPTY_SET));
	}

	public void testElement() throws Exception {
		DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
		Document doc = builder.newDocumentBuilder().newDocument();
		trip.element(doc.createElement("root"));
		assertEquals(null, trip.element(null));
	}

	public void testSetOfElement() throws Exception {
		DocumentBuilderFactory builder = DocumentBuilderFactory.newInstance();
		Document doc = builder.newDocumentBuilder().newDocument();
		trip.setOfElement(singleton(doc.createElement("root")));
		assertEquals(EMPTY_SET, trip.setOfElement(EMPTY_SET));
	}

	public void testMap() throws Exception {
		assertEquals(singletonMap("key", "value"), trip.map(singletonMap("key",
				"value")));
		assertEquals(EMPTY_MAP, trip.map(EMPTY_MAP));
		assertEquals(null, trip.map(null));
	}

	public void testSetOfMap() throws Exception {
		assertEquals(singleton(singletonMap("key", "value")), trip
				.setOfMap(singleton(singletonMap("key", "value"))));
		assertEquals(singleton(EMPTY_MAP), trip
				.setOfMap(singleton((Map<String, String>) EMPTY_MAP)));
		assertEquals(EMPTY_SET, trip.setOfMap(EMPTY_SET));
	}

	public void testMapOfIntegerToSetOfInteger() throws Exception {
		assertEquals(singletonMap(1, singleton(2)), trip
				.mapOfIntegerToSetOfInteger(singletonMap(1, singleton(2))));
		assertEquals(EMPTY_MAP, trip.mapOfIntegerToSetOfInteger(EMPTY_MAP));
		assertEquals(null, trip.mapOfIntegerToSetOfInteger(null));
	}

	public void testHttpMessage() throws Exception {
		HttpRequest req = new BasicHttpRequest("GET", "urn:test:request");
		HttpResponse resp = new BasicHttpResponse(new ProtocolVersion("HTTP",
				1, 1), 204, "No Content");
		assertEquals(req.getRequestLine().toString(), ((HttpRequest) trip
				.httpMessage(req)).getRequestLine().toString());
		assertEquals(resp.getStatusLine().toString(), ((HttpResponse) trip
				.httpMessage(resp)).getStatusLine().toString());
		assertEquals(null, trip.httpMessage(null));
	}

	public void testInputStream() throws Exception {
		assertEquals("byte", new String(ChannelUtil.newByteArray(trip
				.inputStream(new ByteArrayInputStream("byte".getBytes())))));
		assertEquals("", new String(ChannelUtil.newByteArray(trip
				.inputStream(new ByteArrayInputStream("".getBytes())))));
		assertEquals(null, trip.inputStream(null));
	}

	public void testSetOfInputStream() throws Exception {
		assertEquals("byte", new String(ChannelUtil.newByteArray(trip
				.setOfInputStream(
						singleton((InputStream) new ByteArrayInputStream("byte"
								.getBytes()))).iterator().next())));
		assertEquals("", new String(ChannelUtil.newByteArray(trip
				.setOfInputStream(
						singleton((InputStream) new ByteArrayInputStream(""
								.getBytes()))).iterator().next())));
		assertEquals(EMPTY_SET, trip.setOfInputStream(EMPTY_SET));
	}

	public void testXMLEventReader() throws Exception {
		assertEquals(null, trip.xmlEventReader(null));
	}
}
