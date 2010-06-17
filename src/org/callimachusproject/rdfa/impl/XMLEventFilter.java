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
package org.callimachusproject.rdfa.impl;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

public abstract class XMLEventFilter extends EventReaderDelegate {
	private XMLEvent next;
	private XMLStreamException nextException;

	public XMLEventFilter() {
		super();
	}

	public XMLEventFilter(XMLEventReader reader) {
		super(reader);
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

	protected abstract boolean accept(XMLEvent event) throws XMLStreamException;

	protected XMLEvent peekNext() throws XMLStreamException {
		return super.peek();
	}

	private XMLEvent take() throws XMLStreamException {
		while (super.hasNext()) {
			XMLEvent result = super.nextEvent();
			if (accept(result))
				return result;
		}
		return null;
	}

}
