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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.callimachusproject.fluid.Vapor;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.server.helpers.ReadableHttpEntityChannel;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

/**
 * Writes application/x-www-form-urlencoded from {@link String} objects.
 * 
 * @author James Leigh
 * 
 */
public class FormStringMessageWriter implements Consumer<CharSequence> {

	private static final String FORM_URLENCODED = "application/x-www-form-urlencoded";

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		if (!CharSequence.class.isAssignableFrom(mtype.asClass()))
			return false;
		return mtype.is(FORM_URLENCODED);
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
				return ftype.as(FORM_URLENCODED).as(media).preferred();
			}

			@Override
			protected ReadableByteChannel asChannel(FluidType media)
					throws IOException, OpenRDFException, XMLStreamException,
					TransformerException, ParserConfigurationException {
				return write(ftype.as(FORM_URLENCODED), result, base);
			}

			@Override
			protected String toCharSequenceMedia(FluidType media) {
				return ftype.as(FORM_URLENCODED).as(media).preferred();
			}

			@Override
			protected CharSequence asCharSequence(FluidType media) throws OpenRDFException,
					IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				return result;
			}

			@Override
			protected String toHttpEntityMedia(FluidType media) {
				return toChannelMedia(media);
			}

			@Override
			protected HttpEntity asHttpEntity(FluidType media)
					throws IOException, OpenRDFException, XMLStreamException,
					TransformerException, ParserConfigurationException {
				String mediaType = toChannelMedia(media);
				return new ReadableHttpEntityChannel(mediaType, getSize(
						ftype.as(FORM_URLENCODED), result), asChannel(media));
			}

			public String toString() {
				return String.valueOf(result);
			}
		};
	}

	long getSize(FluidType mtype, CharSequence result) {
		Charset charset = mtype.getCharset();
		if (charset == null)
			return result.length(); // ISO-8859-1
		return charset.encode(result.toString()).limit();
	}

	ReadableByteChannel write(FluidType mtype, CharSequence result, String base)
			throws IOException, OpenRDFException, XMLStreamException,
			TransformerException, ParserConfigurationException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		writeTo(mtype, result, base, out, 1024);
		return ChannelUtil.newChannel(out.toByteArray());
	}

	private void writeTo(FluidType mtype, CharSequence result, String base,
			OutputStream out, int bufSize) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException {
		Charset charset = mtype.getCharset();
		if (charset == null) {
			charset = Charset.forName("ISO-8859-1");
		}
		Writer writer = new OutputStreamWriter(out, charset);
		writer.append(result);
		writer.flush();
	}

}
