package org.callimachusproject.fluid.producers;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.Producer;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

public class VoidReader implements Producer {

	public boolean isProducable(FluidType ftype, FluidBuilder builder) {
		return ftype.is(Void.class) || ftype.is(Void.TYPE);
	}

	public Object produce(FluidType ftype, ReadableByteChannel in,
			Charset charset, String base, FluidBuilder builder)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException {
		try {
			return null;
		} finally {
			if (in != null) {
				in.close();
			}
		}
	}

}
