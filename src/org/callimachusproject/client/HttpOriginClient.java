package org.callimachusproject.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.ClientProtocolException;
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

/**
 * A shared HttpClient implementation.
 * 
 * @author James Leigh
 *
 */
public class HttpOriginClient extends CloseableHttpClient implements HttpUriClient {
	private final HttpClientManager manager = HttpClientManager.getInstance();
	private final CloseableHttpClient delegate;

	public HttpOriginClient(String source) {
		delegate = manager.createHttpClient(source);
	}

	@Override
	public HttpUriEntity getEntity(String url, String accept)
			throws IOException, ResponseException {
		HttpGet req = new HttpGet(url);
		req.setHeader("Accept", accept);
		return getResponse(req).getEntity();
	}

	@Override
	public HttpUriResponse getResponse(HttpUriRequest request)
			throws IOException, ResponseException {
		HttpClientContext ctx = HttpClientContext.create();
		ctx.setCookieStore(new BasicCookieStore());
		CloseableHttpResponse response = execute(request, ctx);
		URI systemId;
		if (response instanceof HttpUriResponse) {
			systemId = ((HttpUriResponse) response).getURI();
		} else {
			try {
				URI original = request.getURI();
				HttpHost target = ctx.getTargetHost();
				RedirectLocations redirects = (RedirectLocations) ctx.getAttribute(DefaultRedirectStrategy.REDIRECT_LOCATIONS);
				List<URI> list = redirects == null ? null : redirects.getAll();
				URI absolute = URIUtils.resolve(original, target, list);
				systemId = new URI(TermFactory.newInstance(absolute.toASCIIString()).getSystemId());
			} catch (URISyntaxException e) {
				systemId = request.getURI();
			}
		}
		int code = response.getStatusLine().getStatusCode();
		if (code < 200 || code >= 300)
			throw ResponseException.create(response, systemId.toASCIIString());
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
		return delegate.execute(target, request, ctx);
	}

	@Override
	public void close() throws IOException {
		delegate.close();
	}

	@Override
	public HttpParams getParams() {
		return delegate.getParams();
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		return delegate.getConnectionManager();
	}

}
