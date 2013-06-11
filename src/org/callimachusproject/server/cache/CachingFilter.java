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
package org.callimachusproject.server.cache;

import static org.callimachusproject.io.ChannelUtil.newChannel;
import static org.callimachusproject.io.ChannelUtil.newInputStream;
import info.aduna.concurrent.locks.Lock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.ProtocolVersion;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpDateGenerator;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.client.HttpUriResponse;
import org.callimachusproject.io.AutoCloseChannel;
import org.callimachusproject.io.CatReadableByteChannel;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.server.AsyncExecChain;
import org.callimachusproject.server.helpers.CalliContext;
import org.callimachusproject.server.helpers.CompletedResponse;
import org.callimachusproject.server.helpers.DelegatingFuture;
import org.callimachusproject.server.helpers.EntityRemovedHttpResponse;
import org.callimachusproject.server.helpers.FileHttpEntity;
import org.callimachusproject.server.helpers.ReadableHttpEntityChannel;
import org.callimachusproject.server.helpers.Request;
import org.callimachusproject.server.helpers.ResponseCallback;
import org.callimachusproject.server.util.LockCleanupManager;
import org.callimachusproject.util.DomainNameSystemResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A caching HTTP proxy.
 *
 * @author James Leigh
 */
public class CachingFilter implements AsyncExecChain {
	private static final AtomicLong seq = new AtomicLong(0);
	private static final HttpDateGenerator DATE_GENERATOR = new HttpDateGenerator();
	private static final String hostname = DomainNameSystemResolver.getInstance().getLocalHostName();
	private static final String WARN_110 = "110 " + hostname
			+ " \"Response is stale\"";
	private static final String WARN_111 = "111 " + hostname
			+ " \"Revalidation failed\"";
	private static final String WARN_112 = "112 " + hostname
			+ " \"Disconnected operation\"";
	private final Logger logger = LoggerFactory.getLogger(CachingFilter.class);

	private final AsyncExecChain delegate;
	private CacheIndex cache;
	private boolean enabled = true;
	private boolean disconnected;

	public CachingFilter(AsyncExecChain delegate, File dataDir, int maxCapacity) {
		this(delegate, new CacheIndex(dataDir, maxCapacity, new LockCleanupManager(false)));
	}

	private CachingFilter(AsyncExecChain delegate, CacheIndex cache) {
		this.delegate = delegate;
		this.cache = cache;
	}

	public int getMaxCapacity() {
		return cache.getMaxCapacity();
	}

	public void setMaxCapacity(int maxCapacity) {
		cache.setMaxCapacity(maxCapacity);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isAggressive() {
		return cache.isAggressive();
	}

	public void setAggressive(boolean aggressive) {
		cache.setAggressive(aggressive);
	}

	public boolean isDisconnected() {
		return disconnected;
	}

	public void setDisconnected(boolean disconnected) {
		this.disconnected = disconnected;
	}

	public int getSize() {
		return cache.size();
	}

	public void invalidate() throws IOException, InterruptedException {
		cache.invalidate();
	}

	public void reset() {
		try {
			cache.clear();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public Future<HttpResponse> execute(HttpHost target,
			HttpRequest request, HttpContext context,
			FutureCallback<HttpResponse> callback) {
		final Request headers = new Request(request);
		if (!headers.isSafe()) {
			callback = new ResponseCallback(callback) {
				public void completed(HttpResponse result) {
					try {
						invalidate(headers);
					} catch (IOException e) {
						logger.error(e.toString(), e);
					} finally {
						super.completed(result);
					}
				}
			};
		}
		if (!enabled || !headers.isStorable()) {
			return delegate.execute(target, request, context, callback);
		}
		try {
			String url = headers.getRequestURL();
			Lock reset = cache.getReadLock(url);
			try {
				long now = CalliContext.adapt(context).getReceivedOn();
				CachedEntity cached = null;
				CachedRequest index = cache.findCachedRequest(url);
				synchronized (index) {
					cached = index.find(headers);
					boolean stale = isStale(now, headers, cached);
					CompletedResponse ret = new CompletedResponse(callback);
					if (cached != null && disconnected) {
						ret.completed(respondWithCache(now, headers, cached, null));
					} else if (stale && !headers.isOnlyIfCache()) {
						return handleCacheMiss(target, request, context, callback);
					} else if (cached == null && headers.isOnlyIfCache()) {
						ret.completed(respond(headers, 504, "Gateway Timeout"));
					} else {
						ret.completed(respondWithCache(now, headers, cached, null));
					}
					return ret;
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				return delegate.execute(target, request, context, callback);
			} finally {
				reset.release();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.warn(e.getMessage(), e);
			return new CompletedResponse(callback, respond(headers, 504, "Gateway Timeout"));
		}
	}

	private Future<HttpResponse> handleCacheMiss(HttpHost target,
			HttpRequest request, final HttpContext context,
			final FutureCallback<HttpResponse> callback) throws IOException {
		Request headers = new Request(request);
		try {
			String url = headers.getRequestURL();
			Lock lock = cache.getReadLock(url);
			try {
				long now = CalliContext.adapt(context).getReceivedOn();
				CachedEntity cached = null;
				CachedRequest index = cache.findCachedRequest(url);
				synchronized (index) {
					cached = index.find(headers);
					if ("HEAD".equals(request.getRequestLine().getMethod()))
						return delegate.execute(target, request, context, callback);
					boolean stale = isStale(now, headers, cached);
					if (stale && !headers.isOnlyIfCache()) {
						List<CachedEntity> match = index
								.findCachedETags(headers);
						final CachableRequest cachable = createCachableRequest(headers, cached, match);
						DelegatingFuture future = new DelegatingFuture(callback) {
							public void completed(HttpResponse result) {
								try {
									super.completed(filter(cachable, context,
											result));
								} catch (IOException ex) {
									super.failed(ex);
								}
							}
						};
						future.setDelegate(delegate.execute(target, cachable,
								context, future));
						return future;
					}
				}
			} finally {
				lock.release();
			}
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		}
		return delegate.execute(target, request, context, callback);
	}

	HttpResponse filter(CachableRequest request, HttpContext context, HttpResponse resp)
			throws IOException {
		try {
			if (isCachable(request, resp)) {
				String url = request.getRequestURL();
				Lock lock = cache.getReadLock(url);
				try {
					long now = CalliContext.adapt(context).getReceivedOn();
					CachedRequest dx = cache.findCachedRequest(url);
					synchronized (dx) {
						CachedEntity cached = dx.find(request);
						if (resp.getStatusLine().getStatusCode() < 500) {
							File f = saveMessageBody(resp, dx.getDirectory(), url);
							CachedEntity fresh = dx.find(request, resp, f);
							fresh.addRequest(request);
							dx.replace(cached, fresh);
							return respondWithCache(now, request, fresh, resp);
						} else if (cached == null) {
							return resp;
						} else {
							EntityUtils.consume(resp.getEntity());
							HttpUriResponse result = respondWithCache(now, request, cached, resp);
							result.addHeader("Warning", WARN_111);
							logger.warn(resp.getStatusLine().getReasonPhrase());
							return result;
						}
					}
				} finally {
					lock.release();
				}
			}
		} catch (InterruptedException e) {
			logger.warn(e.getMessage(), e);
		} finally {
			request.releaseCachedEntities();
		}
		return resp;
	}

	private CachableRequest createCachableRequest(Request request,
			CachedEntity cached, List<CachedEntity> match) throws IOException,
			InterruptedException {
		String uri = request.getRequestLine().toString();
		Lock lock = cache.getReadLock(uri);
		return new CachableRequest(request, cached, match, lock);
	}

	private HttpUriResponse respond(Request req, int code, String reason) {
		ProtocolVersion ver = HttpVersion.HTTP_1_1;
		BasicHttpResponse resp = new BasicHttpResponse(ver, code, reason);
		resp.setHeader("Date", DATE_GENERATOR.getCurrentDate());
		resp.setHeader("Content-Length", "0");
		return new HttpUriResponse(req.getRequestURL(), resp);
	}

	private File saveMessageBody(HttpResponse res, File dir, String url)
			throws FileNotFoundException, IOException {
		HttpEntity entity = res.getEntity();
		if (entity == null)
			return null;
		if (entity instanceof FileHttpEntity)
			return ((FileHttpEntity) entity).getFile();
		long id = seq.incrementAndGet();
		String hex = Integer.toHexString(url.hashCode());
		File file = new File(dir, "$" + hex + '-' + id + ".part");
		MessageDigest digest = getDigest("MD5");
		dir.mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		try {
			InputStream in = entity.getContent();
			try {
				ChannelUtil.transfer(in, out, digest);
			} finally {
				in.close();
			}
		} finally {
			out.close();
			EntityUtils.consume(entity);
		}
		byte[] hash = Base64.encodeBase64(digest.digest());
		String contentMD5 = new String(hash, "UTF-8");
		res.setHeader("Content-MD5", contentMD5);
		return file;
	}

	private MessageDigest getDigest(String algorithm) {
		try {
			return MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError(e);
		}
	}

	private boolean isCachable(HttpRequest req, HttpResponse res) {
		int code = res.getStatusLine().getStatusCode();
		switch (code) {
		case 200:
			break; // OK
		case 203:
			break; // Non-Authoritative Information
		case 300:
			break; // Multiple Choices
		case 301:
			break; // Moved Permanently
		case 302:
			break; // Found
		case 303:
			break; // See Other
		case 304:
			break; // Not Modified
		case 307:
			break; // Temporary Redirect
		case 308:
			break; // Permanent Redirect
		case 404:
			break; // Not Found
		case 405:
			break; // Method Not Allowed
		case 410:
			break; // Gone
		default:
			return false;
		}
		boolean priv = req.containsHeader("Authorization")
				|| req.containsHeader("Cookie");
		for (Header hd : res.getHeaders("Cache-Control")) {
			if (hd.getValue().contains("no-store"))
				return false;
			if (hd.getValue().contains("private"))
				return false;
			if (hd.getValue().contains("s-maxage=0")) {
				priv = false;
			} else if (hd.getValue().contains("public")) {
				priv = false;
			} else if (hd.getValue().contains("must-revalidate")) {
				priv = false;
			}
		}
		return !priv && res.containsHeader("ETag");
	}

	private boolean isStale(long now, Request headers, CachedEntity cached)
			throws IOException {
		if (cached == null || headers.isStorable() && headers.getCacheControl("no-cache", 0) > 0 || cached.isStale())
			return true;
		int age = cached.getAge(now);
		int lifeTime = cached.getLifeTime(now);
		int maxage = headers.getCacheControl("max-age", Integer.MAX_VALUE);
		int minFresh = headers.getCacheControl("min-fresh", 0);
		int maxStale = headers.getCacheControl("max-stale", 0);
		boolean fresh = age - lifeTime + minFresh <= maxStale;
		return age > maxage || !fresh;
	}

	private HttpUriResponse respondWithCache(long now, Request req,
			CachedEntity cached, HttpResponse upstream) throws IOException, InterruptedException {
		if (req instanceof CachableRequest) {
			req = ((CachableRequest) req).getOriginalRequest();
		}
		int status = cached.getStatus();
		String statusText = cached.getStatusText();
		ProtocolVersion ver = HttpVersion.HTTP_1_1;
		BasicHttpResponse res = new EntityRemovedHttpResponse(ver, status, statusText);
		boolean unmodifiedSince = unmodifiedSince(req, cached);
		boolean modifiedSince = modifiedSince(req, cached);
		List<Long> range = range(req, cached);
		String method = req.getMethod();
		if (!unmodifiedSince) {
			res.setStatusLine(ver, 412, "Precondition Failed");
		} else if (!modifiedSince
				&& ("GET".equals(method) || "HEAD".equals(method))) {
			res.setStatusLine(ver, 304, "Not Modified");
		} else if (!modifiedSince) {
			res.setStatusLine(ver, 412, "Precondition Failed");
		} else if (status == 200 && range != null && range.isEmpty()) {
			res.setStatusLine(ver, 416, "Requested Range Not Satisfiable");
		} else if (status == 200 && range != null) {
			res.setStatusLine(ver, 206, "Partial Content");
		} else if (statusText == null) {
			res.setStatusCode(status);
		} else {
			res.setStatusLine(ver, status, statusText);
		}
		sendEntityHeaders(now, cached, res);
		setAuthorizationHeader(upstream, res);
		if (unmodifiedSince && modifiedSince) {
			sendContentHeaders(cached, res);
			if (range != null) {
				sendRangeBody(method, range, cached, res);
			} else {
				sendMessageBody(method, cached, res);
			}
		} else {
			res.setHeader("Content-Length", "0");
		}
		return new HttpUriResponse(req.getRequestURL(), res);
	}

	private boolean unmodifiedSince(Request req, CachedEntity cached) {
		try {
			long lastModified = cached.lastModified();
			if (lastModified > 0) {
				long unmodified = req.getDateHeader("If-Unmodified-Since");
				if (unmodified > 0 && lastModified > unmodified)
					return false;
			}
		} catch (IllegalArgumentException e) {
			// invalid date header
		}
		Header[] matchs = req.getHeaders("If-Match");
		boolean mustMatch = matchs != null && matchs.length > 0;
		if (mustMatch) {
			String entityTag = cached.getETag();
			for (Header match : matchs) {
				if (match(entityTag, match.getValue()))
					return true;
			}
		}
		return !mustMatch;
	}

	private boolean modifiedSince(Request req, CachedEntity cached) {
		boolean notModified = false;
		try {
			long lastModified = cached.lastModified();
			if (lastModified > 0) {
				long modified = req.getDateHeader("If-Modified-Since");
				notModified = modified > 0;
				if (notModified && modified < lastModified)
					return true;
			}
		} catch (IllegalArgumentException e) {
			// invalid date header
		}
		Header[] matchs = req.getHeaders("If-None-Match");
		boolean mustMatch = matchs != null && matchs.length > 0;
		if (mustMatch) {
			String entityTag = cached.getETag();
			for (Header match : matchs) {
				if (match(entityTag, match.getValue()))
					return false;
			}
		}
		return !notModified || mustMatch;
	}

	/**
	 * None range request return null. Not satisfiable requests return an empty
	 * list. Satisfiable requests return a list of start and length pairs.
	 */
	private List<Long> range(Request req, CachedEntity cached) {
		if (!cached.isBodyPresent())
			return null;
		String tag = req.getHeader("If-Range");
		if (tag != null) {
			if (tag.startsWith("W/") || tag.charAt(0) == '"') {
				if (!match(cached.getETag(), tag))
					return null;
			} else {
				try {
					long date = req.getDateHeader("If-Range");
					if (cached.lastModified() > date)
						return null;
				} catch (IllegalArgumentException e) {
					// invalid date header
					return null;
				}
			}
		}
		try {
			String range = req.getHeader("Range");
			if (range == null || !range.startsWith("bytes="))
				return null;
			Long length = cached.contentLength();
			List<Long> ranges = new ArrayList<Long>();
			for (String r : range.substring(6).split("\\s*,\\s*")) {
				int idx = r.indexOf('-');
				if (idx == 0) {
					long l = Long.parseLong(r.substring(1));
					if (l > length)
						return null;
					ranges.add(length - l);
					ranges.add(l);
				} else if (idx < 0) {
					long l = Long.parseLong(r);
					if (l >= length)
						return Collections.emptyList();
					ranges.add(l);
					ranges.add(length - l);
				} else {
					long b = Long.parseLong(r.substring(0, idx));
					long e = Long.parseLong(r.substring(idx + 1));
					if (b > e)
						return null;
					if (b >= length)
						return Collections.emptyList();
					if (b == 0 && e + 1 >= length)
						return null;
					ranges.add(b);
					if (e < length) {
						ranges.add(e + 1 - b);
					} else {
						ranges.add(length - b);
					}
				}
			}
			return ranges;
		} catch (Exception e) {
			return null;
		}
	}

	private boolean match(String tag, String match) {
		if (tag == null)
			return false;
		if ("*".equals(match))
			return true;
		if (match.startsWith("W/") && !tag.startsWith("W/")) {
			match = match.substring(2);
		}
		if (match.equals(tag))
			return true;
		int md = match.indexOf('-');
		int td = tag.indexOf('-');
		if (td >= 0 && md >= 0)
			return false;
		if (md < 0) {
			md = match.lastIndexOf('"');
		}
		if (td < 0) {
			td = tag.lastIndexOf('"');
		}
		int mq = match.indexOf('"');
		int tq = tag.indexOf('"');
		if (mq < 0 || tq < 0 || md < 0 || td < 0)
			return false;
		return match.substring(mq, md).equals(tag.substring(tq, td));
	}

	private void sendEntityHeaders(long now, CachedEntity cached,
			HttpResponse res) throws IOException {
		String warning = cached.getWarning();
		if (warning != null && warning.length() > 0) {
			res.addHeader("Warning", warning);
		}
		int age = cached.getAge(now);
		res.setHeader("Age", Integer.toString(age));
		if (age > cached.getLifeTime()) {
			res.addHeader("Warning", WARN_110);
		}
		if (disconnected) {
			res.addHeader("Warning", WARN_112);
		}
		String tag = cached.getETag();
		if (tag != null && tag.length() > 0) {
			res.setHeader("ETag", tag);
		}
		if (cached.lastModified() > 0) {
			res.setHeader("Last-Modified", cached.getLastModified());
		}
		if (cached.date() > 0) {
			res.setHeader("Date", cached.getDate());
		}
		String type = cached.getContentType();
		if (type != null) {
			res.setHeader("Content-Type", type);
		}
		String control = cached.getCacheControl();
		int maxAge = cached.getMaxAgeHeuristic(now);
		if (maxAge > 0 && control == null) {
			control = "max-age=" + maxAge;
		} else if (maxAge >= 0) {
			control += ",max-age=" + maxAge;
		}
		if (control != null) {
			res.setHeader("Cache-Control", control);
		}
	}

	private void setAuthorizationHeader(HttpResponse upstream, BasicHttpResponse response) {
		if (upstream != null && upstream.containsHeader("Authentication-Info")) {
			response.addHeader(upstream.getFirstHeader("Authentication-Info"));
		}
	}

	private void sendContentHeaders(CachedEntity cached, HttpResponse res) {
		for (Map.Entry<String, String> e : cached.getContentHeaders()
				.entrySet()) {
			String key = e.getKey();
			String value = e.getValue();
			if (!res.containsHeader(key) && value != null && value.length() > 0) {
				res.setHeader(key, value);
			}
		}
	}

	private void sendRangeBody(String method, List<Long> range,
			CachedEntity cached, HttpResponse res) throws IOException,
			InterruptedException {
		if (range.size() == 0)
			return;
		long contentLength = cached.contentLength();
		if (range.size() == 2) {
			long start = range.get(0);
			long length = range.get(1);
			long end = start + length - 1;
			String contentRange = "bytes " + start + "-" + end + "/"
					+ contentLength;
			res.setHeader("Content-Range", contentRange);
			res.setHeader("Content-Length", Long.toString(length));
			String type = null;
			Header hd = res.getFirstHeader("Content-Type");
			if (hd != null) {
				type = hd.getValue();
			}
			ReadableByteChannel in = cached.writeBody(start, length);
			final Lock inUse = cached.open();
			res.setEntity(new ReadableHttpEntityChannel(type, length, in,
					new Runnable() {
						public void run() {
							inUse.release();
						}
					}));
		} else {
			String boundary = "THIS_STRING_SEPARATES";
			String type = "multipart/byteranges; boundary=" + boundary;
			res.setHeader("ContentType", type);
			res.setHeader("Transfer-Encoding", "chunked");
			CatReadableByteChannel out = new CatReadableByteChannel();
			out.print("--");
			out.println(boundary);
			for (int i = 0, n = range.size(); i < n; i += 2) {
				long start = range.get(i);
				long length = range.get(i + 1);
				long end = start + length - 1;
				String ctype = cached.getContentType();
				if (ctype != null) {
					out.print("Content-Type: ");
					out.println(ctype);
				}
				out.print("Content-Length: ");
				out.println(Long.toString(length));
				out.print("Content-Range: bytes ");
				out.print(Long.toString(start));
				out.print("-");
				out.print(Long.toString(end));
				out.print("/");
				out.println(Long.toString(contentLength));
				out.println();
				out.append(cached.writeBody(start, length));
				out.println();
				out.print("--");
				out.println(boundary);
			}
			final Lock inUse = cached.open();
			res.setEntity(new ReadableHttpEntityChannel(type, -1, out,
					new Runnable() {
						public void run() {
							inUse.release();
						}
					}));
		}
	}

	private void sendMessageBody(String method, CachedEntity cached,
			HttpResponse res) throws IOException, InterruptedException {
		res.setHeader("Accept-Ranges", "bytes");
		String length = cached.getContentLength();
		if (length == null) {
			res.setHeader("Content-Length", "0");
		} else {
			res.setHeader("Content-Length", length);
		}
		if (!"HEAD".equals(method) && cached.isBodyPresent()) {
			String type = null;
			Header hd = res.getFirstHeader("Content-Type");
			if (hd != null) {
				type = hd.getValue();
			}
			if (cached.contentLength() == 0) {
				StringEntity entity = new StringEntity("");
				entity.setContentType(type);
				res.setEntity(entity);
			} else {
				final Lock inUse = cached.open();
				final File file = cached.getBody();
				NFileEntity entity = new NFileEntity(file, ContentType.create(type), true) {
					
					public String toString() {
						return file.toString();
					}

					public InputStream getContent() throws IOException {
						final ReadableByteChannel ch = newChannel(super
								.getContent());
						return newInputStream(new AutoCloseChannel(ch) {
							public String toString() {
								return file.toString();
							}

							public void close() throws IOException {
								try {
									super.close();
								} finally {
									inUse.release();
								}
							}
						});
					}

					public void close() throws IOException {
						try {
							super.close();
						} finally {
							inUse.release();
						}
					}
				};
				entity.setContentEncoding(res.getLastHeader("Content-Encoding"));
				res.setEntity(entity);
			}
		}
	}

	private void invalidate(Request headers) throws IOException {
		String loc = headers.getResolvedHeader("Location");
		String cloc = headers.getResolvedHeader("Content-Location");
		cache.invalidate(headers.getRequestURL(), loc, cloc);
	}
}
