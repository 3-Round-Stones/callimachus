/**
 * 
 */
package org.callimachusproject.server.helpers;

import java.io.OutputStream;

import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class XHTMLInfoWriter {
	private XMLStreamWriter out;

	public XHTMLInfoWriter(OutputStream out) throws XMLStreamException,
			FactoryConfigurationError {
		this.out = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
	}

	public void writeAnchor(String href, String anchor)
			throws XMLStreamException {
		writeStartElement("a");
		writeAttribute("href", href);
		writeAttribute("id", anchor);
		writeAttribute("name", anchor);
		writeCharacters(anchor);
		writeEndElement();
	}

	public void writeAttribute(String localName, String value)
			throws XMLStreamException {
		if (value != null) {
			out.writeAttribute(localName, value);
		}
	}

	public void writeCharacters(String text) throws XMLStreamException {
		if (text != null) {
			out.writeCharacters(text);
		}
	}

	public void writeEmptyElement(String localName) throws XMLStreamException {
		out.writeEmptyElement(localName);
	}

	public void writeEndDocument() throws XMLStreamException {
		out.writeEndElement();
		out.writeEndElement();
		out.writeEndDocument();
	}

	public void writeEndElement() throws XMLStreamException {
		out.writeEndElement();
	}

	public void writeTitle(String label) throws XMLStreamException {
		writeStartElement("h1");
		writeCharacters(label);
		writeEndElement();
	}

	public void writeHeading(String label) throws XMLStreamException {
		writeStartElement("h2");
		writeCharacters(label);
		writeEndElement();
	}

	public void writeSubheading(String label) throws XMLStreamException {
		writeStartElement("h3");
		writeCharacters(label);
		writeEndElement();
	}

	public void writeLink(String href, String label) throws XMLStreamException {
		writeStartElement("a");
		writeAttribute("href", href);
		writeCharacters(label);
		writeEndElement();
	}

	public void writeLink(String href, String title, String label)
			throws XMLStreamException {
		writeStartElement("a");
		writeAttribute("href", href);
		writeAttribute("title", title);
		writeCharacters(label);
		writeEndElement();
	}

	public void writeDefinition(String key, String value)
			throws XMLStreamException {
		if (value != null) {
			if (key != null) {
				writeStartElement("dt");
				writeCharacters(key);
				writeEndElement();
			}
			writeStartElement("dd");
			writeCharacters(value);
			writeEndElement();
		}
	}

	public void writeStartDocument(String title) throws XMLStreamException {
		out.writeStartDocument();
		out.writeStartElement("html");
		out.setDefaultNamespace("http://www.w3.org/1999/xhtml");
		out.writeStartElement("head");
		out.writeStartElement("title");
		out.writeCharacters(title);
		out.writeEndElement();
		out.writeEndElement();
		out.writeStartElement("body");
	}

	public void writeStartElement(String localName) throws XMLStreamException {
		out.writeStartElement(localName);
	}
}