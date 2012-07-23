package org.callimachusproject.fluid;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.server.util.MessageType;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

public interface Fluid {

	boolean isText();

	long getByteStreamSize();

	String getContentType();

	ReadableByteChannel asChannel() throws IOException, OpenRDFException, XMLStreamException, TransformerException, ParserConfigurationException;

	boolean isReadable(MessageType mtype);

	/**
	 * Must close InputStream or return an object that will later close the
	 * InputStream.
	 */
	Object produce(MessageType mtype, ReadableByteChannel in, Charset charset,
			String base, String location) throws OpenRDFException, IOException,
			XMLStreamException, ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException,
			URISyntaxException;
}
