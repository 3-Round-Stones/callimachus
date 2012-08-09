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
package org.callimachusproject.engine.helpers;

import static java.util.Collections.singleton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.Triple;
import org.callimachusproject.engine.model.CURIE;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.Term;
import org.callimachusproject.engine.model.VarOrTerm;

/**
 * Conerts an RDF document of triples into an RDF/XML document.
 * 
 * @author James Leigh
 *
 */
public class RDFXMLEventReader extends XMLEventReaderBase {
	private static final String NCNameChar = "a-zA-Z0-9\\-\\._"
			+ "\\u00C0-\\u00D6\\u00D8-\\u00F6\\u00F8-\\u02FF\\u0370-\\u037D\\u037F-\\u1FFF\\u200C-\\u200D\\u2070-\\u218F\\u2C00-\\u2FEF\\u3001-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFFD"
			+ "\\u00B7\\u0300-\\u036F\\u203F-\\u2040";
	private static final Pattern LOCAL_PART = Pattern.compile("[" + NCNameChar
			+ "]+$");
	private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String XML = "http://www.w3.org/XML/1998/namespace";
	private static final QName ROOT = new QName(RDF, "RDF", "rdf");
	private static final QName DESC = new QName(RDF, "Description", "rdf");
	private static final QName ABOUT = new QName(RDF, "about", "rdf");
	private static final QName NODEID = new QName(RDF, "nodeID", "rdf");
	private static final QName RESOURCE = new QName(RDF, "resource", "rdf");
	private static final QName PARSETYPE = new QName(RDF, "parseType", "rdf");
	private static final QName DATATYPE = new QName(RDF, "datatype", "rdf");
	private static final QName LANG = new QName(XML, "lang", "xml");
	private static final QName BASE = new QName(XML, "base", "xml");
	private String comment;
	private RDFEventReader reader;
	private XMLEventFactory factory = XMLEventFactory.newInstance();
	private Attribute base;
	private Map<String, Namespace> namespaces = new HashMap<String, Namespace>();
	private boolean started;
	private VarOrTerm subject;

	public RDFXMLEventReader(RDFEventReader reader) {
		this(null, reader);
	}

	public RDFXMLEventReader(String comment, RDFEventReader reader) {
		this.comment = comment;
		this.reader = reader;
		namespaces.put("rdf", factory.createNamespace("rdf", RDF));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + reader.toString();
	}

	@Override
	public void close() throws XMLStreamException {
		try {
			reader.close();
		} catch (RDFParseException e) {
			throw new XMLStreamException(e);
		}
	}

	@Override
	protected boolean more() throws XMLStreamException {
		try {
			while (reader.hasNext()) {
				RDFEvent event = reader.next();
				if (process(event))
					return true;
			}
			return false;
		} catch (RDFParseException e) {
			throw new XMLStreamException(e);
		}
	}

	private boolean process(RDFEvent event) throws XMLStreamException {
		if (event.isStartDocument()) {
			add(factory.createStartDocument());
			return true;
		} else if (event.isEndDocument()) {
			if (subject != null) {
				endSubject(subject);
			}
			startDocument();
			add(factory.createEndElement(ROOT, null));
			if (comment != null) {
				add(factory.createComment(comment));
			}
			add(factory.createEndDocument());
			return true;
		} else if (event.isBase()) {
			base = factory.createAttribute(BASE, event.asBase().getBase());
			return false;
		} else if (event.isNamespace()) {
			String p = event.asNamespace().getPrefix();
			String n = event.asNamespace().getNamespaceURI();
			Namespace ns = factory.createNamespace(p, n);
			namespaces.put(ns.getPrefix(), ns);
			return false;
		} else if (event.isStartSubject()) {
			startSubject(event.asSubject().getSubject());
			return true;
		} else if (event.isEndSubject()) {
			endSubject(event.asSubject().getSubject());
			return true;
		} else if (event.isTriple()) {
			processTriple(event.asTriple());
			return true;
		} else {
			return false;
		}
	}

	private void processTriple(Triple event) throws XMLStreamException {
		if (!event.getSubject().equals(subject)) {
			startSubject(event.getSubject());
		}
		QName pred = asQName(event.getPredicate());
		List<Namespace> ns = new ArrayList<Namespace>();
		if (pred.getPrefix().length() == 0) {
			ns.add(factory.createNamespace(pred.getNamespaceURI()));
		}
		List<Attribute> attrs = new ArrayList<Attribute>();
		Term obj = event.getObject();
		if (obj.isXMLLiteral()) {
			attrs.add(factory.createAttribute(PARSETYPE, "Literal"));
			add(factory.createStartElement(pred, attrs.iterator(), ns.iterator()));
			XMLEventReader reader = obj.asXMLLiteral().openXMLEventReader();
			try {
				while (reader.hasNext()) {
					add(reader.nextEvent());
				}
			} finally {
				reader.close();
			}
			add(factory.createEndElement(pred, ns.iterator()));
		} else if (obj.isLiteral()) {
			IRI dt = obj.asLiteral().getDatatype();
			attrs.add(factory.createAttribute(DATATYPE, dt.stringValue()));
			add(factory.createStartElement(pred, attrs.iterator(), ns.iterator()));
			add(factory.createCharacters(obj.stringValue()));
			add(factory.createEndElement(pred, ns.iterator()));
		} else if (obj.isPlainLiteral()) {
			String lang = obj.asPlainLiteral().getLang();
			if (lang != null) {
				attrs.add(factory.createAttribute(LANG, lang));
			}
			add(factory.createStartElement(pred, attrs.iterator(), ns.iterator()));
			add(factory.createCharacters(obj.stringValue()));
			add(factory.createEndElement(pred, ns.iterator()));
		} else if (obj.isIRI()) {
			attrs.add(factory.createAttribute(RESOURCE, obj.stringValue()));
			add(factory.createStartElement(pred, attrs.iterator(), ns.iterator()));
			add(factory.createEndElement(pred, ns.iterator()));
		} else {
			attrs.add(factory.createAttribute(NODEID, obj.stringValue()));
			add(factory.createStartElement(pred, attrs.iterator(), ns.iterator()));
			add(factory.createEndElement(pred, ns.iterator()));
		}
	}

	private void startSubject(VarOrTerm subj) {
		startDocument();
		if (subject != null) {
			endSubject(subject);
		}
		QName name = subj.isIRI() ? ABOUT : NODEID;
		Attribute attr = factory.createAttribute(name, subj.stringValue());
		Iterator<Attribute> attrs = singleton(attr).iterator();
		add(factory.createStartElement(DESC, attrs, null));
		subject = subj;
	}

	private void startDocument() {
		if (!started) {
			started = true;
			Iterator<Namespace> n = namespaces.values().iterator();
			Iterator<Attribute> attr = singleton(base).iterator();
			Iterator<Attribute> b = base == null ? null : attr;
			add(factory.createStartElement(ROOT, b, n));
		}
	}

	private void endSubject(VarOrTerm subj) {
		if (subject != null && subject.equals(subj)) {
			add(factory.createEndElement(DESC, null));
			subject = null;
		}
	}

	private QName asQName(IRI pred) throws XMLStreamException {
		if (pred.isCURIE()) {
			CURIE curie = pred.asCURIE();
			return new QName(curie.getNamespaceURI(), curie.getReference(),
					curie.getPrefix());
		} else {
			String iri = pred.stringValue();
			int idx = getLocalNameIndex(iri);
			return new QName(iri.substring(0, idx), iri.substring(idx));
		}
	}

	private int getLocalNameIndex(String uri) throws XMLStreamException {
		Matcher matcher = LOCAL_PART.matcher(uri);
		if (matcher.find())
			return matcher.start();
		throw new XMLStreamException("No separator character found in IRI: "
				+ uri);
	}

}
