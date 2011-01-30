/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.stream;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;

/**
 * Wrapper class for {@link StartElement}.
 * 
 * @author James Leigh
 *
 */
public class DelegatingStartElement implements StartElement {
	private StartElement element;

	public DelegatingStartElement(StartElement element) {
		this.element = element;
	}

	@Override
	public String toString() {
		StringWriter writer = new StringWriter();
		try {
			writeAsEncodedUnicode(writer);
		} catch (XMLStreamException e) {
			return element.toString();
		}
		return writer.toString();
	}

	@Override
	public int hashCode() {
		return element.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return element.equals(obj);
	}

	public StartElement asStartElement() {
		return this;
	}

	public Characters asCharacters() {
		return element.asCharacters();
	}

	public EndElement asEndElement() {
		return element.asEndElement();
	}

	public Attribute getAttributeByName(QName name) {
		return element.getAttributeByName(name);
	}

	public Iterator getAttributes() {
		return element.getAttributes();
	}

	public int getEventType() {
		return element.getEventType();
	}

	public Location getLocation() {
		return element.getLocation();
	}

	public QName getName() {
		return element.getName();
	}

	public NamespaceContext getNamespaceContext() {
		return element.getNamespaceContext();
	}

	public Iterator getNamespaces() {
		return element.getNamespaces();
	}

	public String getNamespaceURI(String prefix) {
		return element.getNamespaceURI(prefix);
	}

	public QName getSchemaType() {
		return element.getSchemaType();
	}

	public boolean isAttribute() {
		return element.isAttribute();
	}

	public boolean isCharacters() {
		return element.isCharacters();
	}

	public boolean isEndDocument() {
		return element.isEndDocument();
	}

	public boolean isEndElement() {
		return element.isEndElement();
	}

	public boolean isEntityReference() {
		return element.isEntityReference();
	}

	public boolean isNamespace() {
		return element.isNamespace();
	}

	public boolean isProcessingInstruction() {
		return element.isProcessingInstruction();
	}

	public boolean isStartDocument() {
		return element.isStartDocument();
	}

	public boolean isStartElement() {
		return element.isStartElement();
	}

	public void writeAsEncodedUnicode(Writer w) throws XMLStreamException {
		try {
			w.write('<');
			String prefix = getName().getPrefix();
			if (prefix != null && prefix.length() > 0) {
				w.write(prefix);
				w.write(':');
			}
			w.write(getName().getLocalPart());
			outputNamespaceDeclarations(w);
			outputAttr(w);

			w.write('>');
		} catch (IOException ie) {
			throw new XMLStreamException(ie);
		}
	}

	private void outputNamespaceDeclarations(Writer w) throws IOException {
		Iterator iter = getNamespaces();
		while (iter.hasNext()) {
			Namespace ns = (Namespace) iter.next();
			w.write(' ');
			w.write(XMLConstants.XMLNS_ATTRIBUTE);
			String prefix = ns.getPrefix();
			if (prefix != null && prefix.length() > 0) {
				w.write(':');
				w.write(prefix);
			}
			w.write("=\"");
			w.write(ns.getValue());
			w.write('"');
		}
	}

	private void outputAttr(Writer w) throws IOException {
		Iterator it = getAttributes();
		while (it.hasNext()) {
			Attribute attr = (Attribute) it.next();
			// Let's only output explicit attribute values:
			if (!attr.isSpecified()) {
				continue;
			}

			w.write(' ');
			QName name = attr.getName();
			String prefix = name.getPrefix();
			if (prefix != null && prefix.length() > 0) {
				w.write(prefix);
				w.write(':');
			}
			w.write(name.getLocalPart());
			w.write("=\"");
			String val = attr.getValue();
			if (val != null && val.length() > 0) {
				writeEscapedAttrValue(w, val);
			}
			w.write('"');
		}
	}

	private void writeEscapedAttrValue(Writer w, String value)
			throws IOException {
		int i = 0;
		int len = value.length();
		do {
			int start = i;
			char c = '\u0000';

			for (; i < len; ++i) {
				c = value.charAt(i);
				if (c == '<' || c == '&' || c == '"') {
					break;
				}
			}
			int outLen = i - start;
			if (outLen > 0) {
				w.write(value, start, outLen);
			}
			if (i < len) {
				if (c == '<') {
					w.write("&lt;");
				} else if (c == '&') {
					w.write("&amp;");
				} else if (c == '"') {
					w.write("&quot;");

				}
			}
		} while (++i < len);
	}

}
