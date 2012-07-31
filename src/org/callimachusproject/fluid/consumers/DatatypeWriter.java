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

import java.util.Set;

import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.openrdf.repository.object.RDFObject;

/**
 * Writes RDF Object datatypes.
 * 
 * @author James Leigh
 * 
 */
public class DatatypeWriter implements Consumer<Object> {
	private StringBodyWriter delegate = new StringBodyWriter();

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		Class<?> type = mtype.asClass();
		if (Set.class.equals(type))
			return false;
		if (Object.class.equals(type))
			return false;
		if (RDFObject.class.isAssignableFrom(type))
			return false;
		if (type.isArray() && Byte.TYPE.equals(type.getComponentType()))
			return false;
		if (!delegate.isConsumable(mtype.as(String.class), builder))
			return false;
		return builder.isDatatype(type);
	}

	public Fluid consume(Object result, String base, FluidType ftype,
			FluidBuilder builder) {
		String label = asString(result, builder);
		return delegate.consume(label, base, ftype.as(String.class), builder);
	}

	private String asString(Object result, FluidBuilder builder) {
		if (result == null)
			return null;
		return builder.getObjectConnection().getObjectFactory()
				.createLiteral(result).getLabel();
	}

}
