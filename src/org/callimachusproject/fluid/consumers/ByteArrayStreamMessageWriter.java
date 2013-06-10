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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import org.apache.http.HttpEntity;
import org.callimachusproject.fluid.Vapor;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.server.helpers.ReadableHttpEntityChannel;

/**
 * Reads an ByteArrayOutputStream to an {@link ReadableByteChannel}.
 */
public class ByteArrayStreamMessageWriter implements
		Consumer<ByteArrayOutputStream> {

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		if (!mtype.is(ByteArrayOutputStream.class))
			return false;
		return mtype.is("application/*");
	}

	public Fluid consume(final ByteArrayOutputStream result, final String base,
			final FluidType ftype, final FluidBuilder builder) {
		return new Vapor() {
			public String getSystemId() {
				return base;
			}

			public FluidType getFluidType() {
				return ftype;
			}

			public void asVoid() {
				// no-op
			}

			@Override
			protected String toChannelMedia(FluidType media) {
				return ftype.as(media).preferred();
			}

			@Override
			protected ReadableByteChannel asChannel(FluidType media)
					throws IOException {
				if (result == null)
					return ChannelUtil.emptyChannel();
				return ChannelUtil.newChannel(result.toByteArray());
			}

			@Override
			protected String toHttpEntityMedia(FluidType media) {
				return toChannelMedia(media);
			}

			@Override
			protected HttpEntity asHttpEntity(FluidType media)
					throws IOException {
				String mediaType = toChannelMedia(media);
				long size = (long) (result == null ? 0 : result.size());
				return new ReadableHttpEntityChannel(mediaType, size,
						asChannel(media));
			}

			public String toString() {
				return String.valueOf(result);
			}
		};
	}
}
