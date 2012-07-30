package org.callimachusproject.fluid.consumers;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.fluid.AbstractFluid;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;

public class HttpEntityWriter implements Consumer<HttpEntity> {

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		return mtype.is(HttpEntity.class);
	}

	public Fluid consume(final HttpEntity result, final String base,
			final FluidType ftype, final FluidBuilder builder) {
		return new AbstractFluid() {
			public String getSystemId() {
				return base;
			}

			public FluidType getFluidType() {
				return ftype;
			}

			public void asVoid() throws IOException {
				if (result != null) {
					EntityUtils.consume(result);
				}
			}

			public String toChannelMedia(String... media) {
				if (result == null)
					return ftype.as(media).preferred();
				Header hd = result.getContentType();
				if (hd == null)
					return ftype.as(media).preferred();
				return ftype.as(hd.getValue()).as(media).preferred();
			}

			public ReadableByteChannel asChannel(String... media)
					throws IOException, OpenRDFException, XMLStreamException,
					TransformerException, ParserConfigurationException {
				if (result == null)
					return null;
				return ChannelUtil.newChannel(result.getContent());
			}

			public String toString() {
				return String.valueOf(result);
			}
		};
	}

}
