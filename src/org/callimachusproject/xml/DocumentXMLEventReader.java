/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.xml;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndDocument;
import javax.xml.stream.events.StartDocument;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

/**
 * Wraps an {@link XMLEventReader} and every stream starts with
 * {@link javax.xml.stream.events.StartDocument} and ends with
 * {@link javax.xml.stream.events.EndDocument}.
 * 
 * @author James Leigh
 * 
 */
public class DocumentXMLEventReader extends EventReaderDelegate {
	private final XMLEventFactory ef = XMLEventFactory.newInstance();
	private boolean started;
	private Object next;
	private boolean ended;

	public DocumentXMLEventReader(XMLEventReader reader) {
		super(reader);
	}

	public void close() throws XMLStreamException {
		super.close();
	}

	@Override
	public XMLEvent nextEvent() throws XMLStreamException {
		if (next != null) {
			try {
				if (next instanceof XMLEvent)
					return (XMLEvent) next;
			} finally {
				next = null;
			}
		}
		if (!started && !super.hasNext()) {
			started = true;
			return ef.createStartDocument();
		}
		if (!ended && !super.hasNext()) {
			ended = true;
			return ef.createEndDocument();
		}
		XMLEvent ret = super.nextEvent();
		if (ret != null && ret.isStartDocument()) {
			started = true;
		}
		if (ret != null && ret.isEndDocument()) {
			ended = true;
		}
		if (!started) {
			next = ret;
			started = true;
			return ef.createStartDocument();
		}
		return ret;
	}

	@Override
	public Object next() {
		if (next != null) {
			try {
				if (next instanceof XMLEvent)
					return (XMLEvent) next;
			} finally {
				next = null;
			}
		}
		if (!started && !super.hasNext()) {
			started = true;
			return ef.createStartDocument();
		}
		if (!ended && !super.hasNext()) {
			ended = true;
			return ef.createEndDocument();
		}
		Object ret = super.next();
		if (ret != null && ret instanceof StartDocument) {
			started = true;
		}
		if (ret != null && ret instanceof EndDocument) {
			ended = true;
		}
		if (!started) {
			next = ret;
			started = true;
			return ef.createStartDocument();
		}
		return ret;
	}

	@Override
	public boolean hasNext() {
		if (!ended)
			return true;
		return super.hasNext();
	}

	@Override
	public XMLEvent peek() throws XMLStreamException {
		if (next != null && next instanceof XMLEvent)
			return (XMLEvent) next;
		XMLEvent peek = super.peek();
		if (!started && !peek.isStartDocument())
			return ef.createStartDocument();
		if (!ended && peek == null)
			return ef.createEndDocument();
		return peek;
	}

	@Override
	public XMLEvent nextTag() throws XMLStreamException {
		if (!started) {
			started = true;
		}
		return super.nextTag();
	}

}
