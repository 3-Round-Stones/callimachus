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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Extracts an element from an XML document given the element's xptr.
 * 
 * @author James Leigh
 * 
 */
public final class XMLElementReader extends XMLEventConverter {
	private static final QName ID = new QName("id");
	private final String id;
	private final List<Integer> path;
	private int idDepth = 0;
	private List<Integer> stack = new LinkedList<Integer>();
	private int position;
	private Map<String, Namespace> namespaces = new HashMap<String, Namespace>();

	public XMLElementReader(XMLEventReader reader, String element) {
		super(reader);
		String[] ar = element.split("/");
		id = ar[0];
		path = new ArrayList<Integer>(ar.length - 1);
		for (int i = 1; i < ar.length; i++) {
			path.add(Integer.valueOf(ar[i]));
		}
	}

	@Override
	public XMLEvent convert(XMLEvent event) throws XMLStreamException {
		if (event.isStartDocument() || event.isEndDocument())
			return event;
		if (event.isStartElement()) {
			stack.add(position + 1);
			position = 0;
			if (id.equals(getIdOf(event.asStartElement()))) {
				idDepth = stack.size();
			}
			if (id.length() > 0 && idDepth == 0 || idDepth == stack.size()
					|| isParentOrCurrent(subList(stack, idDepth))) {
				Iterator<?> nter = event.asStartElement().getNamespaces();
				while (nter.hasNext()) {
					Namespace ns = (Namespace) nter.next();
					namespaces.put(ns.getPrefix(), ns);
				}
			}
			if ((id.length() == 0 || idDepth > 0) && isCurrent(subList(stack, idDepth))) {
				return new NamespaceStartElement(event.asStartElement(),
						namespaces);
			}
		}
		boolean pass = (id.length() == 0 || idDepth > 0) && isWithin(subList(stack, idDepth));
		if (event.isEndElement()) {
			position = stack.remove(stack.size() - 1);
			if (idDepth > stack.size()) {
				idDepth = 0;
			}
		}
		if (pass)
			return event;
		return null;
	}

	private List<Integer> subList(List<Integer> stack, int start) {
		return stack.subList(start, stack.size());
	}

	private String getIdOf(StartElement event) {
		Attribute attr = event.getAttributeByName(ID);
		if (attr == null)
			return null;
		return attr.getValue();
	}

	private boolean isParentOrCurrent(List<Integer> stack) {
		return stack.size() <= path.size()
				&& path.subList(0, stack.size()).equals(stack);
	}

	private boolean isCurrent(List<Integer> stack) {
		return stack.size() == path.size() && stack.equals(path);
	}

	private boolean isWithin(List<Integer> stack) {
		return stack.size() >= path.size()
				&& stack.subList(0, path.size()).equals(path);
	}
}
