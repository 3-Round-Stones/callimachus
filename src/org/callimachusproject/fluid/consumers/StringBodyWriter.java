/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.callimachusproject.fluid.consumers;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.Vapor;
import org.callimachusproject.fluid.consumers.helpers.CharSequenceReader;
import org.callimachusproject.server.model.ReadableHttpEntityChannel;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

/**
 * Writes a {@link String}.
 * 
 * @author James Leigh
 * 
 */
public class StringBodyWriter implements Consumer<CharSequence> {
	static final boolean SINGLE_BYTE = 1f == java.nio.charset.Charset
			.defaultCharset().newEncoder().maxBytesPerChar();

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		if (!mtype.is("text/*"))
			return false;
		return CharSequence.class.isAssignableFrom(mtype.asClass());
	}

	public Fluid consume(final CharSequence result, final String base,
			final FluidType ftype, final FluidBuilder builder) {
		return new Vapor() {
			public String getSystemId() {
				return base;
			}

			public FluidType getFluidType() {
				return ftype;
			}

			public void asVoid() {
				// no-op
			}

			@Override
			protected String toChannelMedia(FluidType media) {
				return getMediaType(ftype.as(media));
			}

			@Override
			protected ReadableByteChannel asChannel(FluidType media)
					throws IOException {
				return write(ftype.as(toChannelMedia(media)), result, base);
			}

			@Override
			protected String toHttpEntityMedia(FluidType media) {
				return toChannelMedia(media);
			}

			@Override
			protected HttpEntity asHttpEntity(FluidType media)
					throws IOException {
				String mediaType = toChannelMedia(media);
				return new ReadableHttpEntityChannel(mediaType, getSize(result,
						ftype.as(mediaType).getCharset()), asChannel(media));
			}

			@Override
			protected String toCharSequenceMedia(FluidType media) {
				return ftype.as(media).preferred();
			}

			@Override
			protected CharSequence asCharSequence(FluidType media) throws OpenRDFException,
					IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				return result;
			}

			@Override
			protected String toReaderMedia(FluidType media) {
				return ftype.as(media).preferred();
			}

			@Override
			protected Reader asReader(FluidType media) throws OpenRDFException,
					IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				return new CharSequenceReader(result);
			}

			@Override
			protected void writeTo(Writer writer, FluidType media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				writer.append(result);
			}

			public String toString() {
				return String.valueOf(result);
			}
		};
	}

	String getMediaType(FluidType ftype) {
		ftype = ftype.as("text/plain", "text/*");
		String mimeType = ftype.preferred();
		if (mimeType == null)
			return mimeType;
		Charset charset = ftype.getCharset();
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		if (mimeType.contains("charset="))
			return mimeType;
		return mimeType + ";charset=" + charset.name();
	}

	long getSize(CharSequence result, Charset charset) {
		if (result == null)
			return 0;
		if (charset == null && SINGLE_BYTE)
			return result.length();
		if (charset == null)
			return Charset.defaultCharset().encode(result.toString()).limit();
		return charset.encode(result.toString()).limit();
	}

	ReadableByteChannel write(FluidType mtype, CharSequence result, String base)
			throws IOException {
		Charset charset = mtype.getCharset();
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		if (result == null)
			return ChannelUtil.newChannel(new byte[0]);
		return ChannelUtil.newChannel(result.toString().getBytes(charset));
	}
}
