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
package org.callimachusproject.xslt;

import java.io.Closeable;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLReporter;
import javax.xml.stream.XMLResolver;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.util.XMLEventAllocator;

/**
 * Wraps a XMLInputFactory, but closes input streams when the XMLEventReader is
 * closed. This implementation also ensure every stream starts with
 * {@link javax.xml.stream.events.StartDocument} and ends with
 * {@link javax.xml.stream.events.EndDocument}.
 * 
 * @author James Leigh
 * 
 */
public class XMLEventReaderFactory {

	public static XMLEventReaderFactory newInstance() {
		XMLInputFactory factory = XMLInputFactory.newInstance();
		factory.setProperty(XMLInputFactory.IS_VALIDATING, false);
		factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		factory.setProperty(
				"http://java.sun.com/xml/stream/properties/ignore-external-dtd",
				true);
		return new XMLEventReaderFactory(factory);
	}

	private XMLInputFactory factory;

	public XMLEventReaderFactory(XMLInputFactory factory) {
		this.factory = factory;
	}

	public XMLEventReader createXMLEventReader(InputStream stream,
			String encoding) throws XMLStreamException {
		return wrap(factory.createXMLEventReader(stream, encoding), stream);
	}

	public XMLEventReader createXMLEventReader(InputStream stream)
			throws XMLStreamException {
		return wrap(factory.createXMLEventReader(stream), stream);
	}

	public XMLEventReader createXMLEventReader(Reader reader)
			throws XMLStreamException {
		return wrap(factory.createXMLEventReader(reader), reader);
	}

	public XMLEventReader createXMLEventReader(String systemId,
			InputStream stream) throws XMLStreamException {
		return wrap(factory.createXMLEventReader(systemId, stream), stream);
	}

	public XMLEventReader createXMLEventReader(String systemId, Reader reader)
			throws XMLStreamException {
		return wrap(factory.createXMLEventReader(systemId, reader), reader);
	}

	public XMLEventAllocator getEventAllocator() {
		return factory.getEventAllocator();
	}

	public Object getProperty(String name) throws IllegalArgumentException {
		return factory.getProperty(name);
	}

	public XMLReporter getXMLReporter() {
		return factory.getXMLReporter();
	}

	public XMLResolver getXMLResolver() {
		return factory.getXMLResolver();
	}

	public boolean isPropertySupported(String name) {
		return factory.isPropertySupported(name);
	}

	public void setEventAllocator(XMLEventAllocator allocator) {
		factory.setEventAllocator(allocator);
	}

	public void setProperty(String name, Object value)
			throws IllegalArgumentException {
		factory.setProperty(name, value);
	}

	public void setXMLReporter(XMLReporter reporter) {
		factory.setXMLReporter(reporter);
	}

	public void setXMLResolver(XMLResolver resolver) {
		factory.setXMLResolver(resolver);
	}

	private XMLEventReader wrap(XMLEventReader reader, Closeable io) {
		return new DocumentXMLEventReader(new ClosingXMLEventReader(reader, io));
	}
}
