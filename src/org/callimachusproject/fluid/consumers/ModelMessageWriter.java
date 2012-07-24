/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.FluidType;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Model;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.impl.GraphQueryResultImpl;
import org.openrdf.repository.object.ObjectConnection;

/**
 * Writes RDF from a {@link Model}.
 * 
 * @author James Leigh
 * 
 */
public class ModelMessageWriter implements Consumer<Model> {
	private GraphMessageWriter delegate;

	public ModelMessageWriter() {
		delegate = new GraphMessageWriter();
	}

	public boolean isText(FluidType mtype) {
		return delegate.isText(mtype.as(GraphQueryResult.class));
	}

	public long getSize(FluidType mtype, ObjectConnection con, Model result, Charset charset) {
		return -1;
	}

	public boolean isWriteable(FluidType mtype, ObjectConnection con) {
		if (!Model.class.isAssignableFrom((Class<?>) mtype.getClassType()))
			return false;
		return delegate.isWriteable(mtype.as(GraphQueryResult.class), con);
	}

	public String getContentType(FluidType mtype, Charset charset) {
		return delegate.getContentType(mtype.as(GraphQueryResult.class),
				charset);
	}

	public ReadableByteChannel write(FluidType mtype, ObjectConnection con,
			Model model, String base, Charset charset) throws IOException, OpenRDFException {
		GraphQueryResult result = new GraphQueryResultImpl(model
				.getNamespaces(), model);
		return delegate.write(mtype.as(GraphQueryResult.class), con, result, base,
				charset);
	}

	public String toString() {
		return delegate.toString();
	}

	public void writeTo(FluidType mtype, ObjectConnection con, Model model,
			String base, Charset charset, WritableByteChannel out, int bufSize)
			throws IOException, OpenRDFException {
		GraphQueryResult result = new GraphQueryResultImpl(model
				.getNamespaces(), model);
		delegate.writeTo(mtype.as(GraphQueryResult.class), con, result,
				base, charset, out, bufSize);
	}
}
