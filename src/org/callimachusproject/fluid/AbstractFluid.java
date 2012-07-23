/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.producers.BooleanMessageReader;
import org.callimachusproject.fluid.producers.ByteArrayMessageReader;
import org.callimachusproject.fluid.producers.ByteArrayStreamMessageReader;
import org.callimachusproject.fluid.producers.DOMMessageReader;
import org.callimachusproject.fluid.producers.DatatypeReader;
import org.callimachusproject.fluid.producers.DocumentFragmentMessageReader;
import org.callimachusproject.fluid.producers.FormMapMessageReader;
import org.callimachusproject.fluid.producers.FormStringMessageReader;
import org.callimachusproject.fluid.producers.GraphMessageReader;
import org.callimachusproject.fluid.producers.HttpMessageReader;
import org.callimachusproject.fluid.producers.InputStreamBodyReader;
import org.callimachusproject.fluid.producers.ModelMessageReader;
import org.callimachusproject.fluid.producers.NetURIReader;
import org.callimachusproject.fluid.producers.PrimitiveBodyReader;
import org.callimachusproject.fluid.producers.RDFObjectReader;
import org.callimachusproject.fluid.producers.RDFObjectURIReader;
import org.callimachusproject.fluid.producers.ReadableBodyReader;
import org.callimachusproject.fluid.producers.ReadableByteChannelBodyReader;
import org.callimachusproject.fluid.producers.SetOfRDFObjectReader;
import org.callimachusproject.fluid.producers.StringBodyReader;
import org.callimachusproject.fluid.producers.StringURIReader;
import org.callimachusproject.fluid.producers.TupleMessageReader;
import org.callimachusproject.fluid.producers.URIReader;
import org.callimachusproject.fluid.producers.URLReader;
import org.callimachusproject.fluid.producers.XMLEventMessageReader;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.util.MessageType;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

/**
 * Delegates to other {@link Producer}.
 * 
 * @author James Leigh
 * 
 */
public abstract class AbstractFluid implements Fluid {
	private static AbstractFluid instance = new AbstractFluid(null){
	public ReadableByteChannel asChannel() {
		throw new UnsupportedOperationException();
	}};

	public static AbstractFluid getInstance() {
		return instance;
	}

	private static List<Producer> readers = new ArrayList<Producer>();

	static {
		readers.add(new URIReader());
		readers.add(new URLReader());
		readers.add(new StringURIReader());
		readers.add(new NetURIReader());
		readers.add(new RDFObjectURIReader());
		readers.add(new ModelMessageReader());
		readers.add(new GraphMessageReader());
		readers.add(new TupleMessageReader());
		readers.add(new BooleanMessageReader());
		readers.add(new DatatypeReader());
		readers.add(new SetOfRDFObjectReader());
		readers.add(new RDFObjectReader());
		readers.add(new StringBodyReader());
		readers.add(new PrimitiveBodyReader());
		readers.add(new FormMapMessageReader());
		readers.add(new FormStringMessageReader());
		readers.add(new HttpMessageReader());
		readers.add(new InputStreamBodyReader());
		readers.add(new ReadableBodyReader());
		readers.add(new ReadableByteChannelBodyReader());
		readers.add(new XMLEventMessageReader());
		readers.add(new ByteArrayMessageReader());
		readers.add(new ByteArrayStreamMessageReader());
		readers.add(new DOMMessageReader());
		readers.add(new DocumentFragmentMessageReader());
	}
	private final String contentType;
	private final long size;
	private final boolean text;

	public AbstractFluid(String contentType) {
		this(contentType, -1, false);
	}

	public AbstractFluid(String contentType, long size) {
		this(contentType, size, false);
		}

	public AbstractFluid(String contentType, boolean text) {
		this(contentType, -1, text);
		}

	public AbstractFluid(String contentType, long size, boolean text) {
		this.contentType = contentType;
		this.size = size;
		this.text = text;
	}

	public boolean isText() {
		return text;
	}

	public long getByteStreamSize() {
		return size;
	}

	public String getContentType() {
		return contentType;
	}

	public abstract ReadableByteChannel asChannel() throws IOException, OpenRDFException, XMLStreamException, TransformerException, ParserConfigurationException;

	public boolean isReadable(MessageType mtype) {
		return findReader(mtype) != null;
	}

	public Object produce(MessageType mtype, ReadableByteChannel in,
			Charset charset, String base, String location)
			throws TransformerConfigurationException, OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, URISyntaxException {
		Producer reader = findRawReader(mtype);
		if (reader != null)
			return reader.readFrom(mtype, in, charset, base, location);
		if (mtype.isSet()) {
			reader = findComponentReader(mtype);
			if (reader == null && location == null && in == null)
				return Collections.emptySet();
		}
		Class<? extends Object> type = mtype.clas();
		if (reader == null && !type.isPrimitive() && location == null
				&& in == null)
			return null;
		String mime = mtype.getMimeType();
		Type genericType = mtype.type();
		if (reader == null)
			throw new BadRequest("Cannot read " + mime + " into " + genericType);
		Object o = reader.readFrom(mtype.component(), in, charset, base,
				location);
		if (o == null)
			return Collections.emptySet();
		if (o instanceof Set)
			return o;
		return Collections.singleton(o);
	}

	private Producer findReader(MessageType mtype) {
		Producer reader = findRawReader(mtype);
		if (reader != null)
			return reader;
		if (mtype.isSet())
			return findComponentReader(mtype);
		return null;
	}

	private Producer findRawReader(MessageType mtype) {
		for (Producer reader : readers) {
			if (reader.isReadable(mtype)) {
				return reader;
			}
		}
		return null;
	}

	private Producer findComponentReader(MessageType mtype) {
		return findRawReader(mtype.component());
	}

}
