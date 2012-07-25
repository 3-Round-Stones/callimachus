package org.callimachusproject.fluid;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.http.HttpEntity;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

public interface Fluid {

	HttpEntity asHttpEntity(String media) throws IOException, OpenRDFException,
			XMLStreamException, TransformerException,
			ParserConfigurationException;

	ReadableByteChannel asChannel(String media) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException;

	boolean isProducible(String media, Class<?> ctype, Type gtype);

	/**
	 * Must close InputStream or return an object that will later close the
	 * InputStream.
	 */
	<T> T produce(String media, Class<T> ctype, Type gtype)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
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
