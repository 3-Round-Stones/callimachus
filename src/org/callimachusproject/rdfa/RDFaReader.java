/*
   Copyright 2009 Zepheira LLC

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
package org.callimachusproject.rdfa;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.callimachusproject.rdfa.events.Base;
import org.callimachusproject.rdfa.events.Document;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Subject;
import org.callimachusproject.rdfa.events.Triple;
import org.callimachusproject.rdfa.impl.BlankNode;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.Node;
import org.callimachusproject.rdfa.model.PlainLiteral;
import org.callimachusproject.rdfa.model.Reference;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.TypedLiteral;
import org.callimachusproject.stream.NamespaceStartElement;

public class RDFaReader extends RDFEventReader {
	private static final String XML = "http://www.w3.org/XML/1998/namespace";
	private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String XHV = "http://www.w3.org/1999/xhtml/vocab#";
	private static final String XHTML = "http://www.w3.org/1999/xhtml";
	private final static Set<String> reserved = new HashSet<String>(Arrays
			.asList("alternate", "appendix", "bookmark", "cite", "chapter",
					"contents", "copyright", "first", "glossary", "help",
					"icon", "index", "last", "license", "meta", "next",
					"p3pv1", "prev", "collection", "role", "section",
					"stylesheet", "subsection", "start", "top", "up"));
	private final XMLEventReader reader;
	private Base base;
	private Reference document;
	private Tag tag;
	private Queue<RDFEvent> queue = new LinkedList<RDFEvent>();
	private int number;
	private int contentDepth = -1;
	private StringWriter content;
	private XMLEventWriter writer;
	private XMLOutputFactory factory = XMLOutputFactory.newInstance();
	private XMLEventFactory whites = XMLEventFactory.newInstance();
	private TermFactory tf = TermFactory.newInstance();
	private IRI XMLLITERAL = tf.iri(RDF + "XMLLiteral");
	private List<Node> TYPE = Arrays.asList((Node) tf.iri(RDF + "type"));

	public RDFaReader(String base, XMLEventReader reader) {
		this.reader = reader;
		this.base = new Base(base);
		this.document = tf.reference(base, "");
	}

	public String toString() {
		if (base != null)
			return getClass().getSimpleName() + " " + base.getBase();
		return getClass().getSimpleName() + " " + reader.toString();
	}

	public void close() throws RDFParseException {
		try {
			reader.close();
		} catch (XMLStreamException e) {
			throw new RDFParseException(e);
		}
	}

	protected RDFEvent take() throws RDFParseException {
		try {
			while (queue.isEmpty()) {
				if (!reader.hasNext())
					return null;
				process(reader.nextEvent());
			}
			return queue.remove();
		} catch (XMLStreamException e) {
			throw new RDFParseException(e);
		} catch (IOException e) {
			throw new RDFParseException(e);
		}
	}

	private void process(XMLEvent event) throws XMLStreamException,
			IOException, RDFParseException {
		if (event.isStartDocument()) {
			processStartDocument(event);
		} else if (event.isEndDocument()) {
			processEndDocument((EndDocument) event);
		} else if (event.isStartElement()) {
			processStartElement(event.asStartElement());
		} else if (event.isEndElement()) {
			processEndElement(event.asEndElement());
		} else if (contentDepth >= 0) {
			if (writer == null && event.isCharacters()) {
				content.append(event.asCharacters().getData());
			} else if (event.isCharacters()) {
				writer.add(event);
			} else if (!tag.isTextContent()) {
				getContentWriter().add(event);
			}
		}
	}

	private void processStartDocument(XMLEvent event) {
		queue.add(new Document(true));
	}

	private void processEndDocument(EndDocument event) {
		queue.add(new Document(false));
	}

	private void processStartElement(StartElement event)
			throws XMLStreamException, RDFParseException {
		if (contentDepth >= 0) {
			contentDepth++;
			if (!tag.isTextContent()) {
				XMLEventWriter writer = getContentWriter();
				if (contentDepth == 1) {
					Map<String, Namespace> map = tag.getNamespaceMap();
					Iterator iter = event.getNamespaces();
					while (iter.hasNext()) {
						Namespace ns = (Namespace) iter.next();
						map.put(ns.getPrefix(), ns);
					}
					writer.add(new NamespaceStartElement(event, map));
				} else {
					writer.add(event);
				}
			}
		} else {
			tag = new Tag(tag, event, reader.peek().isEndElement(), ++number);
			tag.started();
			if (tag.isContent()) {
				contentDepth = 0;
				content = new StringWriter();
				if (tag.isXMLContent()) {
					writer = factory.createXMLEventWriter(content);
				}
			}
		}
	}

	private XMLEventWriter getContentWriter() throws XMLStreamException {
		if (writer == null) {
			String text = content.toString();
			content = new StringWriter();
			writer = factory.createXMLEventWriter(content);
			if (text.length() > 0) {
				writer.add(whites.createCharacters(text));
			}
		}
		return writer;
	}

	private void processEndElement(EndElement event) throws XMLStreamException,
			RDFParseException {
		if (contentDepth > 0) {
			if (!tag.isTextContent()) {
				writer.add(event);
			}
			contentDepth--;
		} else if (contentDepth == 0) {
			contentDepth = -1;
			if (writer != null) {
				writer.close();
				tag.setDatatype(XMLLITERAL);
			}
			tag.setContent(content.toString());
			content = null;
			writer = null;
			tag.end(event);
			tag = tag.getParent();
		} else {
			tag.end(event);
			tag = tag.getParent();
		}
	}

	private Base base(String base) {
		String uri = resolve(base);
		this.base = new Base(uri);
		this.document = tf.reference(base, "");
		return this.base;
	}

	private String resolve(String relative) {
		return base.resolve(relative);
	}

	private class Tag {
		private Tag parent;
		private StartElement event;
		private int number;
		private boolean empty;
		private String content;
		private Node datatype;
		private boolean startedSubject;
		private Node list;

		public Tag(Tag parent, StartElement event, boolean empty, int number) {
			this.parent = parent;
			this.event = event;
			this.empty = empty;
			this.number = number;
		}

		public String toString() {
			return event.toString();
		}

		public Tag getParent() {
			return parent;
		}

		public String getContent() {
			String attr = attr("content");
			if (attr != null)
				return attr;
			if (content != null)
				return content;
			return "";
		}

		public void setContent(String content) {
			this.content = content;
		}

		public Node getDatatype() throws RDFParseException {
			if (datatype == null)
				return curie(attr("datatype"));
			return datatype;
		}

		public void setDatatype(IRI datatype) {
			this.datatype = datatype;
		}

		public String getNamespaceURI(String prefix) {
			Iterator iter = event.getNamespaces();
			while (iter.hasNext()) {
				Namespace ns = (Namespace) iter.next();
				if (ns.getPrefix().equals(prefix))
					return ns.getNamespaceURI();
			}
			if (parent == null && "".equals(prefix))
				return "http://www.w3.org/1999/xhtml/vocab#";
			if (parent == null)
				return null;
			return parent.getNamespaceURI(prefix);
		}

		public Map<String, Namespace> getNamespaceMap() {
			Map<String, Namespace> map;
			if (parent == null) {
				map = new HashMap<String, Namespace>();
			} else {
				map = parent.getNamespaceMap();
			}
			Iterator iter = event.getNamespaces();
			while (iter.hasNext()) {
				Namespace ns = (Namespace) iter.next();
				map.put(ns.getPrefix(), ns);
			}
			return map;
		}

		public Node getCurrentSubject() throws RDFParseException {
			Node newSubject = getNewSubject();
			if (newSubject != null)
				return newSubject;
			if (parent == null)
				return document;
			Node resource = parent.getResource();
			if (resource != null)
				return resource;
			return parent.getCurrentSubject();
		}

		public Node getNewSubject() throws RDFParseException {
			Node about = uriOrSafeCURIE(attr("about"));
			if (about != null) {
				return about;
			}
			Node src = uriOrSafeCURIE(attr("src"));
			if (src != null) {
				return src;
			}
			if (attr("rel") == null && attr("rev") == null) {
				Node resource = uriOrSafeCURIE(attr("resource"));
				if (resource != null)
					return resource;
				Node href = uriOrSafeCURIE(attr("href"));
				if (href != null)
					return href;
			}
			if (isHTML(event.getName())) {
				String tn = event.getName().getLocalPart();
				if ("head".equalsIgnoreCase(tn) || "body".equalsIgnoreCase(tn))
					return document;
			}
			if (curies(attr("typeof")) != null)
				return getBlankNode();
			if (parent != null && parent.isHanging()) {
				if (attr("property") != null || attr("rel") != null
						|| attr("rev") != null) {
					return parent.getResource();
				}
			}
			return null;
		}

		public List<Node> getRel() throws RDFParseException {
			return curies(attr("rel"));
		}

		public List<Node> getRev() throws RDFParseException {
			return curies(attr("rev"));
		}

		public Node getResource() throws RDFParseException {
			Node resource = uriOrSafeCURIE(attr("resource"));
			if (resource != null) {
				return resource;
			}
			Node href = uriOrSafeCURIE(attr("href"));
			if (href != null) {
				return href;
			}
			if (getRel() != null || getRev() != null) {
				return getNextBlankNode();
			}
			return null;
		}

		public BlankNode getBlankNode() {
			return new BlankNode("n" + number);
		}

		public BlankNode getNextBlankNode() {
			return new BlankNode("n" + (number + 1));
		}

		public boolean isStartSubject() throws RDFParseException {
			if (parent != null && parent.isHanging())
				return true;
			Node subj = getNewSubject();
			if (subj == null)
				return false;
			if (parent == null)
				return true;
			return !subj.equals(parent.getResource());
		}

		public boolean isStartResource() throws RDFParseException {
			if (empty)
				return false;
			if (isHanging())
				return false;
			return getRel() != null || getRev() != null;
		}

		public boolean isHanging() throws RDFParseException {
			if (attr("resource") != null || attr("href") != null)
				return false;
			return getRel() != null || getRev() != null;
		}

		public boolean isTriplePresent() throws RDFParseException {
			List<Node> typeof = curies(attr("typeof"));
			if (typeof != null)
				return true;
			return curies(attr("property")) != null || getRel() != null
					|| getRev() != null;
		}

		public boolean isXMLContent() throws RDFParseException {
			Node datatype = curie(attr("datatype"));
			return datatype != null && XMLLITERAL.equals(datatype);
		}

		public boolean isTextContent() throws RDFParseException {
			Node datatype = curie(attr("datatype"));
			if (datatype == null || XMLLITERAL.equals(datatype))
				return false;
			return attr("property") != null && attr("content") == null;
		}

		public boolean isContent() {
			return attr("property") != null && attr("content") == null;
		}

		public void started() throws RDFParseException {
			if (isTriplePresent() || (parent != null && parent.isHanging())) {
				if (!isStartSubject()) {
					chain();
				}
				Node subj = getCurrentSubject();
				if (parent != null && parent.isHanging()) {
					List<Node> rel = parent.getRel();
					if (rel != null) {
						triple(parent.getCurrentSubject(), rel, subj);
					}
				}
				if (isStartSubject()) {
					chain();
				}
				if (parent != null && parent.isHanging()) {
					List<Node> rev = parent.getRev();
					if (rev != null) {
						triple(subj, rev, parent.getCurrentSubject());
					}
				}
			}
			if ("base".equals(event.getName().getLocalPart())) {
				String href = attr("href");
				if (href != null) {
					queue.add(base(href));
				}
			}
			Iterator nter = event.getNamespaces();
			while (nter.hasNext()) {
				Namespace ns = (Namespace) nter.next();
				String uri = ns.getNamespaceURI();
				if (uri != null) {
					queue.add(new org.callimachusproject.rdfa.events.Namespace(
							ns.getPrefix(), uri));
				}
			}
			Node subj = getCurrentSubject();
			List<Node> typeof = curies(attr("typeof"));
			if (typeof != null) {
				for (Node t : typeof) {
					triple(subj, TYPE, t);
				}
			}
			Node newSubject = getNewSubject();
			List<Node> rel = getRel();
			List<Node> rev = getRev();
			if (parent != null && newSubject != null && isHanging() && empty) {
				if (rel != null) {
					triple(parent.getCurrentSubject(), rel, newSubject);
				}
				if (rev != null) {
					triple(newSubject, rev, parent.getCurrentSubject());
				}
			} else if (isHanging() && empty) {
				Node obj = getBlankNode();
				if (rel != null) {
					triple(subj, rel, obj);
				}
				if (rev != null) {
					triple(obj, rev, subj);
				}
			} else if (!isHanging()) {
				if (rel != null) {
					triple(subj, rel, getResource());
				}
				if (isStartResource()) {
					queue.add(new Subject(true, getResource()));
				}
				if (rev != null) {
					triple(getResource(), rev, subj);
				}
			}
		}

		public void chain() throws RDFParseException {
			if (parent != null && !parent.isTriplePresent()) {
				parent.chain();
			}
			if (isStartSubject()) {
				if (!startedSubject) {
					startedSubject = true;
					queue.add(new Subject(true, getCurrentSubject()));
				}
			}
		}

		public void end(EndElement event) throws RDFParseException {
			if (isStartResource()) {
				queue.add(new Subject(false, getResource()));
			}
			Node subj = getCurrentSubject();
			String property = attr("property");
			String content = getContent();
			if (property != null && content != null) {
				Node datatype = getDatatype();
				List<Node> pred = curies(property);
				if (pred != null && datatype == null) {
					String lang = getLang();
					plain(subj, pred, content, lang);
				} else if (pred != null) {
					typed(subj, pred, content, (IRI) datatype);
				}
			}
			if (startedSubject) {
				queue.add(new Subject(false, subj));
			}
		}

		private boolean isHTML(QName name) {
			String ns = name.getNamespaceURI();
			if (ns == null)
				return true;
			if ("http://www.w3.org/1999/xhtml".equals(ns))
				return true;
			return false;
		}

		private String getLang() {
			Attribute attr = event.getAttributeByName(new QName(XML, "lang"));
			if (attr != null && attr.getValue().length() > 0)
				return attr.getValue();
			String lang = attr("lang");
			if (lang != null && lang.length() > 0)
				return lang;
			lang = attr("xml:lang");
			if (lang != null && lang.length() > 0)
				return lang;
			if (parent == null)
				return null;
			return parent.getLang();
		}

		private String attr(String attr) {
			Attribute a = event.getAttributeByName(new QName(attr));
			if (a == null)
				return null;
			return a.getValue();
		}

		private Node uriOrSafeCURIE(String value) throws RDFParseException {
			if (value == null)
				return null;
			if (!value.startsWith("["))
				return tf.reference(resolve(value), value);
			return curie(value.substring(1, value.length() - 1));
		}

		private List<Node> curies(String value) throws RDFParseException {
			if (value == null)
				return null;
			List<Node> list = new ArrayList<Node>();
			for (String v : value.split("\\s+")) {
				Node node = curie(v);
				if (node != null) {
					list.add(node);
				}
			}
			if (list.isEmpty())
				return null;
			return list;
		}

		private Node curie(String value) throws RDFParseException {
			if (value == null)
				return null;
			if (value.startsWith("_:") && getNamespaceURI("_") == null)
				return new BlankNode(value.substring(2));
			int idx = value.indexOf(":");
			if (idx < 0 && reserved.contains(value))
				return tf.iri(XHV + value);
			if (idx < 0)
				return null;
			String prefix = value.substring(0, idx);
			String namespaceURI = getNamespaceURI(prefix);
			if (namespaceURI == null)
				throw new RDFParseException("Undefined Prefix: " + prefix);
			if (XHTML.equals(namespaceURI)) {
				namespaceURI = XHV;
			}
			String reference = value.substring(idx + 1);
			return tf.curie(namespaceURI, reference, prefix);
		}

		private void triple(Node subj, List<Node> pred, Node obj) {
			for (Node p : pred) {
				queue.add(new Triple(subj, (IRI) p, obj));
			}
		}

		private void plain(Node subj, List<Node> pred, String content,
				String lang) {
			PlainLiteral lit = tf.literal(content, lang);
			for (Node p : pred) {
				queue.add(new Triple(subj, (IRI) p, lit));
			}
		}

		private void typed(Node subj, List<Node> pred, String content,
				IRI datatype) {
			TypedLiteral lit = tf.literal(content, datatype);
			for (Node p : pred) {
				queue.add(new Triple(subj, (IRI) p, lit));
			}
		}
	}
}
