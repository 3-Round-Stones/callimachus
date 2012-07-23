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
package org.callimachusproject.fluid.consumers.base;

import info.aduna.iteration.CloseableIteration;
import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import org.callimachusproject.server.util.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures results are closed after been written.
 * 
 * @author James Leigh
 * 
 * @param <FF>
 *            file format
 * @param <S>
 *            reader factory
 * @param <T>
 *            result
 */
public abstract class ResultMessageWriterBase<FF extends FileFormat, S, T extends CloseableIteration<?, ?>>
		extends MessageWriterBase<FF, S, T> {
	private Logger logger = LoggerFactory
			.getLogger(ResultMessageWriterBase.class);

	public ResultMessageWriterBase(FileFormatServiceRegistry<FF, S> registry,
			Class<T> type) {
		super(registry, type);
	}

	@Override
	public ReadableByteChannel write(MessageType mtype, final T result,
			String base, Charset charset) throws IOException {
		final ReadableByteChannel delegate;
		delegate = super.write(mtype, result, base, charset);
		return new ReadableByteChannel() {
			public String toString() {
				return delegate.toString();
			}

			public void close() throws IOException {
				try {
					result.close();
				} catch (Exception e) {
					logger.warn(e.getMessage(), e);
				} finally {
					delegate.close();
				}
			}

			public boolean isOpen() {
				return delegate.isOpen();
			}

			public int read(ByteBuffer dst) throws IOException {
				return delegate.read(dst);
			}
		};
	}

}
