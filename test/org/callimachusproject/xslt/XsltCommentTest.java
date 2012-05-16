package org.callimachusproject.xslt;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.w3c.dom.Document;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

public class XsltCommentTest extends TestCase {

	private ClassLoader cl = XsltBenchmark.class.getClassLoader();
	public static final String XSLT_ECHO = "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
			+ "<xsl:template match='@*|node()'>"
			+ "<xsl:copy>"
			+ "<xsl:apply-templates select='@*|node()'/>"
			+ "</xsl:copy>"
			+ "</xsl:template></xsl:stylesheet>";
	private InputStream input;
	private final DocumentBuilderFactory builder = DocumentBuilderFactory
			.newInstance();
	private ByteArrayOutputStream output;
	{
		builder.setNamespaceAware(true);
		try {
			builder.setFeature(
					"http://apache.org/xml/features/nonvalidating/load-external-dtd",
					false);
		} catch (ParserConfigurationException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public void setUp() throws Exception {
		input = cl.getResourceAsStream("org/callimachusproject/xslt/page-comment.xhtml");
		output = new ByteArrayOutputStream(262144);
	}

	@Override
	public void tearDown() throws Exception {
		input.close();
	}

	public void testXSLT() throws Exception {
		new XSLTransformer(new StringReader(XSLT_ECHO), "http://example.com/").transform(input, "http://example.com/").toOutputStream(output);
		verifyResult();
	}

	private void verifyResult() throws Exception {
		InputStream in = cl.getResourceAsStream("org/callimachusproject/xslt/page-comment.xhtml");
		try {
			DocumentBuilder db = builder.newDocumentBuilder();
			Document doc = db.parse(in);
			DOMImplementationRegistry registry = DOMImplementationRegistry
					.newInstance();
			DOMImplementationLS impl = (DOMImplementationLS) registry
					.getDOMImplementation("LS");
			LSSerializer writer = impl.createLSSerializer();
			String str = writer.writeToString(doc);
			String actual = new String(output.toByteArray(), "UTF-8");
			assertTrue(actual.contains("<html"));
			assertEquals(str.substring(str.indexOf("<html")), actual.substring(actual.indexOf("<html")));
		} finally {
			in.close();
		}
	}
}
