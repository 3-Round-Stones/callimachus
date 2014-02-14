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
package org.callimachusproject.fluid.producers;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

import javax.imageio.ImageIO;

import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.Producer;
import org.callimachusproject.io.ChannelUtil;

public class BufferedImageReader implements Producer {

	private static final String[] READER_MIME_TYPES = ImageIO.getReaderMIMETypes();

	@Override
	public boolean isProducable(FluidType ftype, FluidBuilder builder) {
		return ftype.asClass().isAssignableFrom(BufferedImage.class) && ftype.is(READER_MIME_TYPES);
	}

	@Override
	public BufferedImage produce(FluidType ftype, ReadableByteChannel in,
			Charset charset, String base, FluidBuilder builder)
			throws Exception {
		if (in == null)
			return null;
		InputStream stream = ChannelUtil.newInputStream(in);
		return ImageIO.read(stream);
	}

}
