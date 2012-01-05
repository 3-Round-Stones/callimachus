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
package org.callimachusproject.engine.impl;

import java.io.StringReader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.XMLLiteral;

/**
 * RDF XML literal.
 * 
 * @author James Leigh
 *
 */
public class XMLLiteralImpl extends XMLLiteral {
	private static XMLInputFactory factory = XMLInputFactory.newInstance();
	private String xml;
	private IRI xmlliteral;

	public XMLLiteralImpl(String xml, IRI xmlliteral) {
		this.xml = xml;
		this.xmlliteral = xmlliteral;
	}

	public String getLabel() {
		return xml;
	}

	public IRI getDatatype() {
		return xmlliteral;
	}

	@Override
	public String stringValue() {
		return xml;
	}

	@Override
	public XMLEventReader openXMLEventReader() throws XMLStreamException {
		String rdfwrapper = "<rdfwrapper>" + xml + "</rdfwrapper>";
		StringReader reader = new StringReader(rdfwrapper);
		XMLEventReader delegate = factory.createXMLEventReader(reader);
		return new XMLEventFilter(delegate) {
			private boolean wrapped;

			@Override
			protected boolean accept(XMLEvent event) throws XMLStreamException {
				if (wrapped) {
					if (event.isEndDocument() || peekNext().isEndDocument())
						return false;
					return true;
				} else if (event.isStartElement()) {
					wrapped = true;
				}
				return false;
			}

		};
	}

}
