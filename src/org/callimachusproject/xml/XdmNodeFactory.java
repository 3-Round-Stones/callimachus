package org.callimachusproject.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXSource;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class XdmNodeFactory implements EntityResolver, URIResolver {
	private static final String XML_MEDIA = "application/xml, application/xslt+xml, text/xml, text/xsl";
	private final Processor processor;
	private final InputSourceResolver resolver;

	public XdmNodeFactory(String systemId, Processor processor) {
		this.processor = processor;
		this.resolver = new InputSourceResolver(systemId, XML_MEDIA);
	}

	public Source resolve(String href, String base) throws TransformerException {
		return resolver.resolve(href, base);
	}

	public InputSource resolveEntity(String publicId, String systemId)
			throws IOException {
		return resolver.resolveEntity(publicId, systemId);
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
        reader.setEntityResolver(resolver);
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
