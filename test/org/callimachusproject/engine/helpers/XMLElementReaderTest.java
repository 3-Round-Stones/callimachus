package org.callimachusproject.engine.helpers;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import junit.framework.TestCase;

public class XMLElementReaderTest extends TestCase {
	private XMLInputFactory xif = XMLInputFactory.newInstance();
	private XMLOutputFactory xof = XMLOutputFactory.newInstance();

	public String extract(String xptr, String xml) throws Exception {
		XMLEventReader input = xif.createXMLEventReader(new StringReader(xml));
		XMLEventReader output = new XMLElementReader(input, xptr);
		StringWriter out = new StringWriter();
		XMLEventWriter writer = xof.createXMLEventWriter(out);
		writer.add(output);
		writer.close();
		return out.toString();
	}

	public void testRootPath() throws Exception {
		assertEquals(
				"<?xml version=\"1.0\"?><root><child id=\"child\"><leaf></leaf></child></root>",
				extract("/1",
						"<?xml version=\"1.0\"?><root><child id=\"child\"><leaf></leaf></child></root>"));
	}

	public void testPath() throws Exception {
		assertEquals(
				"<?xml version=\"1.0\"?><child id=\"child\"><leaf></leaf></child>",
				extract("/1/1",
						"<?xml version=\"1.0\"?><root><child id=\"child\"><leaf></leaf></child></root>"));
	}

	public void testId() throws Exception {
		assertEquals(
				"<?xml version=\"1.0\"?><child id=\"child\"><leaf></leaf></child>",
				extract("child",
						"<?xml version=\"1.0\"?><root><child id=\"child\"><leaf></leaf></child></root>"));
	}

	public void testIdPath() throws Exception {
		assertEquals(
				"<?xml version=\"1.0\"?><leaf></leaf>",
				extract("child/1",
						"<?xml version=\"1.0\"?><root><child id=\"child\"><leaf></leaf></child></root>"));
	}
}
