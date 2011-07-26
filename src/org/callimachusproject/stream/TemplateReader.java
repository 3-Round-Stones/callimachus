package org.callimachusproject.stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;


public class TemplateReader extends XMLEventReaderBase {
	private final XMLEventReader xslt;
	private int depth = 0;
	private int hide = Integer.MAX_VALUE;
	private XMLEventFactory ef = XMLEventFactory.newInstance();

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
			}
			if (depth < hide) {
				add(removeExpressions(next));
				added = true;
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
	 */
	private XMLEvent removeExpressions(XMLEvent next) {
		if (next.isCharacters()) {
			String data = next.asCharacters().getData();
			if (data.contains("{")) {
				data = data.replaceAll("\\{[^\\{]*\\}", "");
				return ef.createCharacters(data);
			}
		} else if (next.isStartElement()) {
			boolean found = false;
			List<Attribute> list = new ArrayList<Attribute>();
			StartElement start = next.asStartElement();
			Iterator<Attribute> iter = start.getAttributes();
			while (iter.hasNext()) {
				Attribute attr = iter.next();
				String data = attr.getValue();
				if (data.contains("{")) {
					data = data.replaceAll("\\{[^\\{]*\\}", "");
					list.add(ef.createAttribute(attr.getName(), data));
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

	/**
	 * TODO only strip out variable if not bound
	 */
	private boolean isStartResource(StartElement element) {
		String about = attr(element, "about");
		String typeof = attr(element, "typeof");
		String resource = attr(element, "resource");
		String property = attr(element, "property");
		String content = attr(element, "content");
		if (about != null && about.startsWith("?"))
			return true;
		if (resource != null && resource.startsWith("?"))
			return true;
		if (typeof != null && about == null)
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