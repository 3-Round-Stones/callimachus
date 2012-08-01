/*
   Copyright (c) 2012 3 Round Stones Inc, Some Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.fluid;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.ObjectConnection;
import org.xml.sax.SAXException;

/**
 * Converts Java Objects (of supported types) to other media types.
 * 
 * @author James Leigh
 * 
 */
public class FluidBuilder {
	private final List<Consumer<?>> consumers;
	private List<Producer> producers;
	private final ObjectConnection con;

	public FluidBuilder(List<Consumer<?>> consumers, List<Producer> producers) {
		assert consumers != null;
		assert producers != null;
		this.consumers = consumers;
		this.producers = producers;
		this.con = null;
	}

	public FluidBuilder(List<Consumer<?>> consumers, List<Producer> producers,
			ObjectConnection con) {
		assert consumers != null;
		assert producers != null;
		assert con != null;
		this.consumers = consumers;
		this.producers = producers;
		this.con = con;
	}

	@Override
	public String toString() {
		return con.toString();
	}

	public ObjectConnection getObjectConnection() {
		return con;
	}

	public boolean isDatatype(Class<?> type) {
		if (con == null)
			return false;
		return con.getObjectFactory().isDatatype(type);
	}

	public boolean isConcept(Class<?> component) {
		if (con == null)
			return false;
		if (component.isAnnotationPresent(Iri.class))
			return true;
		for (Annotation ann : component.getAnnotations()) {
			for (Method m : ann.annotationType().getDeclaredMethods()) {
				if (m.isAnnotationPresent(Iri.class))
					return true;
			}
		}
		return con.getObjectFactory().isNamedConcept(component);
	}

	public boolean isConsumable(Type gtype, String... media) {
		return isConsumable(new FluidType(gtype, media));
	}

	public boolean isConsumable(FluidType mtype) {
		if (mtype.isCollection()) {
			Consumer<?> writer = findRawWriter(mtype.component());
			if (writer != null)
				return true;
		}
		return findRawWriter(mtype) != null;
	}

	public Fluid media(String... media) {
		return channel(null, null, media);
	}

	public Fluid nil(FluidType ftype) {
		return consume(null, null, ftype);
	}

	public Fluid uri(String uri, String base) {
		return consume(uri, base, new FluidType(String.class, "text/uri-list"));
	}

	public Fluid channel(final ReadableByteChannel in, final String base,
			final String... mediaTypes) {
		final FluidType inType = new FluidType(ReadableByteChannel.class,
				mediaTypes);
		return new AbstractFluid() {

			@Override
			public String getSystemId() {
				return base;
			}

			@Override
			public FluidType getFluidType() {
				return inType;
			}

			@Override
			public String toString() {
				return String.valueOf(in) + " " + Arrays.toString(mediaTypes);
			}

			@Override
			public void asVoid() throws IOException {
				if (in != null) {
					in.close();
				}
			}

			@Override
			public String toMedia(FluidType ftype) {
				FluidType outType = inType.as(ftype);
				Producer reader = findReader(outType);
				if (reader == null)
					return null;
				return outType.preferred();
			}

			@Override
			public Object as(FluidType ftype)
					throws TransformerConfigurationException, OpenRDFException,
					IOException, XMLStreamException,
					ParserConfigurationException, SAXException,
					TransformerException {
				Charset charset = inType.getCharset();
				FluidType outType = inType.as(ftype);
				Producer reader = findRawReader(outType);
				if (reader != null)
					return reader.produce(outType, in, charset, base,
							FluidBuilder.this);
				if (outType.isCollection()) {
					reader = findRawReader(outType.component());
					if (reader == null && in == null)
						return outType.castSet(Collections.emptySet());
				}
				if (reader == null && !outType.isPrimitive() && in == null)
					return null;
				if (reader == null)
					throw new BadRequest("Cannot read " + inType + " into "
							+ ftype);
				Object o = reader.produce(outType.component(), in, charset,
						base, FluidBuilder.this);
				return outType.castComponent(o);
			}

			private Producer findReader(FluidType mtype) {
				Producer reader = findRawReader(mtype);
				if (reader != null)
					return reader;
				if (mtype.isCollection())
					return findRawReader(mtype.component());
				return null;
			}

			private Producer findRawReader(FluidType mtype) {
				for (Producer reader : producers) {
					if (reader.isProducable(mtype, FluidBuilder.this)) {
						return reader;
					}
				}
				return null;
			}
		};
	}

	public Fluid stream(final InputStream in, final String base,
			final String... media) {
		return channel(ChannelUtil.newChannel(in), base, media);
	}

	public Fluid consume(Object result, String base, Type gtype,
			String... media) {
		return consume(result, base, new FluidType(gtype, media));
	}

	public Fluid consume(Object result, String base, FluidType mtype) {
		Consumer writer = findRawWriter(mtype);
		if (writer == null && mtype.isCollection()) {
			writer = findRawWriter(mtype.component());
			if (writer != null) {
				Object array = mtype.toArray(result);
				int len = Array.getLength(array);
				Fluid[] fluids = new Fluid[len];
				for (int i = 0; i < len; i++) {
					fluids[i] = consume(Array.get(array, i), base,
							mtype.component());
				}
				return new FluidArray(fluids, nil(mtype.component()), base, mtype);
			}
		}
		if (writer == null)
			throw new BadRequest("Cannot write " + mtype + " into "
					+ mtype);
		return new ChannelFluid(writer.consume(result, base, mtype, this), this);
	}

	private Consumer<?> findRawWriter(FluidType mtype) {
		for (Consumer<?> w : consumers) {
			if (w.isConsumable(mtype, this)) {
				return w;
			}
		}
		return null;
	}

}
