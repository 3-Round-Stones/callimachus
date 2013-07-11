package org.callimachusproject.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.server.exceptions.ResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class HttpUriClient extends CloseableHttpClient implements HttpClient {
	private final Logger logger = LoggerFactory.getLogger(HttpUriClient.class);

	protected abstract HttpClient getDelegate() throws IOException;

	public HttpUriEntity getEntity(String url, String accept)
			throws IOException, ResponseException {
		HttpGet req = new HttpGet(url);
		req.setHeader("Accept", accept);
		return getResponse(req).getEntity();
	}

	public HttpUriResponse getResponse(HttpUriRequest request)
			throws IOException, ResponseException {
		HttpUriResponse response = getAnyResponse(request);
		int code = response.getStatusLine().getStatusCode();
		if (code < 200 || code >= 300)
			throw ResponseException.create(response, response.getSystemId());
		return response;
	}

	public HttpUriResponse getAnyResponse(HttpUriRequest request)
			throws IOException, ResponseException {
		HttpClientContext ctx = HttpClientContext.create();
		ctx.setCookieStore(new BasicCookieStore());
		CloseableHttpResponse response = execute(request, ctx);
		URI systemId;
		if (response instanceof HttpUriResponse) {
			systemId = ((HttpUriResponse) response).getURI();
		} else {
			systemId = getSystemId(ctx);
		}
		if (response instanceof HttpUriResponse) {
			return (HttpUriResponse) response;
		} else {
			return new HttpUriResponse(systemId.toASCIIString(), response);
		}
	}

	@Override
	protected CloseableHttpResponse doExecute(HttpHost target,
			HttpRequest request, HttpContext context) throws IOException,
			ClientProtocolException {
		HttpClientContext ctx;
		if (context == null) {
			ctx = HttpClientContext.create();
		} else {
			ctx = HttpClientContext.adapt(context);
		}
		if (ctx.getCookieStore() == null) {
			ctx.setCookieStore(new BasicCookieStore());
		}
		HttpResponse response = getDelegate().execute(target, request, ctx);
		if (response instanceof CloseableHttpResponse) {
			return (CloseableHttpResponse) response;
		} else {
			return new HttpUriResponse(getSystemId(ctx).toASCIIString(), response);
		}
	}

	@Override
	public void close() throws IOException {
		HttpClient delegate = getDelegate();
		if (delegate instanceof CloseableHttpClient) {
			((CloseableHttpClient) delegate).close();
		}
	}

	@Override
	public HttpParams getParams() {
		try {
			return getDelegate().getParams();
		} catch (IOException e) {
			logger.error(e.toString(), e);
			return null;
		}
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		try {
			return getDelegate().getConnectionManager();
		} catch (IOException e) {
			logger.error(e.toString(), e);
			return null;
		}
	}

	private URI getSystemId(HttpClientContext ctx) {
		HttpUriRequest request = (HttpUriRequest) ctx.getRequest();
		try {
			URI original = request.getURI();
			HttpHost target = ctx.getTargetHost();
			RedirectLocations redirects = (RedirectLocations) ctx.getAttribute(DefaultRedirectStrategy.REDIRECT_LOCATIONS);
			List<URI> list = redirects == null ? null : redirects.getAll();
			URI absolute = URIUtils.resolve(original, target, list);
			return new URI(TermFactory.newInstance(absolute.toASCIIString()).getSystemId());
		} catch (URISyntaxException e) {
			return request.getURI();
		}
	}
}
