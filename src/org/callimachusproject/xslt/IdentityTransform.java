package org.callimachusproject.xslt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidFactory;
import org.openrdf.OpenRDFException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.xml.sax.SAXException;

public class IdentityTransform extends TransformBuilder {
	private final FluidFactory ff = FluidFactory.getInstance();
	private final Fluid fluid;

	public <T> IdentityTransform(T object, Class<T> type) {
		this.fluid = ff.builder().consume(object, null, type,
				"application/xml", "text/xml", "image/xml", "text/xsl",
				"application/xml-external-parsed-entity");
	}

	public <T> IdentityTransform(T object, String systemId, Class<T> type) {
		this.fluid = ff.builder().consume(object, systemId, type,
				"application/xml", "text/xml", "image/xml", "text/xsl",
				"application/xml-external-parsed-entity");
	}

	@Override
	public Document asDocument() throws TransformerException, IOException {
		try {
			return fluid.asDocument();
		} catch (OpenRDFException e) {
			throw new TransformerException(e);
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} catch (SAXException e) {
			throw new TransformerException(e);
		}
	}

	@Override
	public DocumentFragment asDocumentFragment() throws TransformerException,
			IOException {
		try {
			return fluid.asDocumentFragment();
		} catch (OpenRDFException e) {
			throw new TransformerException(e);
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} catch (SAXException e) {
			throw new TransformerException(e);
		}
	}

	@Override
	public XMLEventReader asXMLEventReader() throws TransformerException,
			IOException {
		try {
			return fluid.asXMLEventReader();
		} catch (OpenRDFException e) {
			throw new TransformerException(e);
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} catch (SAXException e) {
			throw new TransformerException(e);
		}
	}

	@Override
	public InputStream asInputStream() throws TransformerException, IOException {
		try {
			return fluid.asStream();
		} catch (OpenRDFException e) {
			throw new TransformerException(e);
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} catch (SAXException e) {
			throw new TransformerException(e);
		}
	}

	@Override
	public Reader asReader() throws TransformerException, IOException {
		try {
			return fluid.asReader();
		} catch (OpenRDFException e) {
			throw new TransformerException(e);
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} catch (SAXException e) {
			throw new TransformerException(e);
		}
	}

	@Override
	public void toOutputStream(OutputStream out) throws IOException,
			TransformerException {
		try {
			fluid.streamTo(out);
		} catch (OpenRDFException e) {
			throw new TransformerException(e);
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} catch (SAXException e) {
			throw new TransformerException(e);
		}
	}

	@Override
	public void toWriter(Writer writer) throws IOException,
			TransformerException {
		try {
			fluid.writeTo(writer);
		} catch (OpenRDFException e) {
			throw new TransformerException(e);
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} catch (SAXException e) {
			throw new TransformerException(e);
		}
	}

	@Override
	public void close() throws TransformerException, IOException {
		try {
			fluid.asVoid();
		} catch (OpenRDFException e) {
			throw new TransformerException(e);
		} catch (XMLStreamException e) {
			throw new TransformerException(e);
		} catch (ParserConfigurationException e) {
			throw new TransformerException(e);
		} catch (SAXException e) {
			throw new TransformerException(e);
		}
	}

}
