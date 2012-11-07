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
package org.callimachusproject.server.model;

import info.aduna.net.ParsedURI;

import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import org.apache.commons.httpclient.util.DateParseException;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.server.exceptions.BadRequest;

/**
 * Utility class for {@link HttpServletRequest}.
 * 
 * @author James Leigh
 * 
 */
public class Request extends EditableHttpEntityEnclosingRequest {
	private long received = System.currentTimeMillis();
	private final boolean safe;
	private final boolean storable;
	private final InetAddress remoteAddr;
	private final boolean internal;
	private String iri;

	public Request(Request request) {
		this(request, request.getRemoteAddr(), request.isInternal());
	}

	public Request(HttpRequest request, InetAddress remoteAddr, boolean internal) {
		super(request);
		assert remoteAddr != null;
		this.remoteAddr = remoteAddr;
		this.internal = internal;
		String method = getMethod();
		safe = method.equals("HEAD") || method.equals("GET")
				|| method.equals("OPTIONS") || method.equals("PROFIND");
		storable = safe && !isMessageBody()
				&& getCacheControl("no-store", 0) == 0;
		if (request instanceof Request) {
			iri = ((Request) request).getIRI();
		} else {
			iri = getURIFromRequestTarget(getRequestLine().getUri());
		}
	}

	public boolean isInternal() {
		return internal;
	}

	/**
	 * Response has been created, but may not yet be written.
	 */
	public void closeRequest() {
		HttpEntity entity = getEntity();
		if (entity != null) {
			EntityUtils.consumeQuietly(entity);
		}
		if (getEnclosingRequest() instanceof Request) {
			((Request) getEnclosingRequest()).closeRequest();
		}
	}

	public final long getReceivedOn() {
		return received;
	}

	public void setReceivedOn(long received) {
		this.received = received;
	}

	@Override
	public Request clone() {
		Request clone = (Request) super.clone();
		clone.received = received;
		return clone;
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
		try {
			return DateUtil.parseDate(value).getTime();
		} catch (DateParseException e) {
			return -1;
		}
	}

	public String getResolvedHeader(String name) {
		String value = getHeader(name);
		if (value == null)
			return null;
		return resolve(value);
	}

	public InetAddress getRemoteAddr() {
		return remoteAddr;
	}

	public int getMaxAge() {
		return getCacheControl("max-age", Integer.MAX_VALUE);
	}

	public int getMinFresh() {
		return getCacheControl("min-fresh", 0);
	}

	public int getMaxStale() {
		return getCacheControl("max-stale", 0);
	}

	public final boolean isStorable() {
		return storable;
	}

	public final boolean isSafe() {
		return safe;
	}

	public boolean invalidatesCache() {
		String method = getMethod();
		return !isSafe() && !method.equals("TRACE") && !method.equals("COPY")
				&& !method.equals("LOCK") && !method.equals("UNLOCK");
	}

	public boolean isNoCache() {
		return isStorable() && getCacheControl("no-cache", 0) > 0;
	}

	public boolean isOnlyIfCache() {
		return isStorable() && getCacheControl("only-if-cached", 0) > 0;
	}

	public String getMethod() {
		return getRequestLine().getMethod();
	}

	public String getRequestTarget() {
		return getRequestLine().getUri();
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
		return getURLFromRequestTarget(uri);
	}

	public String getURLFromRequestTarget(String path) {
		if (path.equals("*"))
			return "*";
		if (!path.startsWith("/")) {
			return canonicalize(path);
		}
		String qs = null;
		int qx = path.indexOf('?');
		if (qx > 0) {
			qs = path.substring(qx + 1);
			path = path.substring(0, qx);
		}
		String scheme = getScheme().toLowerCase();
		String host = getAuthority().toLowerCase();
		// path is already encoded, so use ParsedURI to concat
		// note that java.net.URI would double encode the path here
		return canonicalize(new ParsedURI(scheme, host, path, qs, null).toString());
	}

	public String getIRI() {
		return iri;
	}

	public void setIRI(String iri) {
		this.iri = iri;
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

	public String getURIFromRequestTarget(String path) {
		try {
			int qx = path.indexOf('?');
			if (qx > 0) {
				path = path.substring(0, qx);
			}
			if (!path.startsWith("/"))
				return TermFactory.newInstance(path).getSystemId();
			String scheme = getScheme().toLowerCase();
			String host = getAuthority().toLowerCase();
			String uri = new ParsedURI(scheme, host, path, null, null).toString();
			return TermFactory.newInstance(uri).getSystemId();
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
		StringBuilder sb = new StringBuilder();
		InetAddress addr = getRemoteAddr();
		if (addr == null) {
			sb.append('-');
		} else {
			sb.append(addr.getHostAddress());
		}
		sb.append("\t?\t");
		sb.append('"').append(getRequestLine().toString()).append('"');
		return sb.toString();
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
		throw new BadRequest("Missing Host Header");
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
		// Set by custom HTTP(S)ServerIOEventDispatch
		Object scheme = getParams().getParameter("http.protocol.scheme");
		if (scheme == null)
			return "http";
		return (String) scheme;
	}

	public Enumeration getHeaderEnumeration(String name) {
		Vector values = new Vector();
		for (Header hd : getHeaders(name)) {
			values.add(hd.getValue());
		}
		return values.elements();
	}

	protected List<String> getHeaderValues(String... names) {
		List<String> values = new ArrayList<String>();
		for (Header hd : getAllHeaders()) {
			for (String name : names) {
				if (name.equalsIgnoreCase(hd.getName())) {
					values.add(hd.getValue());
				}
			}
		}
		return values;
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
