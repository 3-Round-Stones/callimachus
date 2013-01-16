/*
   Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some
   Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.engine;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.stream.Location;
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

import org.callimachusproject.engine.events.Base;
import org.callimachusproject.engine.events.Document;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.Subject;
import org.callimachusproject.engine.events.Triple;
import org.callimachusproject.engine.expressions.ExpressionUtil;
import org.callimachusproject.engine.helpers.AbstractRDFEventReader;
import org.callimachusproject.engine.helpers.NamespaceStartElement;
import org.callimachusproject.engine.impl.BlankNode;
import org.callimachusproject.engine.impl.FallbackLocation;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.Literal;
import org.callimachusproject.engine.model.Node;
import org.callimachusproject.engine.model.PlainLiteral;
import org.callimachusproject.engine.model.Reference;
import org.callimachusproject.engine.model.TermOrigin;

/**
 * RDFa parser.
 * 
 * @author James Leigh
 * @author Steve Battle
 * 
 */
public class RDFaReader extends AbstractRDFEventReader {
	private static final String XML = "http://www.w3.org/XML/1998/namespace";
	private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String XHV = "http://www.w3.org/1999/xhtml/vocab#";
	private static final String XHTML = "http://www.w3.org/1999/xhtml";
	private final XMLEventReader reader;
	private final String systemId;
	private Base base;
	private Reference document;
	private Tag tag;
	private final LinkedList<XMLEvent> peek = new LinkedList<XMLEvent>();
	private Queue<RDFEvent> queue = new LinkedList<RDFEvent>();
	private int number;
	private int contentDepth = -1;
	private StringWriter content;
	private Location contentLocation;
	private XMLEventWriter writer;
	private XMLOutputFactory factory = XMLOutputFactory.newInstance();
	private XMLEventFactory whites = XMLEventFactory.newInstance();
	private AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
	private IRI XMLLITERAL = tf.iri(RDF + "XMLLiteral");
	private List<Node> TYPE = Arrays.asList((Node) tf.iri(RDF + "type"));
	
	// The element stack keeps track of the origin of the node
	private Stack<Integer> elementStack = new Stack<Integer>();
	private int elementIndex = 1;
	

	public RDFaReader(String base, XMLEventReader reader, String systemId) {
		try {
			assert reader.peek().getLocation().getCharacterOffset() >= 0;
		} catch (XMLStreamException e) {
			throw new AssertionError(e);
		}
		this.reader = reader;
		this.systemId = systemId;
		this.base = new Base(systemId);
		if (base != null) {
			this.base = new Base(this.base.resolve(base), null);
		}
		this.document = tf.reference(this.base.getBase(), this.base.getReference());
	}
	
	/* return the elementStack as a string that represents a template path */
	
	public TermOrigin origin(Location location) {
		TermOrigin b = new TermOrigin(location);
		for(Iterator<Integer> i=elementStack.iterator(); i.hasNext();) {
			b = b.slash(i.next());
		}
		return b;
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
			throw new RDFParseException(e.getMessage() + " in " + systemId, e);
		}
	}

	protected RDFEvent take() throws RDFParseException {
		try {
			while (queue.isEmpty()) {
				while (peek.size() < 2 && reader.hasNext()) {
					while (reader.hasNext()) {
						XMLEvent next = reader.nextEvent();
						assert next.getLocation().getCharacterOffset() >= 0 || next.isEndDocument() : next;
						peek.add(next);
						if (!next.isCharacters())
							break;
					}
				}
				if (peek.isEmpty())
					return null;
				XMLEvent event = peek.remove();
				if (!peek.isEmpty() && event.isStartElement() && peek.getLast().isEndElement()) {
					StringBuilder sb = new StringBuilder();
					for (int i=0; peek.get(i).isCharacters(); i++) {
						sb.append(peek.get(i).asCharacters().getData());
					}
					process(event, sb);
				} else {
					process(event, null);
				}
			}
			return queue.remove();
		} catch (XMLStreamException e) {
			throw new RDFParseException(e.getMessage() + " in " + systemId, e);
		} catch (IOException e) {
			throw new RDFParseException(e.getMessage() + " in " + systemId, e);
		}
	}

	private void process(XMLEvent event, CharSequence characterData) throws XMLStreamException,
			IOException, RDFParseException {
		assert event.getLocation().getCharacterOffset() >= 0 || event.isEndDocument();
		if (event.isStartDocument()) {
			processStartDocument(event);
		} else if (event.isEndDocument()) {
			processEndDocument((EndDocument) event);
		} else if (event.isStartElement()) {
			elementStack.push(elementIndex);
			elementIndex=1;
			processStartElement(event.asStartElement(), characterData);
		} else if (event.isEndElement()) {
			processEndElement(event.asEndElement());
			if (!elementStack.isEmpty()) elementIndex = elementStack.pop();
			elementIndex++;
		} else if (contentDepth >= 0) {
			contentLocation = event.getLocation();
			if (writer == null && event.isCharacters()) {
				content.append(event.asCharacters().getData());
			} else if (event.isCharacters()) {
				writer.add(event);
			} else if (!tag.isTextContent()) {
				getContentWriter().add(event);
			}
		}
		// process property expressions embedded in character data
		if (event.isCharacters() && tag!=null) {
			Node subj = tag.getResource();
			if (subj==null) subj = tag.getCurrentSubject();
			tag.addPropertyExpressions(subj,event.asCharacters().getData(), event.getLocation());
		}
	}

	private void processStartDocument(XMLEvent event) {
		queue.add(new Document(true, event.getLocation()));
	}

	private void processEndDocument(EndDocument event) {
		queue.add(new Document(false, event.getLocation()));
	}

	private void processStartElement(StartElement event, CharSequence characterData)
			throws XMLStreamException, RDFParseException {
		if (contentDepth >= 0) {

			contentDepth++;
			if (!tag.isTextContent()) {
				XMLEventWriter writer = getContentWriter();
				if (contentDepth == 1) {
					Map<String, Namespace> map = tag.getNamespaceMap();
					Iterator<?> iter = event.getNamespaces();
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
			tag = new Tag(tag, event, characterData, ++number);
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
			contentLocation = event.getLocation();
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
			if (contentLocation == null) {
				contentLocation = event.getLocation();
			}
			tag.setContent(content.toString(), contentLocation);
			content = null;
			writer = null;
			tag.end(event);
			tag = tag.getParent();
		} else {
			tag.end(event);
			tag = tag.getParent();
		}
	}

	private Base base(String base, Location location) {
		String uri = resolve(base);
		this.base = new Base(uri, location);
		this.document = tf.reference(base, this.base.getReference());
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
		private Location contentLocation;
		private Node datatype;
		private boolean startedSubject;
		TermOrigin origin;
		// record variable nodes as they are declared
		// the same variable may be re-used within a descendant tag
		HashMap<String,Node> vars = new HashMap<String,Node>();

		public Tag(Tag parent, StartElement event, CharSequence characterData, int number) throws RDFParseException {
			this.parent = parent;
			this.event = event;
			this.empty = ExpressionUtil.isEmpty(characterData, event.getNamespaceContext(), event.getLocation());
			this.number = number;
			this.origin = origin(event.getLocation());
			if (parent!=null) vars.putAll(parent.vars);
		}

		public String toString() {
			return event.toString();
		}
		
		Node addVar(Node node) {
			if (node!=null && node.isReference()) {
				String url = node.asReference().stringValue();
				int n = url.indexOf("?");
				if (n>=0 && url.startsWith(base.getBase()+"?")) 
					vars.put(url.substring(n), node);
			}
			return node;
		}
		
		public Tag getParent() {
			return parent;
		}
		
//		public String getContent() {
//			String attr = attr("content");
//			if (attr != null)
//				return attr;
//			if (content != null)
//				return content;
//			return "";
//		}

		public Attribute getAttributeContent() {
			Attribute a = event.getAttributeByName(new QName("content"));
			if (a == null)
				return null;
			return a;
		}

		public void setContent(String content, Location location) {
			assert location != null;
			this.content = content;
			this.contentLocation = location;
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
			Iterator<?> iter = event.getNamespaces();
			while (iter.hasNext()) {
				Namespace ns = (Namespace) iter.next();
				if (ns.getPrefix().equals(prefix))
					return resolve(ns.getNamespaceURI());
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
			Iterator<?> iter = event.getNamespaces();
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
			String attr = attr("about");
			if (vars.containsKey(attr)) return vars.get(attr);
			Node about = uriOrSafeCURIE(attr);
			if (about != null) return addVar(about);

			attr = attr("src");
			if (vars.containsKey(attr)) return vars.get(attr);
			Node src = uriOrSafeCURIE(attr);
			if (src != null) return addVar(src);
			
			if (attr("rel") == null && attr("rev") == null) {
				
				attr = attr("resource");
				if (vars.containsKey(attr)) return vars.get(attr);
				Node resource = uriOrSafeCURIE(attr);
				if (resource != null) return addVar(resource);
				
				attr = attr("href");
				if (vars.containsKey(attr)) return vars.get(attr);
				Node href = uriOrSafeCURIE(attr);
				if (href != null) return addVar(href);
				
			}
			if (isHTML(event.getName())) {
				String tn = event.getName().getLocalPart();
				if ("head".equalsIgnoreCase(tn) || "body".equalsIgnoreCase(tn))
					return document;
			}
			
			// typeof (including empty typeof) introduces a blank node
			String typeof = attr("typeof");
			if (typeof!=null)
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
			String attr = attr("resource");
			if (vars.containsKey(attr)) return vars.get(attr);
			Node resource = uriOrSafeCURIE(attr);
			if (resource != null) return addVar(resource);

			attr = attr("href");
			if (vars.containsKey(attr)) return vars.get(attr);
			Node href = uriOrSafeCURIE(attr);
			if (href != null) return addVar(href);

			if (getRel() != null || getRev() != null) {
				return getNextBlankNode();
			}
			return null;
		}

		public BlankNode getBlankNode() {
			BlankNode b = new BlankNode("n" + number);
			b.setOrigin(origin.blank());
			return b;
		}

		public BlankNode getNextBlankNode() {
			BlankNode b = new BlankNode("n" + (number + 1));
			b.setOrigin(origin);
			return b;
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
			Node subj = getCurrentSubject();
			// add property expressions in attributes of this event
			addPropertyExpressions(subj, event);
			// add properties with @content
			addAttributeContent(subj);
			
			if (isTriplePresent() || (parent != null && parent.isHanging())) {
				if (!isStartSubject()) {
					chain();
				}
				// subj is null if this tag doesn't introduce a subject
				Node newSubj = getNewSubject();
				if (parent != null && parent.isHanging() && newSubj!=null) {
					List<Node> rel = parent.getRel();
					if (rel != null) {
						triple(parent.getCurrentSubject(), rel, newSubj, false);
					}
					List<Node> rev = parent.getRev();
					if (rev != null) {
						triple(newSubj, rev, parent.getCurrentSubject(), true);
					}
				}
				// open new subject
				if (isStartSubject()) {
					chain();
				}
			}
			if ("base".equals(event.getName().getLocalPart())) {
				String href = attr("href");
				if (href != null) {
					queue.add(base(href, getLocation()));
				}
			}
			Iterator<?> nter = event.getNamespaces();
			while (nter.hasNext()) {
				Namespace ns = (Namespace) nter.next();
				String uri = ns.getNamespaceURI();
				if (uri != null) {
					queue.add(new org.callimachusproject.engine.events.Namespace(
							ns.getPrefix(), uri, getLocation()));
				}
			}

			List<Node> typeof = curies(attr("typeof"));
			if (typeof != null) {
				for (Node t : typeof) {
					triple(subj, TYPE, t, false);
				}
			}

			Node newSubject = getNewSubject();
			List<Node> rel = getRel();
			List<Node> rev = getRev();
			if (parent != null && newSubject != null && isHanging() && empty) {
				if (rel != null) {
					triple(parent.getCurrentSubject(), rel, newSubject, false);
				}
				if (rev != null) {
					triple(newSubject, rev, parent.getCurrentSubject(), true);
				}
			} else if (isHanging() && empty) {
				Node obj = getBlankNode();
				if (rel != null) {
					triple(subj, rel, obj, false);
				}
				if (rev != null) {
					triple(obj, rev, subj, true);
				}
			} else if (!isHanging()) {
				if (rel != null) {
					triple(subj, rel, getResource(), false);
				}
				if (rev != null) {
					triple(getResource(), rev, subj, true);
				}
				if (isStartResource()) {
					queue.add(new Subject(true, getResource(), getLocation()));
				}
			}
		}
		
		void addPropertyExpressions(Node subj, CharSequence value, Location location) throws RDFParseException {
			if (subj==null) return;
			List<RDFEvent> list = ExpressionUtil.pattern(tf, subj, value, origin, location, event.getNamespaceContext());
			for (RDFEvent triple : list) {
				if (isHanging()) {
					List<Node> rel = getRel();
					List<Node> rev = getRev();
					if (rel != null)
						triple(getCurrentSubject(), rel, subj, false);
					if (rev != null)
						triple(subj, rev, getCurrentSubject(), true);
					queue.add(new Subject(true, subj, location));
				}
				else if (parent.isHanging()) {
					List<Node> rel = parent.getRel();
					List<Node> rev = parent.getRev();
					if (rel != null)
						triple(parent.getCurrentSubject(), rel, subj, false);
					if (rev != null)
						triple(subj, rev, parent.getCurrentSubject(), true);
					queue.add(new Subject(true, subj, location));
				}
				
				queue.add(triple);
				
				if (isHanging() || parent.isHanging()) {
					queue.add(new Subject(false, subj, location));
				}
			}
				
		}

		private void addPropertyExpressions(Node subj, StartElement start) 
		throws RDFParseException {
			Iterator<?> i = start.getAttributes();
			while (i.hasNext()) {
				Attribute a = (Attribute) i.next();
				String value = a.getValue();
				addPropertyExpressions(subj, value, getLocation(a));
			}
		}

		
		
		/* Add @content on start tag */
		
		private void addAttributeContent(Node subj) throws RDFParseException {
			String property = attr("property");
			Attribute content = getAttributeContent();
			if (property != null && content != null) {
				Node datatype = getDatatype();
				List<Node> pred = curies(property);
				if (pred != null && datatype == null) {
					String lang = getLang();
					plain(subj, pred, content.getValue(), getLocation(content), lang);
				} else if (pred != null) {
					typed(subj, pred, content.getValue(), getLocation(content), (IRI) datatype);
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
					queue.add(new Subject(true, getCurrentSubject(), getLocation()));
				}
			}
		}

		public void end(EndElement event) throws RDFParseException {
			if (isStartResource()) {
				queue.add(new Subject(false, getResource(), getLocation()));
			}
			Node subj = getCurrentSubject();
			String property = attr("property");
			
			// add properties with content defined in the text on end tag
			//String content = getContent();
			if (property != null && content != null) {
				Node datatype = getDatatype();
				List<Node> pred = curies(property);
				if (pred != null && datatype == null) {
					String lang = getLang();
					plain(subj, pred, content, contentLocation, lang);
				} else if (pred != null) {
					typed(subj, pred, content, contentLocation, (IRI) datatype);
				}
			}
			if (startedSubject) {
				queue.add(new Subject(false, subj, getLocation()));
			}
		}

		private Location getLocation() {
			return event.getLocation();
		}

		private Location getLocation(Attribute attr) {
			return new FallbackLocation(attr, event);
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
			// braces are not safe characters in URIs and are used to define template expressions
			if (value == null || value.indexOf('{')>=0 )
				return null;
			if (value.equals("[]")) {
				return this.getParent().getNewSubject();
			}
			if (!value.startsWith("[")) {
				Reference r = tf.reference(resolve(value), value);
				r.setOrigin(origin);
				return r;
			}
			Node c = curie(value.substring(1, value.length() - 1));
			c.setOrigin(origin);
			return c;
		}

		private List<Node> curies(String value) throws RDFParseException {
			if (value == null)
				return null;
			List<Node> list = new ArrayList<Node>();
			for (String v : value.split("\\s+")) {
				if (v.length() > 0) {
					Node node = curie(v);
					if (node != null) {
						list.add(node);
					}
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
			if (idx < 0)
				return null;
			String prefix = value.substring(0, idx);
			// this may not be a curie
			if (value.contains("://")) {
				return tf.iri(value);
			}
			String namespaceURI = getNamespaceURI(prefix);
			if (namespaceURI == null)
				throw new RDFParseException("Undefined Prefix: " + prefix);
			if (XHTML.equals(namespaceURI)) {
				namespaceURI = XHV;
			}
			String reference = value.substring(idx + 1);
			Node c = tf.curie(namespaceURI, reference, prefix);
			c.setOrigin(origin);
			return c;
		}

		private void triple(Node subj, List<Node> pred, Node obj, boolean inverse) {
			for (Node p : pred) {
				queue.add(new Triple(subj, (IRI) p, obj, inverse, getLocation()));
			}
		}

		private void plain(Node subj, List<Node> pred, String content,
				Location location, String lang) {
			PlainLiteral lit = tf.literal(content, lang);
			//lit.setOrigin(origin+" "+TEXT_CONTENT);
			if (content.isEmpty()) {
				lit.setOrigin(origin.textContent(location));
			} else {
				lit.setOrigin(origin);
			}
			for (Node p : pred) {
				queue.add(new Triple(subj, (IRI) p, lit, getLocation()));
			}
		}

		private void typed(Node subj, List<Node> pred, String content,
				Location location, IRI datatype) {
			Literal lit = tf.literal(content, datatype);
			//lit.setOrigin(origin+" "+TEXT_CONTENT);
			if (content.isEmpty()) {
				lit.setOrigin(origin.textContent(location));
			} else {
				lit.setOrigin(origin);
			}
			for (Node p : pred) {
				queue.add(new Triple(subj, (IRI) p, lit, getLocation()));
			}
		}

	}
}
