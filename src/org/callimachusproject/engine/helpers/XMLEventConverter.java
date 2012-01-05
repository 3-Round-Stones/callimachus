/*
 * Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
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
package org.callimachusproject.engine.helpers;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayDeque;
import java.util.NoSuchElementException;
import java.util.Queue;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * Provides basic methods to implement a {@link EventReaderDelegate} filter and
 * provides replay functionality to re-read the start of a document.
 * 
 * @author James Leigh
 * 
 */
public abstract class XMLEventConverter extends EventReaderDelegate {
	private XMLEvent next;
	private Queue<XMLEvent> buffer;
	private boolean reset = true;
	private int readlimit = 0;
	private XMLStreamException nextException;

	public XMLEventConverter() {
		super();
	}

	public XMLEventConverter(XMLEventReader reader) {
		super(reader);
	}

	public void mark(int readlimit) {
		if (buffer != null)
			throw new IllegalStateException("Can only mark once");
		buffer = new ArrayDeque<XMLEvent>(readlimit + 1);
		this.readlimit = readlimit;
		reset = false;
	}

	public void reset() {
		reset = true;
		readlimit = 0;
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

	protected abstract XMLEvent convert(XMLEvent event) throws XMLStreamException;

	private XMLEvent take() throws XMLStreamException {
		if (reset && buffer != null && !buffer.isEmpty()) {
			return buffer.poll();
		}
		while (super.hasNext()) {
			XMLEvent result = convert(super.nextEvent());
			if (result != null) {
				if (readlimit > 0) {
					buffer.add(result);
					if (buffer.size() > readlimit) {
						reset = true;
						readlimit = 0;
						buffer = null;
					}
				}
				return result;
			}
		}
		return null;
	}

}
