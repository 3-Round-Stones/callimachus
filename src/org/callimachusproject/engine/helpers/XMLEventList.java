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
