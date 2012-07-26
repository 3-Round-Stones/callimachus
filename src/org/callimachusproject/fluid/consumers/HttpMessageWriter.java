/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
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
import java.nio.channels.ReadableByteChannel;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.fluid.AbstractFluid;
import org.callimachusproject.fluid.Consumer;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.server.util.CatReadableByteChannel;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;
import org.xml.sax.SAXException;

/**
 * Writes {@link HttpMessage} objects to message/http streams.
 * 
 * @author James Leigh
 * 
 */
public class HttpMessageWriter implements Consumer<HttpMessage> {

	public boolean isConsumable(FluidType mtype, FluidBuilder builder) {
		if (mtype.is(HttpResponse.class) || mtype.is(HttpMessage.class)
				|| mtype.is(HttpRequest.class)
				|| mtype.is(HttpEntityEnclosingRequest.class))
			return true;
		return mtype.isUnknown() && mtype.is("message/http");
	}

	public Fluid consume(final HttpMessage result, final String base,
			final FluidType ftype, final FluidBuilder builder) {
		return new AbstractFluid() {
			public String getSystemId() {
				return base;
			}

			public FluidType getFluidType() {
				return ftype;
			}

			public void asVoid() throws IOException {
				HttpEntity entity = getEntity(result);
				if (entity != null) {
					EntityUtils.consume(entity);
				}
			}

			public String toChannelMedia(String... media) {
				return getMediaType(ftype.as(media));
			}

			public ReadableByteChannel asChannel(String... media)
					throws IOException, OpenRDFException, XMLStreamException,
					TransformerException, ParserConfigurationException {
				try {
					return write(ftype.as(toChannelMedia(media)), result,
							base);
				} finally {
					asVoid();
				}
			}

			public String toHttpResponseMedia(String... media) {
				if (result instanceof HttpResponse)
					return ftype.as(media).preferred();
				return null;
			}

			public HttpResponse asHttpResponse(String... media)
					throws IOException, OpenRDFException, XMLStreamException,
					TransformerException, ParserConfigurationException,
					SAXException {
				return (HttpResponse) result;
			}

			public String toString() {
				return String.valueOf(result);
			}
		};
	}

	String getMediaType(FluidType ftype) {
		return ftype.as("message/http", "application/http", "*/*").preferred();
	}

	ReadableByteChannel write(FluidType mtype, HttpMessage result,
			String base) throws IOException, OpenRDFException,
			XMLStreamException, TransformerException,
			ParserConfigurationException {
		CatReadableByteChannel cat = new CatReadableByteChannel();
		if (result instanceof HttpResponse) {
			print(cat, ((HttpResponse) result).getStatusLine());
		} else if (result instanceof HttpRequest) {
			print(cat, ((HttpRequest) result).getRequestLine());
		}
		if (result != null) {
			for (Header hd : result.getAllHeaders()) {
				print(cat, hd);
			}
		}
		HttpEntity entity = getEntity(result);
		if (entity == null) {
			cat.println();
		} else {
			Header type = entity.getContentType();
			if (!result.containsHeader("Content-Type") && type != null) {
				print(cat, type);
			}
			Header encoding = entity.getContentEncoding();
			if (!result.containsHeader("Content-Encoding") && encoding != null) {
				print(cat, encoding);
			}
			long length = entity.getContentLength();
			if (!result.containsHeader("Content-Length") && length >= 0) {
				print(cat, length);
			}
			ReadableByteChannel in = ChannelUtil
					.newChannel(entity.getContent());
			if (result.containsHeader("Content-Length") || length >= 0) {
				cat.println();
				cat.append(in);
			} else if (result.containsHeader("Transfer-Encoding")) {
				cat.println();
				cat.append(in);
			} else {
				print(cat, new BasicHeader("Transfer-Encoding", "identity"));
				cat.println();
				cat.append(in);
			}
		}
		return cat;
	}

	HttpEntity getEntity(HttpMessage msg) {
		if (msg instanceof HttpResponse) {
			return ((HttpResponse) msg).getEntity();
		} else if (msg instanceof HttpEntityEnclosingRequest) {
			return ((HttpEntityEnclosingRequest) msg).getEntity();
		} else {
			return null;
		}
	}

	private void print(CatReadableByteChannel cat, RequestLine line)
			throws IOException {
		cat.print(line.getMethod());
		cat.print(" ");
		cat.print(line.getUri());
		cat.print(" ");
		print(cat, line.getProtocolVersion());
	}

	private void print(CatReadableByteChannel cat, StatusLine status)
			throws IOException {
		print(cat, status.getProtocolVersion());
		cat.print(" ");
		cat.print(Integer.toString(status.getStatusCode()));
		cat.print(" ");
		cat.println(status.getReasonPhrase());
	}

	private void print(CatReadableByteChannel cat, ProtocolVersion ver)
			throws IOException {
		cat.print(ver.getProtocol());
		cat.print("/");
		cat.print(Integer.toString(ver.getMajor()));
		cat.print(".");
		cat.print(Integer.toString(ver.getMinor()));
	}

	private void print(CatReadableByteChannel cat, Header hd)
			throws IOException {
		cat.print(hd.getName());
		cat.print(": ");
		cat.println(hd.getValue());
	}

	private void print(CatReadableByteChannel cat, long length)
			throws IOException {
		cat.print("Content-Length: ");
		cat.println(Long.toString(length));
	}

}
