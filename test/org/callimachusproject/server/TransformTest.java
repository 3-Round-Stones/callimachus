package org.callimachusproject.server;

import java.io.InputStream;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.transform;
import org.callimachusproject.annotations.type;
import org.callimachusproject.annotations.xslt;
import org.callimachusproject.server.base.MetadataServerTestCase;
import org.callimachusproject.xml.XMLEventReaderFactory;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Matching;
import org.openrdf.model.Model;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;

import com.sun.jersey.api.client.WebResource;

public class TransformTest extends MetadataServerTestCase {
	private static final String TURTLE_HELLO = "<urn:test:hello> <http://www.w3.org/1999/02/22-rdf-syntax-ns#value> \"hello world!\" .";
	public static final String XSLT_EXECUTE = "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
			+ "<xsl:output omit-xml-declaration='yes'/>"
			+ "<xsl:template match='echo'>"
			+ "<xsl:copy-of select='node()'/>"
			+ "</xsl:template></xsl:stylesheet>";

	@Matching("/service")
	public static abstract class Service {
		@query("world")
		@type("text/xml")
		@transform("urn:test:execute")
		public String world() {
			return "<echo>hello world!</echo>";
		}

		@query("hello")
		@type("text/plain")
		public String hello(
				@transform("urn:test:execute") @type("text/plain") String world) {
			return "hello " + world + "!";
		}

		@type("text/plain")
		@Iri("urn:test:execute")
		@xslt(XSLT_EXECUTE)
		public abstract String execute(@type("text/xml") String xml);

		@query("turtle")
		@type("application/x-turtle")
		public Model turtle(
				@transform("urn:test:rdfvalue") @type({ "application/rdf+xml",
						"application/x-turtle" }) GraphQueryResult result)
				throws QueryEvaluationException {
			Model model = new LinkedHashModel();
			while (result.hasNext()) {
				model.add(result.next());
			}
			return model;
		}

		@type("application/rdf+xml")
		@Iri("urn:test:rdfvalue")
		public Model extract(@type("text/string") String input) {
			Model model = new LinkedHashModel();
			model.add(new URIImpl("urn:test:hello"), RDF.VALUE,
					new LiteralImpl(input));
			return model;
		}

		@query("increment")
		@type("text/plain")
		public int increment(
				@transform("urn:test:decrypt") @query("number") int base) {
			return base + 1;
		}

		@Iri("urn:test:decrypt")
		@type("text/plain")
		public int decrypt(@type("text/plain") String number) {
			return Integer.parseInt(number, 2);
		}

		@query("indirect")
		@type("application/x-turtle")
		@transform("urn:test:serialize")
		public Model indirect() {
			Model model = new LinkedHashModel();
			model.add(new URIImpl("urn:test:hello"), RDF.VALUE,
					new LiteralImpl("hello world!"));
			return model;
		}

		@transform("urn:test:execute")
		@type("text/xml")
		@Iri("urn:test:serialize")
		public String serialize(@type("application/x-turtle") Model model) {
			return "<echo>" + model.objectString() + "</echo>";
		}

		@query("parse")
		@type("text/plain")
		public String parse(
				@transform("urn:test:rdfvalue") @type("*/*") GraphQueryResult result)
				throws QueryEvaluationException {
			return result.next().getObject().stringValue();
		}

		@type("text/plain")
		@Iri("urn:test:parse")
		public String parseModel(
				@transform("urn:test:read") @type("application/x-turtle") Model model)
				throws QueryEvaluationException {
			return model.objectString();
		}

		@type("application/x-turtle")
		@Iri("urn:test:read")
		public Model read(@type("application/x-turtle") Model input) {
			Model model = new LinkedHashModel();
			model.add(new URIImpl("urn:test:hello"), RDF.VALUE,
					new LiteralImpl("hello " + input.objectString() + "!"));
			return model;
		}

		@query("post")
		@type("application/x-turtle")
		@transform("urn:test:parse")
		public Model post(@type("*/*") String input) {
			Model model = new LinkedHashModel();
			model.add(new URIImpl("urn:test:hello"), RDF.VALUE,
					new LiteralImpl(input));
			return model;
		}

		@query("toxml")
		@transform("urn:test:toxml")
		@type("application/rdf+xml")
		public Model toxml() {
			Model model = new LinkedHashModel();
			model.add(new URIImpl("urn:test:hello"), RDF.VALUE,
					new LiteralImpl("hello world!"));
			return model;
		}

		@Iri("urn:test:toxml")
		@type("application/xml")
		public XMLEventReader xml(@type("application/rdf+xml") InputStream in)
				throws XMLStreamException {
			XMLEventReaderFactory factory = XMLEventReaderFactory.newInstance();
			return factory.createXMLEventReader(in);
		}

		@method("GET")
		@query("computeParam")
		@type("text/plain")
		@transform("urn:test:computeParam")
		@Iri("urn:test:computeTransformParam")
		public String computeTransformParam() {
			return "world";
		}

		@Iri("urn:test:computeParam")
		@type("text/plain")
		public String computeParam(@transform("urn:test:computeHello") String hello, @type("text/plain") String world) {
			return hello + " " + world + "!";
		}

		@Iri("urn:test:computeHello")
		@type("text/plain")
		public String computeHello() {
			return "Hello";
		}
	}

	public void setUp() throws Exception {
		config.addConcept(Service.class);
		super.setUp();
	}

	public void testNoOutboundTransform() {
		WebResource service = client.path("service").queryParam("world", "");
		assertEquals("<echo>hello world!</echo>", service.accept("text/xml")
				.get(String.class));
	}

	public void testOutboundTransform() {
		WebResource service = client.path("service").queryParam("world", "");
		assertEquals("hello world!", service.accept("text/plain").get(
				String.class));
	}

	public void testInboundTransform() {
		WebResource service = client.path("service").queryParam("hello", "");
		assertEquals("hello James!", service.accept("text/plain").type(
				"text/xml").post(String.class, "<echo>James</echo>"));
	}

	public void testRDFNoInboundTransform() {
		WebResource service = client.path("service").queryParam("turtle", "");
		assertTrimEquals(TURTLE_HELLO, service.accept("application/x-turtle").type(
				"application/x-turtle").post(String.class, TURTLE_HELLO));
	}

	public void testRDFInboundTransform() {
		WebResource service = client.path("service").queryParam("turtle", "");
		assertTrimEquals(TURTLE_HELLO, service.accept("application/x-turtle").type(
				"text/string").post(String.class, "hello world!"));
	}

	public void testTransformParameter() {
		WebResource service = client.path("service")
				.queryParam("increment", "").queryParam("number",
						Integer.toString(14, 2));
		assertEquals("15", service.type("text/plain").get(String.class));
	}

	public void testNoIndirect() {
		WebResource service = client.path("service").queryParam("indirect", "");
		assertTrimEquals(TURTLE_HELLO, service.accept("application/x-turtle").get(
				String.class));
	}

	public void testPartialIndirect() {
		WebResource service = client.path("service").queryParam("indirect", "");
		assertEquals("<echo>hello world!</echo>", service.accept("text/xml").get(
				String.class));
	}

	public void testIndirect() {
		WebResource service = client.path("service").queryParam("indirect", "");
		assertEquals("hello world!", service.accept("text/plain").get(
				String.class));
	}

	public void testParseNoInboundTransform() {
		WebResource service = client.path("service").queryParam("parse", "");
		assertTrimEquals("hello world!", service.accept("text/plain").type(
				"application/x-turtle").post(String.class, TURTLE_HELLO));
	}

	public void testParseInboundTransform() {
		WebResource service = client.path("service").queryParam("parse", "");
		assertEquals("hello world!", service.accept("text/plain").type(
				"text/string").post(String.class, "hello world!"));
	}

	public void testParseOutboundTransform() {
		WebResource service = client.path("service").queryParam("post", "");
		assertEquals("hello world!", service.accept("text/plain").type(
				"text/string").post(String.class, "world"));
	}

	public void testParameterType() {
		WebResource service = client.path("service").queryParam("toxml", "");
		String body = service.get(String.class);
		assertTrue(body.contains("rdf:RDF"));
	}

	public void testComputeTransformParam() {
		WebResource service = client.path("service").queryParam("computeParam", "");
		String body = service.get(String.class);
		assertTrimEquals("Hello world!", body);
	}

	private void assertTrimEquals(String expected, String actual) {
		if (actual == null) {
			assertEquals(null, expected, null);
		} else {
			assertEquals(null, expected, actual.trim());
		}
	}
}
