package org.callimachusproject.engine;

import org.callimachusproject.engine.events.RDFEvent;

public interface RDFEventReader {

	boolean hasNext() throws RDFParseException;

	RDFEvent peek() throws RDFParseException;

	RDFEvent next() throws RDFParseException;

	void close() throws RDFParseException;

}