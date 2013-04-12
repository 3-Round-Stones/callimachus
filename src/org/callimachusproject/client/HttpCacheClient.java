package org.callimachusproject.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.cache.ResourceFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.cache.CacheConfig;
import org.apache.http.impl.client.cache.CachingHttpClient;
import org.apache.http.impl.client.cache.ManagedHttpCacheStorage;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.server.exceptions.GatewayTimeout;
import org.callimachusproject.server.exceptions.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpCacheClient extends AbstractHttpClient implements HttpUriClient {
	final Logger logger = LoggerFactory.getLogger(HttpClientManager.class);
	private final HttpClient client;
	private final ManagedHttpCacheStorage storage;

	public HttpCacheClient(HttpClient client, ResourceFactory factory,
			CacheConfig config) {
		this.storage = new ManagedHttpCacheStorage(config);
		CachingHttpClient cache = new CachingHttpClient(client, factory,
				this.storage, config);
		this.client = new GUnZipHttpResponseClient(cache);
	}

	@Override
	protected void finalize() throws Throwable {
		storage.shutdown();
	}

	public void cleanResources() {
		storage.cleanResources();
	}

	public void shutdown() {
		storage.shutdown();
	}

	/**
	 * Follows redirects and returns the final 200 or 203 response with the
	 * final request URL represented as the response header Content-Location
	 */
	public HttpUriEntity getEntity(String url, String accept)
			throws IOException, ResponseException {
		return getEntity(url,
				new Header[] { new BasicHeader("Accept", accept) });
	}

	/**
	 * Follows redirects and returns the final 200 or 203 response with the
	 * final request URL represented as the response header Content-Location
	 */
	public HttpUriEntity getEntity(String url, Header[] headers)
			throws IOException, ResponseException {
		HttpGet req = new HttpGet(url);
		req.setHeaders(headers);
		HttpUriResponse resp = getResponse(req);
		if (resp == null || resp.getEntity() == null)
			return null;
		return resp.getEntity();
	}

	/**
	 * Follows redirects and returns the final 200 or 203 response with the
	 * final request URL represented as the response header Content-Location
	 */
	public HttpUriResponse getResponse(HttpUriRequest request)
			throws IOException, ResponseException {
		String frag = null;
		String redirect = request.getURI().toASCIIString();
		String systemId = redirect;
		HttpResponse resp = null;
		for (int i = 0; i < 20 && redirect != null; i++) {
			int hash = redirect.indexOf('#');
			if (hash > 0) {
				frag = redirect.substring(hash);
				redirect = redirect.substring(0, hash);
			}
			systemId = redirect;
			if (request instanceof HttpEntityEnclosingRequest) {
				HttpEntityEnclosingRequest req = new BasicHttpEntityEnclosingRequest(
						request.getMethod(), systemId);
				req.setHeaders(request.getAllHeaders());
				req.setEntity(((HttpEntityEnclosingRequest) request)
						.getEntity());
				resp = this.service(req);
				redirect = null;
			} else {
				HttpRequest req = new BasicHttpRequest(request.getMethod(),
						systemId);
				req.setHeaders(request.getAllHeaders());
				resp = this.service(req);
				redirect = this.redirectLocation(redirect, resp);
			}
		}
		if (frag != null) {
			systemId = systemId + frag;
		}
		int code = resp.getStatusLine().getStatusCode();
		if (code < 200 || code >= 300)
			throw ResponseException.create(resp, systemId);
		return new HttpUriResponse(systemId, resp);
	}

	@Override
	public HttpResponse execute(final HttpHost host, final HttpRequest request,
			HttpContext context) throws IOException, ClientProtocolException {
		HttpResponse resp = client.execute(host, request, context);
		HttpEntity entity = resp.getEntity();
		if (entity != null) {
			resp.setEntity(new CloseableEntity(entity, new Closeable() {
				public void close() throws IOException {
					// this also keeps this object from being finalized
					// until all its response entities are consumed
					String uri = request.getRequestLine().getUri();
					logger.debug("Remote {}{} closed", host, uri);
				}
			}));
		}
		return resp;
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		return client.getConnectionManager();
	}

	@Override
	public HttpParams getParams() {
		return client.getParams();
	}

	/**
	 * {@link HttpEntity#consumeContent()} or
	 * {@link HttpEntity#writeTo(java.io.OutputStream)} must be called if
	 * {@link HttpResponse#getEntity()} is non-null.
	 */
	private HttpResponse service(HttpRequest request) throws IOException,
			GatewayTimeout {
		try {
			return execute(determineTarget(request), request);
		} catch (java.net.ConnectException e) {
			throw new GatewayTimeout(e);
		}
	}

	private String redirectLocation(String base, HttpResponse resp) throws IOException {
		int code = resp.getStatusLine().getStatusCode();
		if (code == 301 || code == 302 || code == 307 || code == 308) {
			Header location = resp.getFirstHeader("Location");
			if (location != null) {
				EntityUtils.consume(resp.getEntity());
				String value = location.getValue();
				if (value.startsWith("/") || !value.contains(":")) {
					try {
						value = TermFactory.newInstance(base).resolve(value);
					} catch (IllegalArgumentException e) {
						logger.warn(e.toString(), e);
						return value;
					}
				}
				return value;
			}
		}
		return null;
	}

	private static HttpHost determineTarget(HttpRequest request)
			throws ClientProtocolException {
		// A null target may be acceptable if there is a default target.
		// Otherwise, the null target is detected in the director.
		HttpHost target = null;

		URI requestURI = URI.create(request.getRequestLine().getUri());
		if (requestURI.isAbsolute()) {
			target = URIUtils.extractHost(requestURI);
			if (target == null) {
				throw new ClientProtocolException(
						"URI does not specify a valid host name: " + requestURI);
			}
		}
		return target;
	}

}
