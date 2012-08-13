package org.callimachusproject.fluid.consumers;

import java.nio.channels.ReadableByteChannel;

import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.Vapor;

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

			public void asVoid() {
				// no-op
			}

			@Override
			protected String toChannelMedia(FluidType media) {
				return ftype.as(media).preferred();
			}

			@Override
			protected ReadableByteChannel asChannel(FluidType media) {
				return null;
			}
		};
	}

}
