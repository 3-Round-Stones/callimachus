package org.callimachusproject.engine.helpers;

import java.util.Collection;
import java.util.LinkedList;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.RDFEvent;

public class RDFEventList extends LinkedList<RDFEvent> {
	private static final long serialVersionUID = -7053498842173406118L;

	public RDFEventList() {
		super();
	}

	public RDFEventList(Collection<RDFEvent> events) {
		super(events);
	}

	/**
	 * 
	 * @param reader is closed
	 * @throws RDFParseException 
	 */
	public RDFEventList(RDFEventReader reader) throws RDFParseException {
		try {
			addAll(reader);
		} finally {
			reader.close();
		}
	}

	/**
	 * 
	 * @param reader is not closed
	 * @throws RDFParseException
	 */
	public void addAll(RDFEventReader reader) throws RDFParseException {
		while (reader.hasNext()) {
			add(reader.next());
		}
	}

	public RDFEventIterator iterator() {
		return new RDFEventIterator(this, super.listIterator());
	}

	public RDFEventIterator listIterator() {
		return new RDFEventIterator(this, super.listIterator());
	}

	public RDFEventIterator listIterator(int index) {
		return new RDFEventIterator(this, super.listIterator(index));
	}

}
