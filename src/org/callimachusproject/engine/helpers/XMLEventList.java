/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.engine.helpers;

import java.util.Collection;
import java.util.LinkedList;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

public class XMLEventList extends LinkedList<XMLEvent> {
	private static final long serialVersionUID = -7053498842173406118L;

	public XMLEventList() {
		super();
	}

	public XMLEventList(Collection<XMLEvent> events) {
		super(events);
	}

	/**
	 * 
	 * @param reader is closed
	 * @throws XMLStreamException
	 */
	public XMLEventList(XMLEventReader reader) throws XMLStreamException {
		try {
			addAll(reader);
		} finally {
			reader.close();
		}
	}

	/**
	 * 
	 * @param reader is not closed
	 * @throws XMLStreamException
	 */
	public void addAll(XMLEventReader reader) throws XMLStreamException {
		while (reader.hasNext()) {
			add(reader.nextEvent());
		}
	}

	@Override
	public boolean add(XMLEvent e) {
		assert e.getLocation().getCharacterOffset() >= 0 || e.isEndDocument() : e;
		return super.add(e);
	}

	public XMLEventIterator iterator() {
		return new XMLEventIterator(super.listIterator());
	}

	public XMLEventIterator listIterator(int index) {
		return new XMLEventIterator(super.listIterator(index));
	}

}
