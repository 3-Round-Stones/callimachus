/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.query.QueryResult;
import org.openrdf.repository.object.RDFObject;

/**
 * Writes RDF URI from RDFObject(s).
 * 
 * @author James Leigh
 * 
 */
public class RDFObjectURIWriter extends URIListWriter<Object> {

	public RDFObjectURIWriter() {
		super(Object.class);
	}

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		if (!super.isConsumable(mtype, builder))
			return false;
		Class<?> c = mtype.asClass();
		if (mtype.isSetOrArray()) {
			c = mtype.component().asClass();
		}
		if (QueryResult.class.isAssignableFrom(c))
			return false;
		if (Object.class.equals(c) || RDFObject.class.equals(c))
			return true;
		return builder.isConcept(c);
	}

	@Override
	protected String toString(Object result) {
		Resource resource = ((RDFObject) result).getResource();
		assert resource != null;
		if (resource instanceof URI)
			return resource.stringValue();
		return "_:" + resource.stringValue();
	}

}
