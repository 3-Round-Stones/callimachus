/*
 * Copyright (c) 2009-2011, James Leigh All rights reserved.
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
package org.callimachusproject.fluid;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.model.ReadableHttpEntityChannel;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.xml.sax.SAXException;

/**
 * Delegates to other {@link Consumer}s.
 * 
 * @author James Leigh
 * 
 */
public class FluidBuilder {
	private final List<Consumer> consumers;
	private List<Producer> producers;
	private final ObjectConnection con;

	public FluidBuilder(List<Consumer> consumers, List<Producer> producers,
			ObjectConnection con) {
		assert consumers != null;
		assert producers != null;
		assert con != null;
		this.consumers = consumers;
		this.producers = producers;
		this.con = con;
	}

	@Override
	public String toString() {
		return con.toString();
	}

	public ObjectConnection getObjectConnection() {
		return con;
	}

	public boolean isConsumable(Type gtype, String media) {
		return isConsumable(new FluidType(gtype, media));
	}

	public boolean isConsumable(FluidType mtype) {
		return findWriter(mtype) != null;
	}

	public Fluid nil(String media) {
		return channel(media, null, null);
	}

	public Fluid nil(Type gtype, String media) {
		return consume(new FluidType(gtype, media), null, null);
	}

	public Fluid uri(String uri, String base) {
		return consume(new FluidType(String.class, "text/uri-list"), uri, base);
	}

	public Fluid channel(final String media, final ReadableByteChannel in,
			final String base) {
		return new AbstractFluid(this) {
			public HttpEntity asHttpEntity(String mediaType)
					throws IOException, OpenRDFException, XMLStreamException,
					TransformerException, ParserConfigurationException {
				return new ReadableHttpEntityChannel(mediaType, -1, in);
			}

			@Override
			public boolean isProducible(FluidType mtype) {
				return findReader(mtype) != null;
			}

			@Override
			public Object produce(FluidType mtype)
					throws TransformerConfigurationException, OpenRDFException,
					IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerException, URISyntaxException {
				HttpEntity entity = asHttpEntity(mtype.getMediaType());
				String contentType = entity.getContentType().getValue();
				ReadableByteChannel in = asChannel(contentType);
				Charset charset = new FluidType(ReadableByteChannel.class,
						contentType).getCharset();
				Producer reader = findRawReader(mtype);
				if (reader != null)
					return reader.readFrom(mtype, con, in, charset, base, null);
				if (mtype.isSet()) {
					reader = findComponentReader(mtype);
					if (reader == null && in == null)
						return Collections.emptySet();
				}
				Class<? extends Object> type = mtype.asClass();
				if (reader == null && !type.isPrimitive() && in == null)
					return null;
				String mime = mtype.getMediaType();
				Type genericType = mtype.asType();
				if (reader == null)
					throw new BadRequest("Cannot read " + mime + " into "
							+ genericType);
				Object o = reader.readFrom(mtype.component(), con, in, charset,
						base, null);
				if (o == null)
					return Collections.emptySet();
				if (o instanceof Set)
					return o;
				return Collections.singleton(o);
			}

			private Producer findReader(FluidType mtype) {
				Producer reader = findRawReader(mtype);
				if (reader != null)
					return reader;
				if (mtype.isSet())
					return findComponentReader(mtype);
				return null;
			}

			private Producer findRawReader(FluidType mtype) {
				for (Producer reader : producers) {
					if (reader.isReadable(mtype, con)) {
						return reader;
					}
				}
				return null;
			}

			private Producer findComponentReader(FluidType mtype) {
				return findRawReader(mtype.component());
			}
		};
	}

	public Fluid stream(final String media, final InputStream in,
			final String base) {
		return channel(media, ChannelUtil.newChannel(in), base);
	}

	public Fluid consume(Type gtype, String media, Object result, String base) {
		return consume(new FluidType(gtype, media), result, base);
	}

	public Fluid consume(FluidType mtype, Object result, String base) {
		Consumer writer;
		writer = findRawWriter(mtype);
		if (writer != null)
			return writer.consume(mtype, result, base, this);
		if (writer == null && mtype.isSet()) {
			writer = findComponentWriter(mtype);
		}
		String mimeType = mtype.getMediaType();
		Type genericType = mtype.asType();
		if (writer == null)
			throw new BadRequest("Cannot write " + genericType + " into "
					+ mimeType);
		if (((Set) result).isEmpty()) {
			result = null;
		} else {
			result = ((Set) result).toArray()[0];
		}
		return writer.consume(mtype.component(), result, base, this);
	}

	private Consumer findWriter(FluidType mtype) {
		Class<?> type = mtype.asClass();
		Consumer writer;
		writer = findRawWriter(mtype);
		if (writer != null)
			return writer;
		if (Set.class.equals(type))
			return findComponentWriter(mtype);
		return null;
	}

	private Consumer findRawWriter(FluidType mtype) {
		for (Consumer w : consumers) {
			if (w.isConsumable(mtype, this)) {
				return w;
			}
		}
		return null;
	}

	private Consumer findComponentWriter(FluidType mtype) {
		if (mtype.isSet()) {
			return findRawWriter(mtype.component());
		}
		return null;
	}

}
