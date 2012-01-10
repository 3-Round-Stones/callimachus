package org.callimachusproject.server.providers;

import java.nio.charset.Charset;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.callimachusproject.server.annotations.query;
import org.callimachusproject.server.annotations.type;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.openrdf.annotations.Matching;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.WebResource.Builder;

public class DOMProviderTest extends MetadataServerTestCase {

	private static final String XML_NO = "<?xml version=\"1.0\" encoding=\""
			+ Charset.defaultCharset().name() + "\" standalone=\"no\"?>";
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
		@type("application/xml")
		public Document document() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			Element element = doc.createElement("document");
			doc.appendChild(element);
			return doc;
		}

		@query("element")
		@type("application/xml")
		public Element element() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			return doc.createElement("element");
		}

		@query("fragment")
		@type("application/xml")
		public DocumentFragment fragment() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			DocumentFragment frag = doc.createDocumentFragment();
			Element element = doc.createElement("fragment");
			frag.appendChild(element);
			return frag;
		}

		@query("fragment-dual")
		@type("application/xml")
		public DocumentFragment fragmentDual() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			DocumentFragment frag = doc.createDocumentFragment();
			Element first = doc.createElement("first");
			Element second = doc.createElement("second");
			frag.appendChild(first);
			frag.appendChild(second);
			return frag;
		}

		@query("fragment-whitespace")
		@type("application/xml")
		public DocumentFragment fragmentWhite() throws ParserConfigurationException {
			Document doc = builder.newDocumentBuilder().newDocument();
			DocumentFragment frag = doc.createDocumentFragment();
			Text space = doc.createTextNode(" ");
			Element element = doc.createElement("fragment");
			frag.appendChild(space);
			frag.appendChild(element);
			return frag;
		}

		@query("document")
		@type("application/xml")
		public void document(@type("*/*") Document document) throws ParserConfigurationException {
			assert document.hasChildNodes();
		}

		@query("element")
		@type("application/xml")
		public void element(@type("*/*") Element element) throws ParserConfigurationException {
			assert element.getNodeName().equals("element");
		}

		@query("fragment")
		@type("application/xml")
		public void fragment(@type("*/*") DocumentFragment frag) throws ParserConfigurationException {
			assert frag.hasChildNodes();
		}

		@query("fragment-dual")
		@type("application/xml")
		public void fragmentDual(@type("*/*") DocumentFragment frag) throws ParserConfigurationException {
			assertEquals(2, frag.getChildNodes().getLength());
		}

		@query("fragment-whitespace")
		@type("application/xml")
		public void fragmentWhite(@type("*/*") DocumentFragment frag) throws ParserConfigurationException {
			assertEquals(2, frag.getChildNodes().getLength());
		}
	}

	public void setUp() throws Exception {
		config.addConcept(Controller.class);
		super.setUp();
	}

	public void testDocument() throws Exception {
		assertEquals(XML_NO + "<document/>", getString("document"));
		putString("document", "<document/>");
	}

	public void testElement() throws Exception {
		assertEquals(XML + "<element/>", getString("element"));
		putString("element", "<element/>");
	}

	public void testFragment() throws Exception {
		assertEquals("<fragment/>", getString("fragment"));
		putString("fragment", "<fragment/>");
	}

	public void testFragmentDual() throws Exception {
		assertEquals("<first/><second/>", getString("fragment-dual"));
		putString("fragment-dual", "<first/><second/>");
	}

	public void testFragmentWhitespace() throws Exception {
		assertEquals(" <fragment/>", getString("fragment-whitespace"));
		putString("fragment-whitespace", " <fragment/>");
	}

	private String getString(String operation) {
		WebResource path = client.path("/").queryParam(operation, "");
		Builder req = path.header("Accept-Encoding", "gzip;q=0");
		return req.get(String.class);
	}

	private void putString(String operation, String data) {
		WebResource path = client.path("/").queryParam(operation, "");
		Builder req = path.header("Accept-Encoding", "gzip;q=0");
		req.put(data);
	}

}
