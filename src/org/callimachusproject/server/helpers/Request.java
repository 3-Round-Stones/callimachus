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
package org.callimachusproject.server.helpers;

import info.aduna.net.ParsedURI;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.server.exceptions.BadRequest;

/**
 * Utility class for {@link HttpServletRequest}.
 * 
 * @author James Leigh
 * 
 */
public class Request extends EditableHttpEntityEnclosingRequest {
	private final String protocol;
	private final String origin;
	private final String iri;

	public Request(HttpRequest request, HttpContext context) {
		this(request, CalliContext.adapt(context).getProtocolScheme());
	}

	public Request(HttpRequest request, String protocol) {
		super(request);
		String path = request.getRequestLine().getUri();
		if (protocol == null) {
			String msg = "Could not determine protocol for " + path;
			throw new IllegalStateException(msg);
		}
		this.protocol = protocol;
		try {
			int qx = path.indexOf('?');
			if (qx > 0) {
				path = path.substring(0, qx);
			}
			if (path.startsWith("/")) {
				String scheme = getScheme().toLowerCase();
				String host = getAuthority().toLowerCase();
				ParsedURI parsed = new ParsedURI(scheme, host, path, null, null);
				String uri = parsed.toString();
				iri = TermFactory.newInstance(uri).getSystemId();
				origin = scheme + "://" + host;
			} else {
				iri = canonicalize(path);
				ParsedURI parsed = new ParsedURI(iri);
				origin = parsed.getScheme() + "://" + parsed.getAuthority();
			}
		} catch (IllegalArgumentException e) {
			throw new BadRequest(e);
		}
	}

	public String getHeader(String name) {
		Header[] headers = getHeaders(name);
		if (headers == null || headers.length == 0)
			return null;
		return headers[0].getValue();
	}

	public long getDateHeader(String name) {
		String value = getHeader(name);
		if (value == null)
			return -1;
		Date date = DateUtils.parseDate(value);
		if (date == null)
			return -1;
		return date.getTime();
	}

	public String getResolvedHeader(String name) {
		String value = getHeader(name);
		if (value == null)
			return null;
		return resolve(value);
	}

	public final boolean isStorable() {
		return isSafe() && !isMessageBody()
				&& getCacheControl("no-store", 0) == 0;
	}

	public final boolean isSafe() {
		String method = getMethod();
		return method.equals("HEAD") || method.equals("GET")
				|| method.equals("OPTIONS") || method.equals("PROFIND");
	}

	public boolean isOnlyIfCache() {
		return isStorable() && getCacheControl("only-if-cached", 0) > 0;
	}

	public String getMethod() {
		return getRequestLine().getMethod();
	}

	public String getQueryString() {
		String qs = getRequestLine().getUri();
		int idx = qs.indexOf('?');
		if (idx < 0)
			return null;
		return qs.substring(idx + 1);
	}

	public String getRequestURL() {
		String uri = getRequestLine().getUri();
		if (uri.equals("*"))
			return "*";
		if (!uri.startsWith("/")) {
			return canonicalize(uri);
		}
		String qs = null;
		int qx = uri.indexOf('?');
		if (qx > 0) {
			qs = uri.substring(qx + 1);
			uri = uri.substring(0, qx);
		}
		String scheme = getScheme().toLowerCase();
		String host = getAuthority().toLowerCase();
		// path is already encoded, so use ParsedURI to concat
		// note that java.net.URI would double encode the path here
		return canonicalize(new ParsedURI(scheme, host, uri, qs, null).toString());
	}

	public String getIRI() {
		return iri;
	}

	public String getOrigin() {
		return origin;
	}

	public String getRequestURI() {
		String path = getRequestLine().getUri();
		try {
			int qx = path.indexOf('?');
			if (qx > 0) {
				path = path.substring(0, qx);
			}
			if (!path.startsWith("/"))
				return path;
			String scheme = getScheme().toLowerCase();
			String host = getAuthority().toLowerCase();
			return new ParsedURI(scheme, host, path, null, null).toString();
		} catch (IllegalArgumentException e) {
			throw new BadRequest(e);
		}
	}

	public String resolve(String url) {
		if (url == null)
			return null;
		try {
			TermFactory tf = TermFactory.newInstance(getRequestURI());
			return tf.resolve(url);
		} catch (IllegalArgumentException e) {
			throw new BadRequest(e);
		}
	}

	public boolean isMessageBody() {
		String length = getHeader("Content-Length");
		return length != null && !"0".equals(length)
				|| getHeader("Transfer-Encoding") != null;
	}

	public String toString() {
		return getRequestLine().toString();
	}

	public String getAuthority() {
		String uri = getRequestLine().getUri();
		if (uri != null && !uri.equals("*") && !uri.startsWith("/")) {
			try {
				String authority = new java.net.URI(uri).getAuthority();
				if (authority != null)
					return authority;
			} catch (URISyntaxException e) {
				// try the host header
			}
		}
		String host = getHeader("Host");
		if (host != null)
			return host.toLowerCase();
		throw new BadRequest("Missing Host Header for request-uri: " + uri);
	}

	public String getScheme() {
		String uri = getRequestLine().getUri();
		if (uri != null && !uri.equals("*") && !uri.startsWith("/")) {
			try {
				String scheme = new java.net.URI(uri).getScheme();
				if (scheme != null)
					return scheme;
			} catch (URISyntaxException e) {
				// try the host header
			}
		}
		return protocol;
	}

	public Enumeration getHeaderEnumeration(String name) {
		Vector values = new Vector();
		for (Header hd : getHeaders(name)) {
			values.add(hd.getValue());
		}
		return values.elements();
	}

	private String canonicalize(String url) {
		try {
			return TermFactory.newInstance(url).getSystemId();
		} catch (IllegalArgumentException e) {
			throw new BadRequest(e);
		}
	}

	private int getCacheControl(String directive, int def) {
		Enumeration headers = getHeaderEnumeration("Cache-Control");
		while (headers.hasMoreElements()) {
			String value = (String) headers.nextElement();
			for (String v : value.split("\\s*,\\s*")) {
				int idx = v.indexOf('=');
				if (idx >= 0 && directive.equals(v.substring(0, idx))) {
					try {
						return Integer.parseInt(v.substring(idx + 1));
					} catch (NumberFormatException e) {
						// invalid number
					}
				} else if (directive.equals(v)) {
					return Integer.MAX_VALUE;
				}
			}
		}
		return def;
	}

}
