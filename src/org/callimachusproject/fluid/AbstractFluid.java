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
import org.callimachusproject.server.model.ReadableHttpEntityChannel;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

public abstract class AbstractFluid implements Fluid {
	private final FluidBuilder builder;

	public AbstractFluid(FluidBuilder builder) {
		this.builder = builder;
	}

	public String toHttpEntityMedia(String media) {
		return toChannelMedia(media);
	}

	public HttpEntity asHttpEntity(String media) throws IOException,
			OpenRDFException, XMLStreamException, TransformerException,
			ParserConfigurationException {
		String mediaType = toHttpEntityMedia(media);
		if (mediaType == null)
			return null;
		return new ReadableHttpEntityChannel(mediaType, -1, asChannel(mediaType));
	}

	public String toMedia(Type gtype, String media) {
		return toMedia(new FluidType(gtype, media));
	}

	public Object as(Type gtype, String media) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException, URISyntaxException {
		return as(new FluidType(gtype, media));
	}

	public String toMedia(FluidType ftype) {
		if (ftype.is(ReadableByteChannel.class))
			return toChannelMedia(ftype.getMediaType());
		if (ftype.is(HttpEntity.class))
			return toHttpEntityMedia(ftype.getMediaType());

		String media = toChannelMedia(ftype.getMediaType());
		return builder.media(media).toMedia(ftype);
	}

	public Object as(FluidType ftype) throws OpenRDFException,
			IOException, XMLStreamException, ParserConfigurationException,
			SAXException, TransformerConfigurationException,
			TransformerException, URISyntaxException {
		if (ftype.is(ReadableByteChannel.class))
			return asChannel(ftype.getMediaType());
		if (ftype.is(HttpEntity.class))
			return asHttpEntity(ftype.getMediaType());

		String media = toChannelMedia(ftype.getMediaType());
		return builder.channel(media, asChannel(media), null).as(ftype);
	}

}
