/*
 * Copyright 2009-2010, Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.writers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.http.object.util.MessageType;
import org.openrdf.repository.object.RDFObject;

/**
 * Writes RDF Object datatypes.
 * 
 * @author James Leigh
 * 
 */
public class DatatypeWriter implements MessageBodyWriter<Object> {
	private StringBodyWriter delegate = new StringBodyWriter();

	public boolean isText(MessageType mtype) {
		return delegate.isText(mtype.as(String.class));
	}

	public long getSize(MessageType mtype, Object result, Charset charset) {
		if (result == null)
			return 0;
		String label = mtype.getObjectFactory().createLiteral(result)
				.getLabel();
		return delegate.getSize(mtype.as(String.class), label, charset);
	}

	public boolean isWriteable(MessageType mtype) {
		Class<?> type = mtype.clas();
		if (Set.class.equals(type))
			return false;
		if (Object.class.equals(type))
			return false;
		if (RDFObject.class.isAssignableFrom(type))
			return false;
		if (type.isArray() && Byte.TYPE.equals(type.getComponentType()))
			return false;
		if (!delegate.isWriteable(mtype.as(String.class)))
			return false;
		return mtype.isDatatype(type);
	}

	public String getContentType(MessageType mtype, Charset charset) {
		return delegate.getContentType(mtype.as(String.class), charset);
	}

	public ReadableByteChannel write(MessageType mtype, Object result,
			String base, Charset charset) throws IOException, OpenRDFException,
			XMLStreamException, TransformerException,
			ParserConfigurationException {
		if (result == null)
			return ChannelUtil.emptyChannel();
		String label = mtype.getObjectFactory().createLiteral(result)
				.getLabel();
		return delegate.write(mtype.as(String.class), label, base, charset);
	}

	public void writeTo(MessageType mtype, Object object, String base,
			Charset charset, OutputStream out, int bufSize) throws IOException {
		String label = mtype.getObjectFactory().createLiteral(object)
				.getLabel();
		delegate.writeTo(mtype, label, base, charset, out, bufSize);
	}

}
