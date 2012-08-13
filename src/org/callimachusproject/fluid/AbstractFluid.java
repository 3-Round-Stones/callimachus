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
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.xml.stream.XMLEventReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.callimachusproject.server.util.ChannelUtil;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

/**
 * Abstract class that redirects many calls to {@link #toMedia(FluidType)} and
 * {@link #as(FluidType)}.
 * 
 * @author James Leigh
 * 
 */
public abstract class AbstractFluid implements Fluid {

	public String toString() {
		String systemId = getSystemId();
		if (systemId == null)
			return String.valueOf(getFluidType());
		return systemId + " " + String.valueOf(getFluidType());
	}

	/**
	 * {@link ReadableByteChannel}
	 */
	public final String toChannelMedia(String... media) {
		return toMedia(new FluidType(ReadableByteChannel.class, media));
	}

	public final ReadableByteChannel asChannel(String... media)
			throws IOException, FluidException {
		return (ReadableByteChannel) as(new FluidType(
				ReadableByteChannel.class, media));
	}

	public final void transferTo(WritableByteChannel out, String... media)
			throws IOException, FluidException {
		ChannelUtil.transfer(asChannel(media), out);
	}

	/**
	 * {@link InputStream}
	 */
	public final String toStreamMedia(String... media) {
		return toMedia(new FluidType(InputStream.class, media));
	}

	public final InputStream asStream(String... media) throws IOException,
			FluidException {
		return (InputStream) as(new FluidType(InputStream.class, media));
	}

	public final void streamTo(OutputStream out, String... media)
			throws IOException, FluidException {
		ChannelUtil.transfer(asStream(media), out);
	}

	/**
	 * {@link Reader}
	 */
	public final String toReaderMedia(String... media) {
		return toMedia(new FluidType(Reader.class, media));
	}

	public final Reader asReader(String... media) throws IOException,
			FluidException {
		return (Reader) as(new FluidType(Reader.class, media));
	}

	public final void writeTo(Writer writer, String... media)
			throws IOException, FluidException {
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
	public final String toStringMedia(String... media) {
		return toMedia(new FluidType(String.class, media));
	}

	public final String asString(String... media) throws IOException,
			FluidException {
		return (String) as(new FluidType(String.class, media));
	}

	/**
	 * {@link CharSequence}
	 */
	public String toCharSequenceMedia(String... media) {
		return toMedia(new FluidType(CharSequence.class, media));
	}

	public CharSequence asCharSequence(String... media) throws IOException,
			FluidException {
		return (CharSequence) as(new FluidType(CharSequence.class, media));
	}

	/**
	 * {@link HttpEntity}
	 */
	public final String toHttpEntityMedia(String... media) {
		return toMedia(new FluidType(HttpEntity.class, media));
	}

	public final HttpEntity asHttpEntity(String... media) throws IOException,
			FluidException {
		return (HttpEntity) as(new FluidType(HttpEntity.class, media));
	}

	/**
	 * {@link HttpResponse}
	 */
	public final String toHttpResponseMedia(String... media) {
		return toMedia(new FluidType(HttpResponse.class, media));
	}

	public final HttpResponse asHttpResponse(String... media)
			throws IOException, FluidException {
		return (HttpResponse) as(new FluidType(HttpResponse.class, media));
	}

	/**
	 * {@link XMLEventReader}
	 */
	public final String toXMLEventReaderMedia(String... media) {
		return toMedia(new FluidType(XMLEventReader.class, media));
	}

	public final XMLEventReader asXMLEventReader(String... media)
			throws IOException, FluidException {
		return (XMLEventReader) as(new FluidType(XMLEventReader.class, media));
	}

	/**
	 * {@link Document}
	 */
	public final String toDocumentMedia(String... media) {
		return toMedia(new FluidType(Document.class, media));
	}

	public final Document asDocument(String... media) throws IOException,
			FluidException {
		return (Document) as(new FluidType(Document.class, media));
	}

	/**
	 * {@link DocumentFragment}
	 */
	public final String toDocumentFragmentMedia(String... media) {
		return toMedia(new FluidType(DocumentFragment.class, media));
	}

	public final DocumentFragment asDocumentFragment(String... media)
			throws IOException, FluidException {
		return (DocumentFragment) as(new FluidType(DocumentFragment.class,
				media));
	}

	/**
	 * {@link Element}
	 */
	public String toElementMedia(String... media) {
		return toMedia(new FluidType(Element.class, media));
	}

	public Element asElement(String... media) throws IOException,
			FluidException {
		return (Element) as(new FluidType(Element.class, media));
	}

	/**
	 * {@link Type}
	 */
	public final String toMedia(Type gtype, String... media) {
		return toMedia(new FluidType(gtype, media));
	}

	public final Object as(Type gtype, String... media) throws IOException,
			FluidException {
		return as(new FluidType(gtype, media));
	}

}
