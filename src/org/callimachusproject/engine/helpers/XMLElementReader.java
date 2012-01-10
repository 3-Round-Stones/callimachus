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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.XMLEvent;

import org.callimachusproject.server.exceptions.NotImplemented;

/**
 * Extracts an element from an XML document given the element's xptr.
 * 
 * @author James Leigh
 *
 */
public final class XMLElementReader extends XMLEventConverter {
	private final List<Integer> path;
	private List<Integer> stack = new LinkedList<Integer>();
	private int position;
	private Map<String, Namespace> namespaces = new HashMap<String, Namespace>();

	public XMLElementReader(XMLEventReader reader, String element) {
		super(reader);
		String[] ar = element.split("/");
		String id = ar[0];
		if (id.length() > 0)
			throw new NotImplemented("ID is not supported in element parameter");
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
			if (stack.size() <= path.size()
					&& path.subList(0, stack.size()).equals(stack)) {
				Iterator nter = event.asStartElement().getNamespaces();
				while (nter.hasNext()) {
					Namespace ns = (Namespace) nter.next();
					namespaces.put(ns.getPrefix(), ns);
				}
			}
			if (stack.size() == path.size() && stack.equals(path)) {
				return new NamespaceStartElement(event.asStartElement(),
						namespaces);
			}
		}
		boolean included = stack.size() >= path.size()
				&& stack.subList(0, path.size()).equals(path);
		if (event.isEndElement()) {
			position = stack.remove(stack.size() - 1);
		}
		if (included)
			return event;
		return null;
	}
}
