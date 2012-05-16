/*
 * Copyright (c) 2012 3 Round Stones Inc., Some rights reserved.
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
package org.callimachusproject.xslt;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DocumentFactory {
	private static final String LOAD_EXTERNAL_DTD = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
	private static final Logger logger = LoggerFactory
			.getLogger(DocumentFactory.class);

	public static DocumentFactory newInstance() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(true);
		factory.setIgnoringComments(false);
		factory.setIgnoringElementContentWhitespace(false);
		try {
			factory.setFeature(LOAD_EXTERNAL_DTD, false);
		} catch (ParserConfigurationException e) {
			logger.warn(e.toString(), e);
		}
		return new DocumentFactory(factory);
	}

	private final DocumentBuilderFactory factory;

	protected DocumentFactory(DocumentBuilderFactory builder) {
		this.factory = builder;
	}

	public Document newDocument() throws ParserConfigurationException {
		return factory.newDocumentBuilder().newDocument();
	}

	public Document parse(InputStream in, String systemId) throws SAXException,
			IOException, ParserConfigurationException {
		return factory.newDocumentBuilder().parse(in, systemId);
	}

	public Document parse(InputStream in) throws SAXException, IOException,
			ParserConfigurationException {
		return factory.newDocumentBuilder().parse(in);
	}

	public Document parse(String url) throws SAXException, IOException,
			ParserConfigurationException {
		return factory.newDocumentBuilder().parse(url);
	}

	public Document parse(InputSource is) throws SAXException, IOException,
			ParserConfigurationException {
		return factory.newDocumentBuilder().parse(is);
	}

	public Document parse(Reader reader, String systemId) throws SAXException,
			IOException, ParserConfigurationException {
		InputSource is = new InputSource(reader);
		is.setSystemId(systemId);
		return factory.newDocumentBuilder().parse(is);
	}

	public Document parse(Reader reader) throws SAXException,
			IOException, ParserConfigurationException {
		return factory.newDocumentBuilder().parse(new InputSource(reader));
	}

}
