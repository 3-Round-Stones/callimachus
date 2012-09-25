package org.callimachusproject.server.providers;

import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.requires;
import org.callimachusproject.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.annotations.Matching;
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

		@query("document")
		@requires("urn:test:grant")
		@type("application/xml")
		public Document document() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			Element element = doc.createElement("document");
			doc.appendChild(element);
			return doc;
		}

		@query("element")
		@requires("urn:test:grant")
		@type("application/xml")
		public Element element() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			return doc.createElement("element");
		}

		@query("fragment")
		@requires("urn:test:grant")
		@type("application/xml")
		public DocumentFragment fragment() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			DocumentFragment frag = doc.createDocumentFragment();
			Element element = doc.createElement("fragment");
			frag.appendChild(element);
			return frag;
		}

		@query("document")
		@requires("urn:test:grant")
		public void document(@type("*/*") Document document) throws ParserConfigurationException {
			assert document.hasChildNodes();
		}

		@query("element")
		@requires("urn:test:grant")
		public void element(@type("*/*") Element element) throws ParserConfigurationException {
			assert element.getNodeName().equals("element");
		}

		@query("fragment")
		@requires("urn:test:grant")
		public void fragment(@type("*/*") DocumentFragment frag) throws ParserConfigurationException {
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
