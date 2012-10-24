/*
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
package org.callimachusproject.xml;

import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.server.exceptions.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Using the supplied factory, caches a Java object from URL.
 * 
 * @author James Leigh
 **/
public abstract class ReusableDocumentResolver<T, E extends Exception> {
	public static void resetCache() {
		resetCount++;
	}

	public static void invalidateCache() {
		invalidateCount++;
	}

	private static final Pattern SMAXAGE = Pattern
			.compile("\\bs-maxage\\s*=\\s*(\\d+)");
	private static final Pattern MAXAGE = Pattern
			.compile("\\bmax-age\\s*=\\s*(\\d+)");
	private static final Pattern CHARSET = Pattern
			.compile("\\bcharset\\s*=\\s*([\\w-:]+)");
	private static volatile int invalidateCount;
	private static volatile int resetCount;
	private static Logger logger = LoggerFactory
			.getLogger(ReusableDocumentResolver.class);

	private int invalidateLastCount = invalidateCount;
	private int resetLastCount = resetCount;
	private String uri;
	private String tag;
	private Integer maxage;
	private long expires;
	private T object;

	@Override
	public String toString() {
		return String.valueOf(uri);
	}

	public T resolve(String systemId) throws IOException, E {
		if (!systemId.startsWith("http:") && !systemId.startsWith("https:"))
			return resolveWithURLConnection(systemId);
		T cached = null;
		String ifNonMatch = null;
		synchronized (this) {
			if (!resetCache(systemId)) {
				if (systemId.equals(uri) && object != null) {
					cached = fromCache(systemId);
					if (cached != null)
						return cached;
					ifNonMatch = tag;
					cached = object;
				}
			}
		}
		String redirect = systemId;
		HttpResponse resp = null;
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		String accept = join(getContentTypes());
		for (int i = 0; i < 20 && redirect != null; i++) {
			systemId = redirect;
			HttpRequest req = new BasicHttpRequest("GET", redirect);
			req.setHeader("Accept", accept);
			if (ifNonMatch != null) {
				req.setHeader("If-None-Match", ifNonMatch);
			}
			resp = client.service(req);
			redirect = client.redirectLocation(redirect, resp);
		}
		return cacheResponse(systemId, resp, cached);
	}

	protected abstract String[] getContentTypes();

	protected abstract T create(String systemId, InputStream in)
			throws IOException, E;

	protected abstract T create(String systemId, Reader in) throws IOException,
			E;

	/**
	 * returns null for 404 resources.
	 * 
	 * @throws E
	 */
	private synchronized T resolveWithURLConnection(String systemId)
			throws IOException, E {
		if (uri == null || !uri.equals(systemId)) {
			uri = systemId;
			object = null;
			tag = null;
			expires = 0;
			maxage = null;
		} else if (object != null
				&& (expires == 0 || expires > currentTimeMillis())) {
			return object;
		}
		URLConnection con = new URL(systemId).openConnection();
		con.addRequestProperty("Accept", join(getContentTypes()));
		con.addRequestProperty("Accept-Encoding", "gzip");
		if (tag != null && object != null) {
			con.addRequestProperty("If-None-Match", tag);
		}
		try {
			if (isStorable(con.getHeaderField("Cache-Control"))) {
				return object = createObject(con);
			} else {
				object = null;
				tag = null;
				expires = 0;
				maxage = 0;
				return createObject(con);
			}
		} catch (FileNotFoundException e) {
			object = null;
			tag = null;
			expires = 0;
			maxage = 0;
			return null;
		}
	}

	private T createObject(URLConnection con) throws IOException, E {
		String cacheControl = con.getHeaderField("Cache-Control");
		long date = con.getHeaderFieldDate("Expires", expires);
		expires = getExpires(cacheControl, date);
		if (con instanceof HttpURLConnection) {
			int status = ((HttpURLConnection) con).getResponseCode();
			if (status == 304 || status == 412) {
				return object; // Not Modified
			}
		}
		logger.info("Compiling {}", con.getURL());
		tag = con.getHeaderField("ETag");
		String base = con.getURL().toExternalForm();
		String type = con.getContentType();
		String encoding = con.getHeaderField("Content-Encoding");
		InputStream in = con.getInputStream();
		if (encoding != null && encoding.contains("gzip")) {
			in = new GZIPInputStream(in);
		}
		Matcher m = CHARSET.matcher(type);
		if (m.find()) {
			Reader reader = new InputStreamReader(in, m.group(1));
			return create(base, reader);
		}
		return create(base, in);
	}

	private synchronized boolean resetCache(String systemId) {
		if (uri == null || !uri.equals(systemId)
				|| resetLastCount != resetCount) {
			uri = systemId;
			object = null;
			tag = null;
			expires = 0;
			maxage = null;
			resetLastCount = resetCount;
			return true;
		}
		return false;
	}

	private synchronized T fromCache(String systemId) {
		if ((expires == 0 || expires > currentTimeMillis())
				&& invalidateLastCount == invalidateCount)
			return object;
		invalidateLastCount = invalidateCount;
		return null;
	}

	private synchronized T cacheResponse(String systemId, HttpResponse resp,
			T cached) throws IOException, E {
		if (isStorable(getHeader(resp, "Cache-Control"))) {
			return object = createObject(systemId, resp, cached);
		} else {
			object = null;
			tag = null;
			expires = 0;
			maxage = null;
			return createObject(systemId, resp, null);
		}
	}

	private String getHeader(HttpResponse resp, String name) {
		if (resp.containsHeader(name))
			return resp.getFirstHeader(name).getValue();
		return null;
	}

	private String join(String[] contentTypes) {
		if (contentTypes == null)
			return "*/*";
		int iMax = contentTypes.length - 1;
		if (iMax == -1)
			return "*/*";

		StringBuilder b = new StringBuilder();
		for (int i = 0;; i++) {
			b.append(String.valueOf(contentTypes[i]));
			if (i == iMax)
				return b.toString();
			b.append(", ");
		}
	}

	private boolean isStorable(String cc) {
		return cc == null || !cc.contains("no-store")
				&& (!cc.contains("private") || cc.contains("public"));
	}

	private T createObject(String systemId, HttpResponse con, T cached)
			throws IOException, E {
		HttpEntity entity = con.getEntity();
		InputStream in = entity == null ? null : entity.getContent();
		String type = getHeader(con, "Content-Type");
		String cacheControl = getHeader(con, "Cache-Control");
		expires = getExpires(cacheControl, expires);
		int status = con.getStatusLine().getStatusCode();
		if (status == 304 || status == 412) {
			if (in != null) {
				in.close();
			}
			return object = cached; // Not Modified
		} else if (status == 404 || status == 405 || status == 410
				|| status == 204) {
			if (in != null) {
				in.close();
			}
			return object = null;
		} else if (status >= 300) {
			throw ResponseException.create(con, systemId);
		}
		logger.info("Compiling {}", systemId);
		tag = getHeader(con, "ETag");
		Matcher m = CHARSET.matcher(type);
		if (m.find()) {
			Reader reader = new InputStreamReader(in, m.group(1));
			return create(systemId, reader);
		}
		return create(systemId, in);
	}

	private long getExpires(String cacheControl, long defaultValue) {
		if (cacheControl != null && cacheControl.contains("no-cache"))
			return -1;
		if (cacheControl != null && cacheControl.contains("s-maxage")) {
			try {
				Matcher m = SMAXAGE.matcher(cacheControl);
				if (m.find()) {
					maxage = parseInt(m.group(1));
				}
			} catch (NumberFormatException e) {
				// skip
			}
		} else if (cacheControl != null && cacheControl.contains("max-age")) {
			try {
				Matcher m = MAXAGE.matcher(cacheControl);
				if (m.find()) {
					maxage = parseInt(m.group(1));
				}
			} catch (NumberFormatException e) {
				// skip
			}
		}
		if (maxage != null)
			return currentTimeMillis() + maxage * 1000;
		return defaultValue;
	}

}
