package org.callimachusproject.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import javax.xml.transform.sax.SAXSource;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class XdmNodeFactory {
	private final Processor processor;
	private final InputSourceResolver resolver;

	public XdmNodeFactory(Processor processor) {
		this.processor = processor;
		this.resolver = new InputSourceResolver();
	}

	public InputSourceResolver getInputSourceResolver() {
		return resolver;
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
            return builder.build(source);
        } catch (SaxonApiException e) {
        	throw new SAXException(e);
        }
    
	}
}
