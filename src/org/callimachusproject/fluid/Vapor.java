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

import javax.xml.stream.XMLEventReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.callimachusproject.io.ChannelUtil;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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

		if (ftype.is(Reader.class) || ftype.is(Readable.class))
			return toReaderMedia(ftype);

		if (ftype.is(String.class) || ftype.is(CharSequence.class))
			return toCharSequenceMedia(ftype);

		if (ftype.is(HttpEntity.class))
			return toHttpEntityMedia(ftype);

		if (ftype.is(HttpResponse.class))
			return toHttpResponseMedia(ftype);

		if (ftype.is(XMLEventReader.class))
			return toXMLEventReaderMedia(ftype);

		if (ftype.is(Document.class) || ftype.is(Node.class))
			return toDocumentMedia(ftype);

		if (ftype.is(DocumentFragment.class))
			return toDocumentFragmentMedia(ftype);

		if (ftype.is(Element.class))
			return toElementMedia(ftype);

		return null;
	}

	public final Object as(FluidType ftype) throws IOException, FluidException {
		try {
			if (ftype.is(ReadableByteChannel.class))
				return asChannel(ftype);

			if (ftype.is(InputStream.class))
				return asStream(ftype);

			if (ftype.is(Reader.class) || ftype.is(Readable.class))
				return asReader(ftype);

			if (ftype.is(CharSequence.class))
				return asCharSequence(ftype);

			if (ftype.is(String.class)) {
				CharSequence cs = asCharSequence(ftype);
				if (cs == null)
					return null;
				return cs.toString();
			}

			if (ftype.is(HttpEntity.class))
				return asHttpEntity(ftype);

			if (ftype.is(HttpResponse.class))
				return asHttpResponse(ftype);

			if (ftype.is(XMLEventReader.class))
				return asXMLEventReader(ftype);

			if (ftype.is(Document.class) || ftype.is(Node.class))
				return asDocument(ftype);

			if (ftype.is(DocumentFragment.class))
				return asDocumentFragment(ftype);

			if (ftype.is(Element.class))
				return asElement(ftype);

			asVoid();
			return null;
		} catch (Error e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (FluidException e) {
			throw e;
		} catch (Exception e) {
			throw fluidException(e);
		}
	}

	/**
	 * {@link ReadableByteChannel}
	 */
	protected String toChannelMedia(FluidType media) {
		return null;
	}

	protected ReadableByteChannel asChannel(FluidType media) throws Exception {
		asVoid();
		return null;
	}

	protected void transferTo(WritableByteChannel out, FluidType media)
			throws Exception {
		ChannelUtil.transfer(asChannel(media), out);
	}

	/**
	 * {@link InputStream}
	 */
	protected String toStreamMedia(FluidType media) {
		return null;
	}

	protected InputStream asStream(FluidType media) throws Exception {
		asVoid();
		return null;
	}

	protected void streamTo(OutputStream out, FluidType media) throws Exception {
		ChannelUtil.transfer(asStream(media), out);
	}

	/**
	 * {@link Reader}
	 */
	protected String toReaderMedia(FluidType media) {
		return null;
	}

	protected Reader asReader(FluidType media) throws Exception {
		asVoid();
		return null;
	}

	protected void writeTo(Writer writer, FluidType media) throws Exception {
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
	 * {@link CharSequence}
	 */
	protected String toCharSequenceMedia(FluidType media) {
		return null;
	}

	protected CharSequence asCharSequence(FluidType media) throws Exception {
		asVoid();
		return null;
	}

	/**
	 * {@link HttpEntity}
	 */
	protected String toHttpEntityMedia(FluidType media) {
		return null;
	}

	protected HttpEntity asHttpEntity(FluidType media) throws Exception {
		asVoid();
		return null;
	}

	/**
	 * {@link HttpResponse}
	 */
	protected String toHttpResponseMedia(FluidType media) {
		return null;
	}

	protected HttpResponse asHttpResponse(FluidType media) throws Exception {
		asVoid();
		return null;
	}

	/**
	 * {@link XMLEventReader}
	 */
	protected String toXMLEventReaderMedia(FluidType media) {
		return null;
	}

	protected XMLEventReader asXMLEventReader(FluidType media) throws Exception {
		asVoid();
		return null;
	}

	/**
	 * {@link Document}
	 */
	protected String toDocumentMedia(FluidType media) {
		return null;
	}

	protected Document asDocument(FluidType media) throws Exception {
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
			throws Exception {
		asVoid();
		return null;
	}

	/**
	 * {@link Element}
	 */
	protected String toElementMedia(FluidType media) {
		return null;
	}

	protected Element asElement(FluidType media) throws Exception {
		asVoid();
		return null;
	}

	private FluidException fluidException(Exception e) throws IOException,
			FluidException {
		try {
			if (e == null)
				return null;
			Throwable cause = e;
			while (cause instanceof IOException && cause != e) {
				cause = cause.getCause();
			}
			if (cause == null)
				throw (IOException) e;
			return new FluidException(e);
		} finally {
			try {
				asVoid();
			} catch (RuntimeException v) {
				v.initCause(e);
				throw v;
			} catch (Error v) {
				v.initCause(e);
				throw v;
			} catch (FluidException v) {
				v.initCause(e);
				throw v;
			} catch (IOException v) {
				v.initCause(e);
				throw v;
			}
		}
	}

}
