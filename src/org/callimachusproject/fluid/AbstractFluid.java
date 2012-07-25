package org.callimachusproject.fluid;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

public abstract class AbstractFluid implements Fluid {
	private final FluidBuilder builder;

	public AbstractFluid(FluidBuilder builder) {
		this.builder = builder;
	}

	public ReadableByteChannel asChannel(String media) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException {
		return ChannelUtil.newChannel(asHttpEntity(media).getContent());
	}

	public boolean isProducible(String media, Class<?> ctype, Type gtype) {
		return isProducible(new FluidType(media, ctype, gtype));
	}

	public <T> T produce(String media, Class<T> ctype, Type gtype)
			throws OpenRDFException, IOException, XMLStreamException,
			ParserConfigurationException, SAXException,
			TransformerConfigurationException, TransformerException,
			URISyntaxException {
		return (T) produce(new FluidType(media, ctype, gtype));
	}

	public boolean isProducible(FluidType mtype) {
		String media = mtype.getMediaType();
		return builder.nil(media).isProducible(mtype);
	}

	public Object produce(FluidType mtype) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException, URISyntaxException {
		String media = mtype.getMediaType();
		InputStream in = asHttpEntity(media).getContent();
		return builder.stream(media, in, null).produce(mtype);
	}

}
