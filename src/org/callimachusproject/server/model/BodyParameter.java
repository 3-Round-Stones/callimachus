/*
 * Copyright (c) 2009, Zepheira All rights reserved.
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
package org.callimachusproject.server.model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.exceptions.UnsupportedMediaType;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.xml.sax.SAXException;

/**
 * Wraps an HttpEntity is a parameter.
 */
public abstract class BodyParameter implements Parameter {
	private final String mimeType;
	private final boolean stream;
	private final String base;
	private final FluidBuilder fb;

	public BodyParameter(boolean stream, String base, String mimeType,
			ObjectConnection con) {
		this.mimeType = mimeType;
		this.stream = stream;
		this.base = base;
		this.fb = FluidFactory.getInstance().builder(con);
	}

	public void close() throws IOException {
		// allow subclasses to override
	}

	public String getContentType() {
		return mimeType;
	}

	public String getMediaType(FluidType ftype) {
		if (!stream && mimeType == null)
			return fb.media().toMedia(ftype);
		return fb.media(mimeType).toMedia(ftype);
	}

	public Object read(FluidType ftype)
			throws TransformerConfigurationException, OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerException, URISyntaxException {
		if (!stream && mimeType == null)
			return null;
		ReadableByteChannel in = getReadableByteChannel();
		Fluid reader = fb.channel(in, base, mimeType);
		if (reader.toMedia(ftype) == null)
			throw new UnsupportedMediaType();
		return reader.as(ftype);
	}

	protected abstract ReadableByteChannel getReadableByteChannel()
			throws IOException;

}
