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
package org.callimachusproject.fluid.producers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.Producer;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.xslt.XMLEventReaderFactory;
import org.openrdf.repository.object.ObjectConnection;

/**
 * Converts an InputStream into a XMLEventReader.
 */
public class XMLEventMessageReader implements Producer<XMLEventReader> {
	private XMLEventReaderFactory factory = XMLEventReaderFactory.newInstance();

	public boolean isReadable(FluidType mtype, ObjectConnection con) {
		String mediaType = mtype.getMediaType();
		if (mediaType != null && !mediaType.startsWith("text/")
				&& !mediaType.startsWith("application/")
				&& !mediaType.startsWith("*/"))
			return false;
		return mtype.asClass().isAssignableFrom(XMLEventReader.class);
	}

	public XMLEventReader readFrom(FluidType mtype, ObjectConnection con,
			ReadableByteChannel in, Charset charset, String base, String location) throws IOException,
			XMLStreamException {
		if (in == null)
			return null;
		InputStream in1 = ChannelUtil.newInputStream(in);
		if (charset == null && location != null)
			return factory.createXMLEventReader(location, in1);
		if (charset == null)
			return factory.createXMLEventReader(in1);
		return factory.createXMLEventReader(in1, charset.name());
	}
}
