/*
 * Copyright (c) 2012 3 Round Stones Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Parses streams into {@link DOMSource}.
 * 
 * @author James Leigh
 *
 */
public class DOMSourceFactory {
	public static DOMSourceFactory newInstance() {
		// StreamSource loads external DTD
		// XML Stream/Event to StAXSource drops comments
		return new DOMSourceFactory(DocumentFactory.newInstance());
	}

	private DocumentFactory factory;

	protected DOMSourceFactory(DocumentFactory factory) {
		this.factory = factory;
	}

	public DOMSource createSource(InputStream in, String systemId)
			throws IOException, SAXException, ParserConfigurationException {
		try {
			if (systemId == null)
				return createSource(factory.parse(in), null);
			return createSource(factory.parse(in, systemId), systemId);
		} finally {
			in.close();
		}
	}

	public DOMSource createSource(Reader reader, String systemId) throws IOException, SAXException, ParserConfigurationException {
		try {
			if (systemId == null)
				return createSource(factory.parse(reader), null);
			return createSource(factory.parse(reader, systemId), systemId);
		} finally {
			reader.close();
		}
	}

	public DOMSource createSource(Node node, String systemId) throws ParserConfigurationException {
		if (node instanceof Document) {
			return createSource((Document) node, systemId);
		} else if (node instanceof DocumentFragment) {
			if (node.getChildNodes().getLength() == 1)
				return createSource(node.getFirstChild(), systemId);
			Document doc = newDocument();
			Element root = doc.createElement("root");
			root.appendChild(doc.importNode(node, true));
			doc.appendChild(root);
			return createSource(doc, systemId);
		} else {
			Document doc = newDocument();
			doc.appendChild(doc.importNode(node, true));
			return createSource(doc, systemId);
		}
	}

	public DOMSource createSource(Document node, String systemId) {
		if (systemId == null) {
			String documentURI = ((Document) node).getDocumentURI();
			if (documentURI != null)
				return createSource(node, documentURI);
		}
		if (systemId == null)
			return new DOMSource(node);
		return new DOMSource(node, systemId);
	}

	private Document newDocument() throws ParserConfigurationException {
		return factory.newDocument();
	}

}
