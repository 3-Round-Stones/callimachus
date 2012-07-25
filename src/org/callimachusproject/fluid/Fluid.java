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

	String toHttpEntityMedia(String media);

	HttpEntity asHttpEntity(String media) throws IOException, OpenRDFException,
			XMLStreamException, TransformerException,
			ParserConfigurationException;

	String toChannelMedia(String media);

	ReadableByteChannel asChannel(String media) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException;

	String toMedia(Type gtype, String media);

	/**
	 * Must close InputStream or return an object that will later close the
	 * InputStream.
	 */
	Object as(Type gtype, String media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException, URISyntaxException;

	String toMedia(FluidType ftype);

	/**
	 * Must close InputStream or return an object that will later close the
	 * InputStream.
	 */
	Object as(FluidType ftype) throws OpenRDFException, IOException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException,
			URISyntaxException;
}
