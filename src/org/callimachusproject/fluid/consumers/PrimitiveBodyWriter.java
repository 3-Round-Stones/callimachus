/*
 * Copyright 2009-2010, Zepheira LLC Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.fluid.consumers;

import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;

/**
 * Writes primitives and their wrappers.
 * 
 * @author James Leigh
 * 
 */
public class PrimitiveBodyWriter implements Consumer<Object> {

	@Override
	public boolean isConsumable(FluidType ftype, FluidBuilder builder) {
		return isPrimitive(ftype.asClass()) && ftype.is("text/*");
	}

	@Override
	public Fluid consume(Object result, String base, FluidType ftype,
			FluidBuilder builder) {
		if (result == null && ftype.is(Boolean.TYPE)) {
			result = "false";
		} else if (result == null && ftype.isPrimitive()) {
			result = "0";
		}
		return builder.consume(result.toString(), base, ftype.as(String.class));
	}

	private boolean isPrimitive(Class<?> asClass) {
		return asClass.isPrimitive() || isPrimitiveWrapper(asClass);
	}

	private boolean isPrimitiveWrapper(Class<?> asClass) {
		if (Boolean.class.equals(asClass))
			return true;
		if (Byte.class.equals(asClass))
			return true;
		if (Short.class.equals(asClass))
			return true;
		if (Character.class.equals(asClass))
			return true;
		if (Integer.class.equals(asClass))
			return true;
		if (Long.class.equals(asClass))
			return true;
		if (Float.class.equals(asClass))
			return true;
		if (Double.class.equals(asClass))
			return true;
		return false;
	}

}
