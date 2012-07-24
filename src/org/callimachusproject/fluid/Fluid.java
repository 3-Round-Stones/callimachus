package org.callimachusproject.fluid;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

public interface Fluid {

	FluidType getFluidType();

	long getByteStreamSize();

	ReadableByteChannel asChannel() throws IOException, OpenRDFException, XMLStreamException, TransformerException, ParserConfigurationException;

	boolean isProducible(String media, Class<?> ctype, Type gtype);

	/**
	 * Must close InputStream or return an object that will later close the
	 * InputStream.
	 */
	Object produce(String media, Class<?> ctype, Type gtype) throws OpenRDFException, IOException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException,
			URISyntaxException;

	boolean isProducible(FluidType mtype);

	/**
	 * Must close InputStream or return an object that will later close the
	 * InputStream.
	 */
	Object produce(FluidType mtype) throws OpenRDFException, IOException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException,
			URISyntaxException;
}
