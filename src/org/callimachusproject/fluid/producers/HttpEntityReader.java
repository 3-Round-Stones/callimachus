package org.callimachusproject.fluid.producers;

import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.Producer;
import org.callimachusproject.server.helpers.ReadableHttpEntityChannel;

public class HttpEntityReader implements Producer {

	public boolean isProducable(FluidType ftype, FluidBuilder builder) {
		return HttpEntity.class.equals(ftype.asClass());
	}

	public HttpEntity produce(FluidType ftype, ReadableByteChannel in,
			Charset charset, String base, FluidBuilder builder) {
		return new ReadableHttpEntityChannel(ftype.preferred(), -1, in);
	}
}
