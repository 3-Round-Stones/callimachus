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
package org.callimachusproject.fluid.consumers.base;

import info.aduna.lang.FileFormat;
import info.aduna.lang.service.FileFormatServiceRegistry;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.callimachusproject.fluid.AbstractFluid;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.util.ProducerChannel;
import org.callimachusproject.server.util.ProducerChannel.WritableProducer;
import org.openrdf.OpenRDFException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.rio.RDFHandlerException;

/**
 * Base class for writers that use a {@link FileFormat}.
 * 
 * @author James Leigh
 * 
 * @param <FF>
 *            file format
 * @param <S>
 *            reader factory
 * @param <T>
 *            Java type returned
 */
public abstract class MessageWriterBase<FF extends FileFormat, S, T> implements
		Consumer<T> {
	private FileFormatServiceRegistry<FF, S> registry;
	private Class<T> type;

	public MessageWriterBase(FileFormatServiceRegistry<FF, S> registry,
			Class<T> type) {
		this.registry = registry;
		this.type = type;
	}

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		if (!this.type.isAssignableFrom((Class<?>) mtype.asClass()))
			return false;
		return getFactory(mtype.getMediaType()) != null;
	}

	public Fluid consume(final FluidType ftype, final T result, final String base,
			final FluidBuilder builder) {
		return new AbstractFluid(builder) {
			public String toChannelMedia(String media) {
				return getMediaType(media);
			}

			public ReadableByteChannel asChannel(String media)
					throws IOException, OpenRDFException, XMLStreamException,
					TransformerException, ParserConfigurationException {
				return write(ftype.as(getMediaType(media)), builder.getObjectConnection(), result, base);
			}

			public String toString() {
				return result.toString();
			}
		};
	}

	private String getMediaType(String mimeType) {
		FF format = getFormat(mimeType);
		String contentType = null;
		if (mimeType != null) {
			for (String content : format.getMIMETypes()) {
				if (mimeType.startsWith(content)) {
					contentType = content;
				}
			}
		}
		if (contentType == null) {
			contentType = format.getDefaultMIMEType();
		}
		if (contentType.startsWith("text/") && format.hasCharset()) {
			Charset charset = new FluidType(Object.class, mimeType).getCharset();
			charset = getCharset(format, charset);
			contentType += ";charset=" + charset.name();
		}
		return contentType;
	}

	protected ReadableByteChannel write(final FluidType mtype, final ObjectConnection con,
			final T result, final String base) throws IOException {
		return new ProducerChannel(new WritableProducer() {
			public void produce(WritableByteChannel out) throws IOException {
				try {
					writeTo(mtype, con, result, base, out, 1024);
				} catch (OpenRDFException e) {
					throw new IOException(e);
				} finally {
					out.close();
				}
			}

			public String toString() {
				return String.valueOf(result);
			}
		});
	}

	public void writeTo(FluidType mtype, ObjectConnection con, T result,
			String base, WritableByteChannel out, int bufSize)
			throws IOException, OpenRDFException {
		Charset charset = mtype.getCharset();
		String mimeType = mtype.getMediaType();
		FF format = getFormat(mimeType);
		if (format.hasCharset()) {
			charset = getCharset(format, charset);
		}
		try {
			writeTo(getFactory(mimeType), result, out, charset, base, con);
		} catch (RDFHandlerException e) {
			Throwable cause = e.getCause();
			try {
				if (cause != null)
					throw cause;
			} catch (IOException c) {
				throw c;
			} catch (OpenRDFException c) {
				throw c;
			} catch (Throwable c) {
				throw e;
			}
		} catch (TupleQueryResultHandlerException e) {
			Throwable cause = e.getCause();
			try {
				if (cause != null)
					throw cause;
			} catch (IOException c) {
				throw c;
			} catch (OpenRDFException c) {
				throw c;
			} catch (Throwable c) {
				throw e;
			}
		}
	}

	protected Charset getCharset(FF format, Charset charset) {
		if (charset == null) {
			charset = format.getCharset();
		}
		return charset;
	}

	public abstract void writeTo(S factory, T result, WritableByteChannel out,
			Charset charset, String base, ObjectConnection con) throws IOException,
			RDFHandlerException, QueryEvaluationException,
			TupleQueryResultHandlerException;

	protected S getFactory(String mimeType) {
		FF format = getFormat(mimeType);
		if (format == null)
			return null;
		return registry.get(format);
	}

	protected FF getFormat(String mimeType) {
		if (mimeType == null || mimeType.contains("*")
				|| "application/octet-stream".equals(mimeType)) {
			for (FF format : registry.getKeys()) {
				if (registry.get(format) != null)
					return format;
			}
			return null;
		}
		int idx = mimeType.indexOf(';');
		if (idx > 0) {
			mimeType = mimeType.substring(0, idx);
		}
		return registry.getFileFormatForMIMEType(mimeType);
	}
}
