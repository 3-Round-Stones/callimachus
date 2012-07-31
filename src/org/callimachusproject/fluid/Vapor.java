/*
   Copyright (c) 2012 3 Round Stones Inc, Some Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.fluid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.SAXException;

/**
 * Abstract class that redirects {@link #toMedia(FluidType)} and
 * {@link #as(FluidType)} to more specific typed methods that can be overridden.
 * 
 * @author James Leigh
 * 
 */
public abstract class Vapor extends AbstractFluid {

	/**
	 * {@link FluidType}
	 */
	public final String toMedia(FluidType ftype) {

		if (ftype.is(ReadableByteChannel.class))
			return toChannelMedia(ftype);

		if (ftype.is(InputStream.class))
			return toStreamMedia(ftype);

		if (ftype.is(Reader.class))
			return toReaderMedia(ftype);

		if (ftype.is(String.class))
			return toStringMedia(ftype);

		if (ftype.is(HttpEntity.class))
			return toHttpEntityMedia(ftype);

		if (ftype.is(HttpResponse.class))
			return toHttpResponseMedia(ftype);

		if (ftype.is(XMLEventReader.class))
			return toXMLEventReaderMedia(ftype);

		if (ftype.is(Document.class))
			return toDocumentMedia(ftype);

		if (ftype.is(DocumentFragment.class))
			return toDocumentFragmentMedia(ftype);

		return null;
	}

	public final Object as(FluidType ftype) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {

		if (ftype.is(ReadableByteChannel.class))
			return asChannel(ftype);

		if (ftype.is(InputStream.class))
			return asStream(ftype);

		if (ftype.is(Reader.class))
			return asReader(ftype);

		if (ftype.is(String.class))
			return asString(ftype);

		if (ftype.is(HttpEntity.class))
			return asHttpEntity(ftype);

		if (ftype.is(HttpResponse.class))
			return asHttpResponse(ftype);

		if (ftype.is(XMLEventReader.class))
			return asXMLEventReader(ftype);

		if (ftype.is(Document.class))
			return asDocument(ftype);

		if (ftype.is(DocumentFragment.class))
			return asDocumentFragment(ftype);

		asVoid();
		return null;
	}

	/**
	 * {@link ReadableByteChannel}
	 */
	protected String toChannelMedia(FluidType media) {
		return null;
	}

	protected ReadableByteChannel asChannel(FluidType media)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		asVoid();
		return null;
	}

	protected void transferTo(WritableByteChannel out, FluidType media)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		ChannelUtil.transfer(asChannel(media), out);
	}

	/**
	 * {@link InputStream}
	 */
	protected String toStreamMedia(FluidType media) {
		return null;
	}

	protected InputStream asStream(FluidType media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		asVoid();
		return null;
	}

	protected void streamTo(OutputStream out, FluidType media)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		ChannelUtil.transfer(asStream(media), out);
	}

	/**
	 * {@link Reader}
	 */
	protected String toReaderMedia(FluidType media) {
		return null;
	}

	protected Reader asReader(FluidType media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		asVoid();
		return null;
	}

	protected void writeTo(Writer writer, FluidType media)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		Reader reader = asReader(media);
		if (reader == null)
			return;
		try {
			int read;
			char[] cbuf = new char[1024];
			while ((read = reader.read(cbuf)) >= 0) {
				writer.write(cbuf, 0, read);
			}
		} finally {
			reader.close();
		}
	}

	/**
	 * {@link String}
	 */
	protected String toStringMedia(FluidType media) {
		return null;
	}

	protected String asString(FluidType media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		asVoid();
		return null;
	}

	/**
	 * {@link HttpEntity}
	 */
	protected String toHttpEntityMedia(FluidType media) {
		return null;
	}

	protected HttpEntity asHttpEntity(FluidType media) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException, SAXException {
		asVoid();
		return null;
	}

	/**
	 * {@link HttpResponse}
	 */
	protected String toHttpResponseMedia(FluidType media) {
		return null;
	}

	protected HttpResponse asHttpResponse(FluidType media) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException, SAXException {
		asVoid();
		return null;
	}

	/**
	 * {@link XMLEventReader}
	 */
	protected String toXMLEventReaderMedia(FluidType media) {
		return null;
	}

	protected XMLEventReader asXMLEventReader(FluidType media)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		asVoid();
		return null;
	}

	/**
	 * {@link Document}
	 */
	protected String toDocumentMedia(FluidType media) {
		return null;
	}

	protected Document asDocument(FluidType media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException {
		asVoid();
		return null;
	}

	/**
	 * {@link DocumentFragment}
	 */
	protected String toDocumentFragmentMedia(FluidType media) {
		return null;
	}

	protected DocumentFragment asDocumentFragment(FluidType media)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		asVoid();
		return null;
	}

}
