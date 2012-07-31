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

import java.io.Closeable;
import java.io.IOException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.EventReaderDelegate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps an {@link XMLEventReader} and closes the stream when the XMLReader is
 * closed.
 * 
 * @author James Leigh
 * 
 */
public class ClosingXMLEventReader extends EventReaderDelegate {
	private final Logger logger = LoggerFactory
			.getLogger(ClosingXMLEventReader.class);
	private Closeable io;

	public ClosingXMLEventReader(XMLEventReader reader, Closeable io) {
		super(reader);
		this.io = io;
	}

	public void close() throws XMLStreamException {
		try {
			super.close();
		} finally {
			try {
				io.close();
			} catch (IOException e) {
				throw new XMLStreamException(e);
			}
		}
	}

	@Override
	public XMLEvent nextEvent() throws XMLStreamException {
		try {
			return super.nextEvent();
		} catch (XMLStreamException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public Object next() {
		try {
			return super.next();
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public boolean hasNext() {
		try {
			return super.hasNext();
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public XMLEvent peek() throws XMLStreamException {
		try {
			return super.peek();
		} catch (XMLStreamException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public String getElementText() throws XMLStreamException {
		try {
			return super.getElementText();
		} catch (XMLStreamException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public XMLEvent nextTag() throws XMLStreamException {
		try {
			return super.nextTag();
		} catch (XMLStreamException e) {
			throw handle(e);
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	@Override
	public void remove() {
		try {
			super.remove();
		} catch (RuntimeException e) {
			throw handle(e);
		} catch (Error e) {
			throw handle(e);
		}
	}

	private <E extends Throwable> E handle(E exc) {
		try {
			close();
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
		return exc;
	}

}
