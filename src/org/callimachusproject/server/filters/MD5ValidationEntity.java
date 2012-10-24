/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
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
package org.callimachusproject.server.filters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.security.NoSuchAlgorithmException;

import org.apache.http.HttpEntity;
import org.callimachusproject.client.HttpEntityWrapper;
import org.callimachusproject.server.util.ChannelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ensures the message body matches the Content-MD5 header.
 */
public class MD5ValidationEntity extends HttpEntityWrapper {
	private String md5;

	public MD5ValidationEntity(HttpEntity entity, String md5) {
		super(entity);
		this.md5 = md5;
	}

	public String getContentMD5() {
		return md5;
	}

	@Override
	protected InputStream getDelegateContent() throws IOException, IllegalStateException {
		InputStream delegate = super.getDelegateContent();
		try {
			ReadableByteChannel in = ChannelUtil.newChannel(delegate);
			return ChannelUtil.newInputStream(new MD5ValidatingChannel(in, md5) {
				public void close() throws IOException {
					super.close();
					md5 = getContentMD5();
				}
			});
		} catch (NoSuchAlgorithmException e) {
			Logger logger = LoggerFactory.getLogger(MD5ValidationEntity.class);
			logger.warn(e.getMessage(), e);
			return delegate;
		}
	}

}
