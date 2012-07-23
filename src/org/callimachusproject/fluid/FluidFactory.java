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
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.consumers.BooleanMessageWriter;
import org.callimachusproject.fluid.consumers.ByteArrayMessageWriter;
import org.callimachusproject.fluid.consumers.ByteArrayStreamMessageWriter;
import org.callimachusproject.fluid.consumers.DOMMessageWriter;
import org.callimachusproject.fluid.consumers.DatatypeWriter;
import org.callimachusproject.fluid.consumers.DocumentFragmentMessageWriter;
import org.callimachusproject.fluid.consumers.FormMapMessageWriter;
import org.callimachusproject.fluid.consumers.FormStringMessageWriter;
import org.callimachusproject.fluid.consumers.GraphMessageWriter;
import org.callimachusproject.fluid.consumers.HttpMessageWriter;
import org.callimachusproject.fluid.consumers.InputStreamBodyWriter;
import org.callimachusproject.fluid.consumers.ModelMessageWriter;
import org.callimachusproject.fluid.consumers.PrimitiveBodyWriter;
import org.callimachusproject.fluid.consumers.RDFObjectURIWriter;
import org.callimachusproject.fluid.consumers.RDFObjectWriter;
import org.callimachusproject.fluid.consumers.ReadableBodyWriter;
import org.callimachusproject.fluid.consumers.ReadableByteChannelBodyWriter;
import org.callimachusproject.fluid.consumers.SetOfRDFObjectWriter;
import org.callimachusproject.fluid.consumers.StringBodyWriter;
import org.callimachusproject.fluid.consumers.TupleMessageWriter;
import org.callimachusproject.fluid.consumers.URIListWriter;
import org.callimachusproject.fluid.consumers.XMLEventMessageWriter;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.util.MessageType;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;

/**
 * Delegates to other {@link Consumer}s.
 * 
 * @author James Leigh
 * 
 */
public class FluidFactory {
	private static FluidFactory instance = new FluidFactory();
	static {
		try {
			instance.init();
		} catch (TransformerConfigurationException e) {
			throw new AssertionError(e);
		}
	}

	public static FluidFactory getInstance() {
		return instance;
	}

	private List<Consumer> writers = new ArrayList<Consumer>();

	private FluidFactory() {
		super();
	}

	private void init() throws TransformerConfigurationException {
		writers.add(new RDFObjectURIWriter());
		writers.add(new BooleanMessageWriter());
		writers.add(new ModelMessageWriter());
		writers.add(new GraphMessageWriter());
		writers.add(new TupleMessageWriter());
		writers.add(new DatatypeWriter());
		writers.add(new SetOfRDFObjectWriter());
		writers.add(new RDFObjectWriter());
		writers.add(new StringBodyWriter());
		writers.add(new PrimitiveBodyWriter());
		writers.add(new HttpMessageWriter());
		writers.add(new InputStreamBodyWriter());
		writers.add(new ReadableBodyWriter());
		writers.add(new ReadableByteChannelBodyWriter());
		writers.add(new XMLEventMessageWriter());
		writers.add(new ByteArrayMessageWriter());
		writers.add(new ByteArrayStreamMessageWriter());
		writers.add(new DOMMessageWriter());
		writers.add(new DocumentFragmentMessageWriter());
		writers.add(new FormMapMessageWriter());
		writers.add(new FormStringMessageWriter());
		writers.add(new URIListWriter(String.class));
		writers.add(new URIListWriter(URI.class));
		writers.add(new URIListWriter(URL.class));
		writers.add(new URIListWriter(java.net.URI.class));
	}

	private boolean isText(MessageType mtype) {
		Consumer writer;
		writer = findRawWriter(mtype);
		if (writer != null)
			return writer.isText(mtype);
		if (writer == null && mtype.isSet()) {
			writer = findComponentWriter(mtype);
		}
		String mimeType = mtype.getMimeType();
		Type genericType = mtype.type();
		if (writer == null && mimeType == null)
			throw new BadRequest("Cannot write " + genericType);
		if (writer == null)
			throw new BadRequest("Cannot write " + genericType + " into "
					+ mimeType);
		return writer.isText(mtype.component());
	}

	private String getContentType(MessageType mtype, Charset charset) {
		Consumer writer;
		writer = findRawWriter(mtype);
		if (writer != null)
			return writer.getContentType(mtype, charset);
		if (writer == null && mtype.isSet()) {
			writer = findComponentWriter(mtype);
		}
		String mimeType = mtype.getMimeType();
		Type genericType = mtype.type();
		if (writer == null && mimeType == null)
			throw new BadRequest("Cannot write " + genericType);
		if (writer == null)
			throw new BadRequest("Cannot write " + genericType + " into "
					+ mimeType);
		return writer.getContentType(mtype.component(), charset);
	}

	private long getSize(MessageType mtype, Object result, Charset charset) {
		Consumer writer;
		writer = findRawWriter(mtype);
		if (writer != null)
			return writer.getSize(mtype, result, charset);
		if (writer == null && mtype.isSet()) {
			writer = findComponentWriter(mtype);
		}
		String mimeType = mtype.getMimeType();
		Type genericType = mtype.type();
		if (writer == null)
			throw new BadRequest("Cannot write " + genericType + " into "
					+ mimeType);
		if (((Set) result).isEmpty()) {
			result = null;
		} else {
			result = ((Set) result).toArray()[0];
		}
		return writer.getSize(mtype.component(), result, charset);
	}

	public boolean isWriteable(MessageType mtype) {
		return findWriter(mtype) != null;
	}

	public Fluid consume(MessageType mtype, Object result,
			String base, Charset charset) {
		Consumer writer;
		writer = findRawWriter(mtype);
		if (writer != null)
			return fluid(writer, mtype, result, base, charset);
		if (writer == null && mtype.isSet()) {
			writer = findComponentWriter(mtype);
		}
		String mimeType = mtype.getMimeType();
		Type genericType = mtype.type();
		if (writer == null)
			throw new BadRequest("Cannot write " + genericType + " into "
					+ mimeType);
		if (((Set) result).isEmpty()) {
			result = null;
		} else {
			result = ((Set) result).toArray()[0];
		}
		return fluid(writer, mtype.component(), result, base, charset);
	}

	private Fluid fluid(final Consumer writer, MessageType mtype,
			final Object result, final String base, final Charset charset) {
		String contentType = writer.getContentType(mtype, charset);
		final MessageType media = mtype.as(contentType);
		long size = writer.getSize(media, result, charset);
		return new AbstractFluid(contentType, size, writer.isText(mtype)) {
			public ReadableByteChannel asChannel() throws IOException, OpenRDFException, XMLStreamException, TransformerException, ParserConfigurationException {
				return writer.write(media, result, base, charset);
			}
		};
	}

	private Consumer findWriter(MessageType mtype) {
		Class<?> type = mtype.clas();
		Consumer writer;
		writer = findRawWriter(mtype);
		if (writer != null)
			return writer;
		if (Set.class.equals(type))
			return findComponentWriter(mtype);
		return null;
	}

	private Consumer findRawWriter(MessageType mtype) {
		for (Consumer w : writers) {
			if (w.isWriteable(mtype)) {
				return w;
			}
		}
		return null;
	}

	private Consumer findComponentWriter(MessageType mtype) {
		if (mtype.isSet()) {
			return findRawWriter(mtype.component());
		}
		return null;
	}

}
