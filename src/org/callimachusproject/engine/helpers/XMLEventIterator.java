package org.callimachusproject.engine.helpers;

import java.util.ListIterator;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.XMLEvent;

@SuppressWarnings("rawtypes")
public final class XMLEventIterator implements XMLEventReader, ListIterator {
	private final ListIterator<XMLEvent> iter;

	public XMLEventIterator(ListIterator<XMLEvent> iter) {
		this.iter = iter;
	}

	@Override
	public void close() {
		// do nothing
	}

	public String getElementText() {
		StringBuilder sb = new StringBuilder();
		XMLEvent event;
		do {
			event = nextEvent();
			if (event.isCharacters())
				sb.append(event.asCharacters().getData());
		} while (event instanceof Comment || event.isCharacters());
		previous();
		return sb.toString();
	}

	public Object getProperty(String name) {
		return null;
	}

	public XMLEvent peek() {
		if (hasNext()) {
			try {
				return nextEvent();
			} finally {
				previous();
			}
		}
		return null;
	}

	public XMLEvent nextEvent() {
		return iter.next();
	}

	public XMLEvent previousEvent() {
		return iter.previous();
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
		return nextEvent();
	}

	public Object previous() {
		return previousEvent();
	}

	public boolean hasNext() {
		return iter.hasNext();
	}

	@Override
	public boolean hasPrevious() {
		return iter.hasPrevious();
	}

	@Override
	public int nextIndex() {
		return iter.nextIndex();
	}

	@Override
	public int previousIndex() {
		return iter.previousIndex();
	}

	@Override
	public void remove() {
		iter.remove();
	}

	@Override
	public void set(Object e) {
		iter.set((XMLEvent) e);
	}

	@Override
	public void add(Object e) {
		iter.add((XMLEvent) e);
	}
}