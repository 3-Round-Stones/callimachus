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
package org.callimachusproject.server.providers;

import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.callimachusproject.annotations.requires;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.annotations.Matching;
import org.openrdf.annotations.Method;
import org.openrdf.annotations.Path;
import org.openrdf.annotations.Type;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

public class DOMProviderTest extends MetadataServerTestCase {

	private static final String XML = "<?xml version=\"1.0\" encoding=\""
			+ Charset.defaultCharset().name() + "\"?>";

	@Matching("/")
	public static class Controller {
		private DocumentBuilderFactory builder;

		public Controller() {
			builder = DocumentBuilderFactory.newInstance();
			builder.setNamespaceAware(true);
			try {
				builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			} catch (ParserConfigurationException e) {
				throw new AssertionError(e);
			}
		}

		@Method("GET")
		@Path("?document")
		@requires("urn:test:grant")
		@Type("application/xml")
		public Document document() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			Element element = doc.createElement("document");
			doc.appendChild(element);
			return doc;
		}

		@Method("GET")
		@Path("?element")
		@requires("urn:test:grant")
		@Type("application/xml")
		public Element element() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			return doc.createElement("element");
		}

		@Method("GET")
		@Path("?fragment")
		@requires("urn:test:grant")
		@Type("application/xml")
		public DocumentFragment fragment() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			DocumentFragment frag = doc.createDocumentFragment();
			Element element = doc.createElement("fragment");
			frag.appendChild(element);
			return frag;
		}

		@Method("GET")
		@Path("?document")
		@requires("urn:test:grant")
		public void document(@Type("*/*") Document document) throws ParserConfigurationException {
			assert document.hasChildNodes();
		}

		@Method("GET")
		@Path("?element")
		@requires("urn:test:grant")
		public void element(@Type("*/*") Element element) throws ParserConfigurationException {
			assert element.getNodeName().equals("element");
		}

		@Method("GET")
		@Path("?fragment")
		@requires("urn:test:grant")
		public void fragment(@Type("*/*") DocumentFragment frag) throws ParserConfigurationException {
			assert frag.hasChildNodes();
		}
	}

	public void setUp() throws Exception {
		config.addConcept(Controller.class);
		super.setUp();
	}

	public void testDocument() throws Exception {
		assertTrue(getString("document").trim().endsWith("<document/>"));
		putXML("document", "<document/>");
	}

	public void testElement() throws Exception {
		assertEquals(XML + "<element/>", getString("element"));
		putXML("element", "<element/>");
	}

	public void testFragment() throws Exception {
		assertEquals("<fragment/>", getString("fragment"));
		putXML("fragment", "<fragment/>");
	}

	private String getString(String operation) {
		WebResource path = client.path("/").queryParam(operation, "");
		Builder req = path.header("Accept-Encoding", "gzip;q=0");
		return req.get(String.class);
	}

	private void putXML(String operation, String data) {
		WebResource path = client.path("/").queryParam(operation, "");
		Builder req = path.header("Accept-Encoding", "gzip;q=0");
		req.header("Content-Type", "text/xml").put(data);
	}

}
