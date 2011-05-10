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
package org.callimachusproject.stream;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.XMLEvent;

/**
 * Provides a {@link Queue} for buffering {@link XMLEventReader} output.
 * 
 * @author James Leigh
 *
 */

public abstract class XMLEventReaderBase implements XMLEventReader {
	private XMLEvent next;
	private XMLStreamException nextException;
	private Queue<XMLEvent> queue = new LinkedList<XMLEvent>();

	public String getElementText() throws XMLStreamException {
		StringBuilder sb = new StringBuilder();
		XMLEvent event;
		do {
			event = nextEvent();
			if (event.isCharacters())
				sb.append(event.asCharacters().getData());
		} while (event instanceof Comment || event.isCharacters());
		next = event;
		return sb.toString();
	}

	public Object getProperty(String name) throws IllegalArgumentException {
		return null;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

	public boolean hasNext() {
		try {
			if (next == null)
				return (next = take()) != null;
		} catch (XMLStreamException e) {
			nextException = e;
		}
		return true;
	}

	public XMLEvent peek() throws XMLStreamException {
		if (next == null)
			return next = take();
		return next;
	}

	public XMLEvent nextEvent() throws XMLStreamException {
		if (nextException != null) {
			try {
				throw nextException;
			} finally {
				nextException = null;
			}
		}

		if (next == null) {
			XMLEvent result = take();
			if (result == null)
				throw new NoSuchElementException();
			return result;
		}

		try {
			return next;
		} finally {
			next = null;
		}
	}

	public XMLEvent nextTag() throws XMLStreamException {
		XMLEvent event;
		do {
			event = nextEvent();
			if (event.isStartElement() || event.isEndElement())
				return event;
		} while (event instanceof Comment || event.isCharacters()
				&& event.asCharacters().isIgnorableWhiteSpace());
		throw new XMLStreamException("Not a tag: " + event);
	}

	public Object next() {
		try {
			return nextEvent();
		} catch (XMLStreamException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	protected XMLEvent take() throws XMLStreamException {
		while (queue.isEmpty()) {
			if (!more())
				return null;
		}
		return queue.remove();
	}

	protected abstract boolean more() throws XMLStreamException;

	protected void add(XMLEvent next) {
		queue.add(next);
	}

	protected boolean addAll(Collection<? extends XMLEvent> c) {
		return queue.addAll(c);
	}

}
