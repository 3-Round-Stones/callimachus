/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
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
package org.callimachusproject.server.filters;

import static java.net.URLEncoder.encode;
import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Request;

/**
 * Extracts a percent encoded URI from a wrapping URL.
 * 
 * @author James Leigh
 * 
 */
public class IndirectRequestFilter extends Filter {
	private String[] prefixes;

	public IndirectRequestFilter(Filter delegate) {
		super(delegate);
	}

	public String[] getIndirectIdentificationPrefix() {
		return prefixes;
	}

	public void setIndirectIdentificationPrefix(String[] prefix) {
		if (prefix == null || prefix.length == 0) {
			this.prefixes = null;
		} else {
			this.prefixes = prefix;
		}
	}

	@Override
	public HttpResponse intercept(Request req) throws IOException {
		if (prefixes == null)
			return super.intercept(req);
		String uri = req.getRequestURI();
		for (String prefix : prefixes) {
			if (uri != null && uri.startsWith(prefix)) {
				String iri = getIRI(prefix, uri);
				if (!isGood(iri, req))
					return insecure();
				req.setIRI(iri);
				break;
			}
		}
		return super.intercept(req);
	}

	public HttpResponse filter(Request req, HttpResponse resp)
			throws IOException {
		resp = super.filter(req, resp);
		if (prefixes == null)
			return resp;
		String uri = req.getRequestURI();
		for (String prefix : prefixes) {
			if (uri != null && uri.startsWith(prefix)) {
				String target = req.getIRI();
				Header[] headers = resp.getHeaders("Location");
				resp.removeHeaders("Location");
				for (Header hd : headers) {
					String loc = hd.getValue();
					if (loc.equals(target)) {
						resp.addHeader("Location", uri);
					} else if (loc.startsWith(target)) {
						int q = loc.indexOf('?', target.length());
						if (q > 0) {
							int tl = target.length();
							String part = encode(loc.substring(tl, q), "UTF-8");
							String url = uri + part + loc.substring(q);
							resp.addHeader("Location", url);
						} else {
							int tl = target.length();
							String part = encode(loc.substring(tl), "UTF-8");
							resp.addHeader("Location", uri + part);
						}
					} else {
						resp.addHeader(hd);
					}
				}
				break;
			}
		}
		return resp;
	}

	private String getIRI(String prefix, String uri)
			throws UnsupportedEncodingException {
		String encoded = uri.substring(prefix.length());
		String target = URLDecoder.decode(encoded, "UTF-8");
		return TermFactory.newInstance(target).getSystemId();
	}

	private boolean isGood(String iri, Request req) {
		ParsedURI parsed = new ParsedURI(iri);
		String scheme = parsed.getScheme();
		String authority = parsed.getAuthority();
		Object protocol = req.getParams().getParameter("http.protocol.scheme");
		String host = req.getAuthority();
		if (host.equals(authority))
			return false;
		return !"https".equals(scheme) || "https".equals(protocol);
	}

	private BasicHttpResponse insecure() throws UnsupportedEncodingException {
		String msg = "Cannot request secure resource over insecure channel";
		BasicHttpResponse resp;
		resp = new BasicHttpResponse(HttpVersion.HTTP_1_1, 400, msg);
		resp.setEntity(new StringEntity(msg));
		return resp;
	}

}
