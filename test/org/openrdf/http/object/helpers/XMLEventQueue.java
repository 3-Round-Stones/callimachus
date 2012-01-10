/*
 * Copyright (c) 2009, Zepheira All rights reserved.
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
package org.openrdf.http.object.helpers;

import static javax.xml.XMLConstants.DEFAULT_NS_PREFIX;
import static javax.xml.XMLConstants.NULL_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
import static javax.xml.XMLConstants.XML_NS_PREFIX;
import static javax.xml.XMLConstants.XML_NS_URI;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.XMLEvent;

import org.openrdf.repository.object.xslt.XSLTransformer;

/**
 * An XMLEvent queue that can be read using an XMLEventReader.
 */
public class XMLEventQueue implements XMLEventWriter, NamespaceContext {
	private static XMLEvent CLOSED = (XMLEvent) Proxy.newProxyInstance(
			XSLTransformer.class.getClassLoader(),
			new Class<?>[] { XMLEvent.class }, new InvocationHandler() {
				public Object invoke(Object proxy, Method method, Object[] args)
						throws Throwable {
					return null;
				}
			});

	private static class XMLEventQueueReader implements XMLEventReader {
		private XMLEventQueue queue;
		private XMLEvent next;
		private XMLStreamException nextException;

		public XMLEventQueueReader(XMLEventQueue queue) {
			this.queue = queue;
		}

		public void close() throws XMLStreamException {
			queue.abort();
		}

		public boolean hasNext() {
			try {
				if (next == null)
					return (next = queue.take()) != null;
			} catch (XMLStreamException e) {
				nextException = e;
			}
			return true;
		}

		public XMLEvent peek() throws XMLStreamException {
			if (next == null)
				return next = queue.take();
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
				XMLEvent result = queue.take();
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
	}

	private BlockingQueue<XMLEvent> queue;
	private NamespaceContext context = this;
	private Map<String, String> prefixes = new HashMap<String, String>();
	private XMLEventReader reader;
	private volatile boolean abort;
	private volatile XMLStreamException fatal;

	public XMLEventQueue() {
		this(new LinkedBlockingQueue<XMLEvent>());
	}

	public XMLEventQueue(int capacity) {
		this(new ArrayBlockingQueue<XMLEvent>(capacity));
	}

	public XMLEventQueue(BlockingQueue<XMLEvent> queue) {
		this.queue = queue;
		prefixes.put(XML_NS_PREFIX, XML_NS_URI);
		prefixes.put(XMLNS_ATTRIBUTE, XMLNS_ATTRIBUTE_NS_URI);
		this.reader = new XMLEventQueueReader(this);
	}

	public XMLEventReader getXMLEventReader() {
		return reader;
	}

	private XMLEvent take() throws XMLStreamException {
		if (abort) {
			if (fatal != null)
				throw fatal;
			return null;
		}
		try {
			XMLEvent result = queue.take();
			if (CLOSED == result) {
				abort = true;
				return null;
			}
			return result;
		} catch (InterruptedException e) {
			throw new XMLStreamException(e);
		}
	}

	private void abort() throws XMLStreamException {
		abort = true;
		queue.clear();
		queue.offer(CLOSED);
		if (fatal != null)
			throw fatal;
	}

	public void abort(XMLStreamException fatal) {
		abort = true;
		if (fatal != null) {
			this.fatal = fatal;
		}
		queue.clear();
		queue.offer(CLOSED);
	}

	public void add(XMLEvent event) throws XMLStreamException {
		try {
			if (!abort) {
				queue.put(event);
			}
		} catch (InterruptedException e) {
			throw new XMLStreamException(e);
		}
	}

	public void add(XMLEventReader reader) throws XMLStreamException {
		while (reader.hasNext()) {
			add(reader.nextEvent());
		}
	}

	public void close() throws XMLStreamException {
		try {
			if (!abort) {
				queue.put(CLOSED);
			}
		} catch (InterruptedException e) {
			throw new XMLStreamException(e);
		}
	}

	public void flush() throws XMLStreamException {
		// no-op
	}

	public NamespaceContext getNamespaceContext() {
		return context;
	}

	public String getPrefix(String uri) {
		Iterator<String> iter = context.getPrefixes(uri);
		if (iter.hasNext())
			return iter.next();
		return null;
	}

	public void setDefaultNamespace(String uri) throws XMLStreamException {
		prefixes.put(DEFAULT_NS_PREFIX, uri);
	}

	public void setNamespaceContext(NamespaceContext context)
			throws XMLStreamException {
		this.context = context;
	}

	public void setPrefix(String prefix, String uri) throws XMLStreamException {
		prefixes.put(prefix, uri);
	}

	public String getNamespaceURI(String prefix) {
		if (prefix == null)
			throw new IllegalArgumentException();
		if (prefixes.containsKey(prefix))
			return prefixes.get(prefix);
		return NULL_NS_URI;
	}

	public Iterator getPrefixes(String uri) {
		List<String> list = new ArrayList<String>();
		for (Entry<String, String> e : prefixes.entrySet()) {
			if (uri.equals(e.getValue())) {
				list.add(e.getKey());
			}
		}
		return list.iterator();
	}
}
