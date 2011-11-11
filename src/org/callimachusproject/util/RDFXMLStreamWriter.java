/*
   Copyright (c) 2011 3 Round Stones Inc, Some Rights Reserved

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
package org.callimachusproject.util;


import java.net.URISyntaxException;
import java.util.regex.Pattern;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;

public class RDFXMLStreamWriter implements RDFWriter {
	private static final Pattern ESCAPED = Pattern.compile("^\\s|<|&|>|\\s$");
	private final XMLStreamWriter writer;
	private Resource open;
	private String baseURI;
	private String authURI;
	private String pathURI;
	private String queryURI;
	private String fragURI;

	public RDFXMLStreamWriter(XMLStreamWriter writer) {
		this.writer = writer;
	}

	public void setBaseURI(String baseURI) {
		this.baseURI = baseURI;
		if (baseURI == null || java.net.URI.create(baseURI).isOpaque()) {
			authURI = null;
			pathURI = null;
			queryURI = null;
			fragURI = null;
		} else {
			try {
				java.net.URI parsed = java.net.URI.create(baseURI);
				String s = parsed.getScheme();
				String a = parsed.getAuthority();
				authURI = new java.net.URI(s, a, "/", null, null).toString();
			} catch (URISyntaxException e) {
				authURI = null;
			}
			int path = baseURI.lastIndexOf('/');
			pathURI = baseURI.substring(0, path + 1);
			int query = baseURI.lastIndexOf('?');
			if (query < 0) {
				queryURI = baseURI + "?";
			} else {
				queryURI = baseURI.substring(0, query + 1);
			}
			int frag = baseURI.lastIndexOf('#');
			if (frag < 0) {
				fragURI = baseURI + "#";
			} else {
				fragURI = baseURI.substring(0, frag + 1);
			}
		}
	}

	@Override
	public void startRDF() throws RDFHandlerException {
		try {
			startDocument();
		} catch (XMLStreamException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void endRDF() throws RDFHandlerException {
		try {
			endDocument();
		} catch (XMLStreamException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleNamespace(String prefix, String uri)
			throws RDFHandlerException {
		try {
			namespace(prefix, uri);
		} catch (XMLStreamException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleStatement(Statement st) throws RDFHandlerException {
		try {
			statement(st);
		} catch (XMLStreamException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public void handleComment(String data) throws RDFHandlerException {
		try {
			comment(data);
		} catch (XMLStreamException e) {
			throw new RDFHandlerException(e);
		}
	}

	@Override
	public RDFFormat getRDFFormat() {
		return RDFFormat.RDFXML;
	}

	private void startDocument() throws XMLStreamException {
		writer.writeStartDocument();
		writer.writeStartElement("rdf", "RDF", RDF.NAMESPACE);
	}

	private synchronized void endDocument() throws XMLStreamException {
		if (open != null) {
			writer.writeEndElement();
			open = null;
		}
		writer.writeEndElement();
		writer.writeEndDocument();
		writer.flush();
	}

	private void namespace(String prefix, String uri)
			throws XMLStreamException {
		if (open == null) {
			writer.writeNamespace(prefix, uri);
		}
	}

	private synchronized void subject(Resource subject) throws XMLStreamException {
		if (!subject.equals(open)) {
			if (open != null) {
				writer.writeEndElement();
				open = null;
			}
			writer.writeStartElement("rdf", "Description", RDF.NAMESPACE);
			if (subject instanceof URI) {
				writer.writeAttribute("rdf", RDF.NAMESPACE, "about", relativize(subject.stringValue()));
			} else {
				writer.writeAttribute("rdf", RDF.NAMESPACE, "nodeID", subject.stringValue());
			}
			open = subject;
		}
	}

	private synchronized void statement(Statement st) throws XMLStreamException {
		subject(st.getSubject());
		writer.writeStartElement(st.getPredicate().getNamespace(), st.getPredicate().getLocalName());
		if (st.getObject() instanceof Literal) {
			Literal lit = (Literal) st.getObject();
			if (lit.getLanguage() != null) {
				writer.writeAttribute("xml:lang", lit.getLanguage());
			}
			if (lit.getDatatype() != null) {
				writer.writeAttribute("rdf", RDF.NAMESPACE, "datatype", relativize(lit.getDatatype().stringValue()));
			}
			if (ESCAPED.matcher(lit.stringValue()).find()) {
				writer.writeCData(lit.stringValue());
			} else {
				writer.writeCharacters(lit.stringValue());
			}
		} else if (st.getObject() instanceof URI) {
			writer.writeAttribute("rdf", RDF.NAMESPACE, "resource", relativize(st.getObject().stringValue()));
		} else {
			writer.writeAttribute("rdf", RDF.NAMESPACE, "nodeID", st.getObject().stringValue());
		}
		writer.writeEndElement();
	}

	private String relativize(String uri) {
		if (uri.equals(baseURI))
			return "";
		if (pathURI == null)
			return uri;
		if (uri.startsWith(fragURI))
			return uri.substring(fragURI.length() - 1);
		if (uri.startsWith(queryURI))
			return uri.substring(queryURI.length() - 1);
		if (uri.equals(pathURI))
			return ".";
		if (uri.startsWith(pathURI))
			return uri.substring(pathURI.length());
		if (uri.startsWith(authURI))
			return uri.substring(authURI.length() - 1);
		return uri;
	}

	private void comment(String data) throws XMLStreamException {
		writer.writeComment(data);
	}
	
}