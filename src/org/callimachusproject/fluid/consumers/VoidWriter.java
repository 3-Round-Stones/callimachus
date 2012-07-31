package org.callimachusproject.fluid.consumers;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.Vapor;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

public class VoidWriter implements Consumer<Void> {

	@Override
	public boolean isConsumable(FluidType ftype, FluidBuilder builder) {
		return ftype.is(Void.class) || ftype.is(Void.TYPE);
	}

	@Override
	public Fluid consume(Void result, final String base, final FluidType ftype,
			FluidBuilder builder) {
		return new Vapor() {

			public String getSystemId() {
				return base;
			}

			public FluidType getFluidType() {
				return ftype;
			}

			public void asVoid() throws OpenRDFException, IOException,
					XMLStreamException, ParserConfigurationException,
					SAXException, TransformerConfigurationException,
					TransformerException {
				// no-op
			}

			@Override
			protected String toChannelMedia(FluidType media) {
				return ftype.as(media).preferred();
			}

			@Override
			protected ReadableByteChannel asChannel(FluidType media)
					throws OpenRDFException, IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerConfigurationException, TransformerException {
				return null;
			}
		};
	}

}
