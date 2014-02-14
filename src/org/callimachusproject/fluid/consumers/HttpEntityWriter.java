/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.fluid.Vapor;
import org.callimachusproject.io.ChannelUtil;
import org.openrdf.OpenRDFException;

public class HttpEntityWriter implements Consumer<HttpEntity> {

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		return HttpEntity.class.isAssignableFrom(mtype.asClass());
	}

	public Fluid consume(final HttpEntity result, final String base,
			final FluidType ftype, final FluidBuilder builder) {
		return new Vapor() {
			public String getSystemId() {
				return base;
			}

			public FluidType getFluidType() {
				return ftype;
			}

			public void asVoid() throws IOException {
				if (result != null) {
					EntityUtils.consume(result);
				}
			}

			@Override
			protected String toHttpEntityMedia(FluidType media) {
				if (result == null)
					return ftype.as(media).preferred();
				Header hd = result.getContentType();
				if (hd == null)
					return ftype.as(media).preferred();
				return ftype.as(hd.getValue()).as(media).preferred();
			}

			@Override
			protected HttpEntity asHttpEntity(FluidType media) throws Exception {
				if (result == null)
					return null;
				return result;
			}

			@Override
			protected String toChannelMedia(FluidType media) {
				if (result == null)
					return ftype.as(media).preferred();
				Header hd = result.getContentType();
				if (hd == null)
					return ftype.as(media).preferred();
				return ftype.as(hd.getValue()).as(media).preferred();
			}

			@Override
			protected ReadableByteChannel asChannel(FluidType media)
					throws IOException, OpenRDFException, XMLStreamException,
					TransformerException, ParserConfigurationException {
				if (result == null)
					return null;
				return ChannelUtil.newChannel(result.getContent());
			}

			public String toString() {
				return String.valueOf(result);
			}
		};
	}

}
