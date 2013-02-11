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
import java.io.InputStreamReader;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.Producer;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.xml.DocumentFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Parses a DOM Node from an InputStream.
 */
public class DOMMessageReader implements Producer {

	private DocumentFactory builder = DocumentFactory.newInstance();

	public boolean isProducable(FluidType ftype, FluidBuilder builder) {
		Class<?> type = ftype.asClass();
		if (!ftype.isXML())
			return false;
		return type.isAssignableFrom(Document.class)
				|| type.isAssignableFrom(Element.class);
	}

	public Node produce(FluidType ftype, ReadableByteChannel cin,
			Charset charset, String base, FluidBuilder builder)
			throws TransformerConfigurationException, TransformerException,
			ParserConfigurationException, IOException, SAXException {
		InputStream in = ChannelUtil.newInputStream(cin);
		Document doc = createDocument(in, charset);
		if (doc == null || ftype.asClass().isAssignableFrom(Document.class))
			return doc;
		return doc.getDocumentElement();
	}

	private Document createDocument(InputStream in, Charset charset) throws ParserConfigurationException, SAXException,
			IOException {
		try {
			if (in == null)
				return null;
			if (charset == null)
				return builder.parse(in);
			return builder.parse(new InputStreamReader(in, charset));
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}
}
