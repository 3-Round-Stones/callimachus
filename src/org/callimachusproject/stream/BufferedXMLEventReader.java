/*
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

package org.callimachusproject.stream;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.XMLEvent;

/**
 * 
 * @author Steve Battle
 * @author James Leigh
 *
 */
public class BufferedXMLEventReader implements XMLEventReader {
	private static final int CAPACITY_INCREMENT = 100;
	private List<XMLEvent> buffer;
	private XMLEventReader reader;
	private int position, mark=0;

	public BufferedXMLEventReader(XMLEventReader reader) {
		this.reader = reader;
	}
	
	public synchronized int mark() {
		if (buffer==null) {
			buffer = new Vector<XMLEvent>(CAPACITY_INCREMENT, CAPACITY_INCREMENT);
			return mark=0;
		}
		else return mark=position;
	}
	
	public synchronized void mark(int readlimit) {
		buffer = new ArrayList<XMLEvent>(readlimit);
	}
	
	public synchronized void reset() throws XMLStreamException {
		if (buffer==null) throw new XMLStreamException("bad stream reset");
		position = mark;
	}
	
	public synchronized void reset(int position) throws XMLStreamException {
		if (buffer==null || position<0 || position>buffer.size()) 
			throw new XMLStreamException("bad stream reset");
		this.position = position;
	}

	public String getElementText() throws XMLStreamException {
		StringBuilder sb = new StringBuilder();
		XMLEvent event;
		do {
			event = nextEvent();
			if (event.isCharacters())
				sb.append(event.asCharacters().getData());
		} while (event instanceof Comment || event.isCharacters());
		return sb.toString();
	}

	public Object getProperty(String name) throws IllegalArgumentException {
		return null;
	}

	public boolean hasNext() {
		return (position>=0 && buffer!=null && position<buffer.size()) || reader.hasNext() ;
	}

	public XMLEvent peek() throws XMLStreamException {
		if (position>=0 && position<buffer.size()) 
			return buffer.get(position);
		else return reader.peek();
	}
	
	protected final XMLEvent peek(int lookAhead) throws XMLStreamException {
		while (buffer.size()<=lookAhead+position) {
			buffer.add(reader.nextEvent());
		}
		if (buffer.size()==lookAhead+position) return reader.peek();
		return buffer.get(lookAhead+position);
	}

	public XMLEvent nextEvent() throws XMLStreamException {
		if (position>=0 && position<buffer.size()) {
			return buffer.get(position++);
		}
		else {
			position++;
			XMLEvent e = reader.nextEvent();
			if (buffer!=null) buffer.add(e);
			return e;
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

	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void close() throws XMLStreamException {
		if (reader!=null) reader.close();
	}

}
