package org.callimachusproject.fluid;

import java.io.IOException;

class FluidArray extends AbstractFluid {
	private final Fluid[] fluids;
	private final Fluid sample;
	private final String systemId;
	private final FluidType ftype;

	public FluidArray(Fluid[] fluids, Fluid nil, String systemId,
			FluidType ftype) {
		assert fluids != null;
		this.fluids = fluids;
		this.sample = fluids.length > 0 ? fluids[0] : nil;
		this.systemId = systemId;
		this.ftype = ftype;
	}

	@Override
	public String getSystemId() {
		return systemId;
	}

	@Override
	public FluidType getFluidType() {
		return ftype;
	}

	@Override
	public void asVoid() throws IOException, FluidException {
		for (Fluid fluid : fluids) {
			fluid.asVoid();
		}
	}

	@Override
	public String toMedia(FluidType ftype) {
		if (ftype.isCollection())
			return sample.toMedia(ftype.component());
		return sample.toMedia(ftype);
	}

	@Override
	public Object as(FluidType ftype) throws IOException, FluidException {
		if (ftype.isCollection()) {
			Object[] result = new Object[fluids.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = fluids[i].as(ftype.component());
			}
			return ftype.castArray(result);
		} else {
			if (fluids.length == 0)
				return null;
			for (Fluid fluid : fluids) {
				if (fluid != sample) {
					fluid.asVoid();
				}
			}
			return sample.as(ftype);
		}
	}

}
