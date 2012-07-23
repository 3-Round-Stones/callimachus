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

import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import org.callimachusproject.fluid.consumers.base.ResultMessageWriterBase;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryResultUtil;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.resultio.TupleQueryResultFormat;
import org.openrdf.query.resultio.TupleQueryResultWriterFactory;
import org.openrdf.query.resultio.TupleQueryResultWriterRegistry;
import org.openrdf.repository.object.ObjectConnection;

/**
 * Writes tuple results.
 * 
 * @author James Leigh
 * 
 */
public class TupleMessageWriter
		extends
		ResultMessageWriterBase<TupleQueryResultFormat, TupleQueryResultWriterFactory, TupleQueryResult> {

	public TupleMessageWriter() {
		super(TupleQueryResultWriterRegistry.getInstance(),
				TupleQueryResult.class);
	}

	@Override
	public void writeTo(TupleQueryResultWriterFactory factory,
			TupleQueryResult result, WritableByteChannel out, Charset charset,
			String base, ObjectConnection con) throws TupleQueryResultHandlerException,
			QueryEvaluationException {
		QueryResultUtil.report(result, factory.getWriter(ChannelUtil
				.newOutputStream(out)));
	}

	@Override
	protected Charset getCharset(TupleQueryResultFormat format, Charset charset) {
		return format.getCharset();
	}

}
