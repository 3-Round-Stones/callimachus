package org.callimachusproject.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.callimachusproject.expressions.ExpressionFactory;
import org.callimachusproject.rdfa.RDFParseException;


public class TemplateReader extends XMLEventReaderBase {
	private final XMLEventReader xslt;
	private int depth = 0;
	private int hide = Integer.MAX_VALUE;
	private XMLEventFactory ef = XMLEventFactory.newInstance();
	private ExpressionFactory expr = new ExpressionFactory();

	public TemplateReader(XMLEventReader xslt) {
		this.xslt = xslt;
	}

	protected boolean more() throws XMLStreamException {
		boolean added = false;
		while (!added && xslt.hasNext()) {
			XMLEvent next = xslt.nextEvent();
			if (next.isStartElement()) {
				depth++;
				if (depth > 2 && depth < hide && isStartResource(next.asStartElement())) {
					hide = depth;
				}
				Iterator<Namespace> iter = next.asStartElement().getNamespaces();
				while (iter.hasNext()) {
					Namespace ns = iter.next();
					expr.setNamespace(ns.getPrefix(), ns.getValue());
				}
			}
			if (depth < hide) {
				try {
					add(removeExpressions(next));
					added = true;
				} catch (RDFParseException e) {
					throw new XMLStreamException(e);
				}
			}
			if (next.isEndElement()) {
				if (depth == hide) {
					hide = Integer.MAX_VALUE;
				}
				depth--;
			}
		}
		return added;
	}

	/**
	 * TODO evaluate and populate expressions (don't strip them)
	 * @throws RDFParseException 
	 */
	private XMLEvent removeExpressions(XMLEvent next) throws RDFParseException {
		if (next.isCharacters()) {
			String data = next.asCharacters().getData();
			String stripped = removeExpressions(data);
			if (data != stripped) {
				return ef.createCharacters(stripped);
			}
		} else if (next.isStartElement()) {
			boolean found = false;
			List<Attribute> list = new ArrayList<Attribute>();
			StartElement start = next.asStartElement();
			Iterator<Attribute> iter = start.getAttributes();
			while (iter.hasNext()) {
				Attribute attr = iter.next();
				String data = attr.getValue();
				String stripped = removeExpressions(data);
				if (data != stripped) {
					list.add(ef.createAttribute(attr.getName(), stripped));
					found = true;
				} else {
					list.add(attr);
				}
			}
			if (found) {
				Iterator<Attribute> at = list.iterator();
				Iterator<?> ns = start.getNamespaces();
				return ef.createStartElement(start.getName(), at, ns);
			}
		}
		return next;
	}

	private String removeExpressions(String data) throws RDFParseException {
		return expr.parse(data).getTemplate();
	}

	/**
	 * Strip out nested resource, but don't remove create form resources with
	 * typeof attribute. TODO only strip out variable if not bound
	 */
	private boolean isStartResource(StartElement element) {
		String about = attr(element, "about");
		String resource = attr(element, "resource");
		String property = attr(element, "property");
		String content = attr(element, "content");
		if (about != null && about.startsWith("?"))
			return true;
		if (resource != null && resource.startsWith("?"))
			return true;
		if (content != null && content.startsWith("?"))
			return true;
		if (property != null && content == null)
			return true;
		return false;
	}

	private String attr(StartElement element, String attr) {
		Attribute a = element.getAttributeByName(new QName(attr));
		if (a == null)
			return null;
		return a.getValue();
	}

	public void close() throws XMLStreamException {
		xslt.close();
	}
}