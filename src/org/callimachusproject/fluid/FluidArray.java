/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
