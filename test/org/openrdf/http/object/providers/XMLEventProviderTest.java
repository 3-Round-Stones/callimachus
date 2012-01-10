package org.openrdf.http.object.providers;

import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.Comment;
import javax.xml.stream.events.XMLEvent;

import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.base.MetadataServerTestCase;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.object.xslt.XMLEventReaderFactory;

public class XMLEventProviderTest extends MetadataServerTestCase {

	public static abstract class Hello {
		private XMLEventFactory factory = XMLEventFactory.newInstance();

		@query("hello")
		@type("application/xml")
		public XMLEventReader hello() {
			LinkedList<XMLEvent> list = new LinkedList<XMLEvent>();
			list.add(factory.createStartDocument());
			list.add(factory.createStartElement("", "", "hello"));
			list.add(factory.createStartElement("", "", "world"));
			list.add(factory.createEndElement("", "", "world"));
			list.add(factory.createEndElement("", "", "hello"));
			list.add(factory.createEndDocument());
			return new XMLEventIterator(list.iterator());
		}
	}

	public void setUp() throws Exception {
		config.addBehaviour(Hello.class, RDFS.RESOURCE);
		super.setUp();
	}

	public void testXMLEvent() throws Exception {
		InputStream in = client.path("/").queryParam("hello", "").header("Accept-Encoding", "gzip;q=0").get(
				InputStream.class);
		XMLEventReader reader = XMLEventReaderFactory.newInstance()
				.createXMLEventReader(in);
		try {
			while (reader.hasNext()) {
				reader.nextEvent();
			}
		} finally {
			reader.close();
		}
	}

	public static class XMLEventIterator implements XMLEventReader {
		private Iterator<XMLEvent> delegate;
		private XMLEvent next;

		public XMLEventIterator(Iterator<XMLEvent> delegate) {
			this.delegate = delegate;
		}

		public void close() throws XMLStreamException {
			// no-op
		}

		public String getElementText() throws XMLStreamException {
			throw new XMLStreamException();
		}

		public Object getProperty(String name) throws IllegalArgumentException {
			return null;
		}

		public boolean hasNext() {
			try {
				return peek() != null;
			} catch (XMLStreamException e) {
				return false;
			}
		}

		public XMLEvent next() {
			if (next != null) {
				try {
					return next;
				} finally {
					next = null;
				}
			}
			return delegate.next();
		}

		public XMLEvent nextEvent() throws XMLStreamException {
			return next();
		}

		public XMLEvent nextTag() throws XMLStreamException {
			try {
				XMLEvent event;
				do {
					event = nextEvent();
					if (event.isStartElement() || event.isEndElement())
						return event;
				} while (event instanceof Comment || event.isCharacters()
						&& ((Characters) event).isIgnorableWhiteSpace());
				throw new XMLStreamException("Not a tag: " + event);
			} catch (NoSuchElementException e) {
				throw new XMLStreamException(e);
			}
		}

		public XMLEvent peek() throws XMLStreamException {
			if (next != null)
				return next;
			try {
				return next = nextEvent();
			} catch (NoSuchElementException e) {
				return null;
			}
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
