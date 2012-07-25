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
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.callimachusproject.fluid.AbstractFluid;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.model.ReadableHttpEntityChannel;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;

/**
 * Writes a {@link String}.
 * 
 * @author James Leigh
 * 
 */
public class StringBodyWriter implements Consumer<String> {
	static final boolean SINGLE_BYTE = 1f == java.nio.charset.Charset
			.defaultCharset().newEncoder().maxBytesPerChar();

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		String mimeType = mtype.getMediaType();
		if (!String.class.equals(mtype.getClassType()))
			return false;
		return mimeType == null || mimeType.startsWith("text/")
				|| mimeType.startsWith("*");
	}

	private String getMediaType(FluidType mtype, FluidBuilder builder) {
		String mimeType = mtype.getMediaType();
		Charset charset = mtype.getCharset();
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		if (mimeType == null || mimeType.startsWith("*")
				|| mimeType.startsWith("text/*")) {
			mimeType = "text/plain";
		}
		if (mimeType.contains("charset=") || !mimeType.startsWith("text/"))
			return mimeType;
		return mimeType + ";charset=" + charset.name();
	}

	public Fluid consume(final FluidType ftype, final String result, final String base,
			final FluidBuilder builder) {
		return new AbstractFluid(builder) {
			public HttpEntity asHttpEntity(String media) throws IOException,
					OpenRDFException, XMLStreamException, TransformerException,
					ParserConfigurationException {
				String mediaType = getMediaType(ftype.as(media), builder);
				FluidType as = ftype.as(mediaType);
				long size = getSize(result, as.getCharset());
				return new ReadableHttpEntityChannel(mediaType, size, write(as, result, base));
			}

			public String toString() {
				return result.toString();
			}
		};
	}

	private long getSize(String result, Charset charset) {
		if (result == null)
			return 0;
		if (charset == null && SINGLE_BYTE)
			return result.length();
		if (charset == null)
			return Charset.defaultCharset().encode(result).limit();
		return charset.encode(result).limit();
	}

	private ReadableByteChannel write(FluidType mtype, String result,
			String base) throws IOException {
		Charset charset = mtype.getCharset();
		if (charset == null) {
			charset = Charset.defaultCharset();
		}
		if (result == null)
			return ChannelUtil.newChannel(new byte[0]);
		return ChannelUtil.newChannel(result.getBytes(charset));
	}
}
