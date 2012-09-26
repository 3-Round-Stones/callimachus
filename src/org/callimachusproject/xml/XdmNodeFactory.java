package org.callimachusproject.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class XdmNodeFactory implements EntityResolver, URIResolver {
	private static final DocumentFactory df = DocumentFactory.newInstance();
	private final Processor processor;
	private final InputSourceResolver resolver;

	public XdmNodeFactory(Processor processor) {
		this.processor = processor;
		this.resolver = new InputSourceResolver();
	}

	public Source resolve(String href, String base) throws TransformerException {
		Source source = resolver.resolve(href, base);
		if (source == null) {
			try {
				// use empty node-set
				Document doc = df.newDocument();
				return new DOMSource(doc);
			} catch (ParserConfigurationException e) {
				throw new TransformerException(e);
			}
		}
		return source;
	}

	public InputSource resolveEntity(String publicId, String systemId)
			throws IOException {
		return resolver.resolveEntity(publicId, systemId);
	}

	public XdmNode parse(String systemId) throws SAXException, IOException {
		InputSource isource = resolveEntity(null, systemId);
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
        try {
            // Make sure the builder uses our entity resolver
            XMLReader reader = XMLReaderFactory.createXMLReader();
            reader.setEntityResolver(resolver);
            SAXSource source = new SAXSource(reader, isource);
            DocumentBuilder builder = processor.newDocumentBuilder();
            builder.setLineNumbering(true);
            builder.setDTDValidation(false);
            if (isource.getSystemId() != null) {
            	builder.setBaseURI(URI.create(isource.getSystemId()));
            }
            return builder.build(source);
        } catch (SaxonApiException e) {
        	throw new SAXException(e);
        }
    
	}
}
