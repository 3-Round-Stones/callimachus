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

import java.io.IOException;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import org.callimachusproject.fluid.consumers.helpers.MessageWriterBase;
import org.callimachusproject.io.ChannelUtil;
import org.openrdf.query.resultio.BooleanQueryResultFormat;
import org.openrdf.query.resultio.BooleanQueryResultWriterFactory;
import org.openrdf.query.resultio.BooleanQueryResultWriterRegistry;
import org.openrdf.repository.object.ObjectConnection;

/**
 * Writes a boolean query result.
 * 
 * @author James Leigh
 * 
 */
public class BooleanMessageWriter
		extends
		MessageWriterBase<BooleanQueryResultFormat, BooleanQueryResultWriterFactory, Boolean> {

	public BooleanMessageWriter() {
		super(BooleanQueryResultWriterRegistry.getInstance(), Boolean.class);
	}

	@Override
	public void writeTo(BooleanQueryResultWriterFactory factory,
			Boolean result, WritableByteChannel out, Charset charset,
			String base, ObjectConnection con) throws IOException {
		factory.getWriter(ChannelUtil.newOutputStream(out)).write(
				result != null && result);
	}

	@Override
	protected boolean isAssignableFrom(Class<?> type) {
		return Boolean.class.equals(type) || Boolean.TYPE.equals(type);
	}

	@Override
	protected Charset getCharset(BooleanQueryResultFormat format,
			Charset charset) {
		return format.getCharset();
	}

}
