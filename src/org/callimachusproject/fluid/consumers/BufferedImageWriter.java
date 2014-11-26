/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.fluid.consumers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.Vapor;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.io.ProducerStream;
import org.callimachusproject.io.ProducerStream.OutputProducer;

public class BufferedImageWriter implements Consumer<BufferedImage> {

	private static final String[] WRITER_MIME_TYPES = ImageIO
			.getWriterMIMETypes();

	@Override
	public boolean isConsumable(FluidType ftype, FluidBuilder builder) {
		if (!BufferedImage.class.isAssignableFrom(ftype.asClass()))
			return false;
		return ftype.is(WRITER_MIME_TYPES);
	}

	@Override
	public Fluid consume(final BufferedImage result, final String base,
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
			protected ReadableByteChannel asChannel(final FluidType media)
					throws IOException {
				if (result == null)
					return null;
				return ChannelUtil.newChannel(new ProducerStream(new OutputProducer() {
					public void produce(OutputStream out) throws IOException {
						try {
							streamTo(out, ftype.as(toChannelMedia(media)));
						} finally {
							out.close();
						}
					}

					public String toString() {
						return result.toString();
					}
				}));
			}

			@Override
			protected String toStreamMedia(FluidType media) {
				return toChannelMedia(media);
			}

			@Override
			protected InputStream asStream(FluidType media) throws IOException {
				if (result == null)
					return null;
				ByteArrayOutputStream baos = new ByteArrayOutputStream(8192);
				try {
					streamTo(baos, media);
				} finally {
					baos.close();
				}
				return new ByteArrayInputStream(baos.toByteArray());
			}

			@Override
			protected void streamTo(OutputStream out, FluidType media)
					throws IOException {
				if (result == null)
					return;
				String type = ftype.as(media).preferred();
				Iterator<ImageWriter> iter = ImageIO
						.getImageWritersByMIMEType(type);
				if (!iter.hasNext())
					throw new IOException("No Image writer found for " + type);
				ImageOutputStream stream = null;
				try {
					stream = ImageIO.createImageOutputStream(out);
				} catch (IOException e) {
					throw new IIOException("Can't create output stream!", e);
				}
				try {
					ImageWriter writer = iter.next();
					writer.setOutput(stream);
					try {
						writer.write(result);
					} catch (IOException e) {
						writer.abort();
						throw e;
					} finally {
						writer.dispose();
					}
				} finally {
					stream.close();
				}
			}

			public String toString() {
				return String.valueOf(result);
			}
		};
	}

}
