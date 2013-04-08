package org.callimachusproject.client;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.server.exceptions.ResponseException;

public class HttpOriginClient extends AbstractHttpClient implements HttpUriClient {
	private final String origin;

	public HttpOriginClient(String origin) {
		assert origin != null;
		int scheme = origin.indexOf("://");
		if (scheme < 0 && (origin.startsWith("file:") || origin.startsWith("jar:file:"))) {
			this.origin = "file://";
		} else {
			if (scheme < 0)
				throw new IllegalArgumentException("Not an absolute hierarchical URI: " + origin);
			int path = origin.indexOf('/', scheme + 3);
			if (path >= 0) {
				this.origin = origin.substring(0, path);
			} else {
				this.origin = origin;
			}
		}
	}

	@Override
	public String toString() {
		return origin;
	}

	private HttpUriClient getDelegate() {
		return HttpClientManager.getInstance().getClient();
	}

	@Override
	public HttpParams getParams() {
		return getDelegate().getParams();
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		return getDelegate().getConnectionManager();
	}

	@Override
	public HttpResponse execute(HttpHost target, HttpRequest request,
			HttpContext context) throws IOException, ClientProtocolException {
		request.addHeader("Origin", origin);
		return getDelegate().execute(target, request, context);
	}

	@Override
	public HttpUriEntity getEntity(String url, String accept)
			throws IOException, ResponseException {
		HttpGet req = new HttpGet(url);
		req.setHeader("Accept", accept);
		req.addHeader("Origin", origin);
		return getDelegate().getResponse(req).getEntity();
	}

	@Override
	public HttpUriEntity getEntity(String url, Header[] headers)
			throws IOException, ResponseException {
		HttpGet req = new HttpGet(url);
		req.setHeaders(headers);
		req.addHeader("Origin", origin);
		return getDelegate().getResponse(req).getEntity();
	}

	@Override
	public HttpUriResponse getResponse(HttpUriRequest request)
			throws IOException, ResponseException {
		request.addHeader("Origin", origin);
		return getDelegate().getResponse(request);
	}
}
