/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.Configuration;
import net.sf.saxon.lib.ModuleURIResolver;
import net.sf.saxon.lib.UnparsedTextURIResolver;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.trans.XPathException;

import org.apache.http.client.HttpClient;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class XdmNodeFactory implements EntityResolver, URIResolver, ModuleURIResolver, UnparsedTextURIResolver {
	private static final String XML_MEDIA = "application/xml, application/xslt+xml, text/xml, text/xsl";
	private static final String XQUERY_MEDIA = "application/xquery, " + XML_MEDIA;
	private final Processor processor;
	private final InputSourceResolver xmlResolver;
	private final ModuleURIResolver xqueryResolver;

	public XdmNodeFactory(Processor processor, HttpClient client) {
		this.processor = processor;
		this.xmlResolver = new InputSourceResolver(XML_MEDIA, client);
		this.xqueryResolver = new InputSourceResolver(XQUERY_MEDIA, client);
	}

	public Reader resolve(URI absoluteURI, String encoding, Configuration config)
			throws XPathException {
		return xmlResolver.resolve(absoluteURI, encoding, config);
	}

	public StreamSource resolve(String href, String base) throws TransformerException {
		return xmlResolver.resolve(href, base);
	}

	public InputSource resolveEntity(String publicId, String systemId)
			throws IOException {
		return xmlResolver.resolveEntity(publicId, systemId);
	}

	public StreamSource[] resolve(String moduleURI, String baseURI,
			String[] locations) throws XPathException {
		return xqueryResolver.resolve(moduleURI, baseURI, locations);
	}

	public XdmNode parse(String systemId) throws SAXException, IOException {
		InputSource isource = resolveEntity(null, systemId);
		if (isource == null)
			return null;
		try {
			return parse(isource);
		} finally {
			InputStream in = isource.getByteStream();
			if (in != null) {
				in.close();
			}
			Reader reader = isource.getCharacterStream();
			if (reader != null) {
				reader.close();
			}
		}
	}

	public XdmNode parse(String systemId, Reader in) throws SAXException, IOException {
		if (in == null)
			return null;
		try {
			InputSource source = new InputSource(in);
			source.setSystemId(systemId);
			return parse(source);
		} finally {
			in.close();
		}
	}

	public XdmNode parse(String systemId, InputStream in) throws SAXException, IOException {
		if (in == null)
			return null;
		try {
			InputSource source = new InputSource(in);
			source.setSystemId(systemId);
			return parse(source);
		} finally {
			in.close();
		}
	}

	private XdmNode parse(InputSource isource) throws SAXException {
        // Make sure the builder uses our entity resolver
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setEntityResolver(xmlResolver);
        SAXSource source = new SAXSource(reader, isource);
        if (isource.getSystemId() != null) {
        	source.setSystemId(isource.getSystemId());
        }
        return parse(source);
	}

	private XdmNode parse(Source source)
			throws SAXException {
		try {
            DocumentBuilder builder = processor.newDocumentBuilder();
            builder.setLineNumbering(true);
            builder.setDTDValidation(false);
            if (source.getSystemId() != null) {
            	builder.setBaseURI(URI.create(source.getSystemId()));
            }
            return builder.build(source);
        } catch (SaxonApiException e) {
        	throw new SAXException(e);
        }
	}
}
