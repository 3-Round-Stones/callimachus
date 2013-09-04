package org.callimachusproject.io;

import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URISyntaxException;

import javax.xml.stream.XMLStreamException;

import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.RDFWriterFactory;
import org.openrdf.rio.rdfxml.RDFXMLParserFactory;

public class RDFXMLStreamWriterTest extends RDFWriterTestCase {

	public RDFXMLStreamWriterTest() {
		super(new RDFWriterFactory() {
			public RDFFormat getRDFFormat() {
				return RDFFormat.RDFXML;
			}

			public RDFWriter getWriter(OutputStream out) {
				try {
					return new RDFXMLStreamWriterFactory().createWriter(out,
							"http://example.com/");
				} catch (XMLStreamException e) {
					throw new UndeclaredThrowableException(e);
				} catch (URISyntaxException e) {
					throw new UndeclaredThrowableException(e);
				}
			}

			public RDFWriter getWriter(Writer writer) {
				try {
					return new RDFXMLStreamWriterFactory().createWriter(writer,
							"http://example.com/");
				} catch (XMLStreamException e) {
					throw new UndeclaredThrowableException(e);
				} catch (URISyntaxException e) {
					throw new UndeclaredThrowableException(e);
				}
			}
		}, new RDFXMLParserFactory());
	}

}
