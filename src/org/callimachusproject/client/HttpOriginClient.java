package org.callimachusproject.client;

import static org.apache.http.client.params.ClientPNames.COOKIE_POLICY;
import static org.apache.http.client.params.ClientPNames.HANDLE_AUTHENTICATION;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.ConnectionReuseStrategy;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.UserTokenHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.client.protocol.RequestAddCookies;
import org.apache.http.client.protocol.RequestAuthCache;
import org.apache.http.client.protocol.RequestClientConnControl;
import org.apache.http.client.protocol.RequestDefaultHeaders;
import org.apache.http.client.protocol.RequestProxyAuthentication;
import org.apache.http.client.protocol.RequestTargetAuthentication;
import org.apache.http.client.protocol.ResponseProcessCookies;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.params.SyncBasicHttpParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.callimachusproject.server.exceptions.ResponseException;

/**
 * A shared HttpClient implementation.
 * 
 * @author James Leigh
 *
 */
public class HttpOriginClient extends AbstractHttpClient implements HttpUriClient {
	private final String origin;

	public HttpOriginClient(String origin) {
		super(null, null);
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
		setRedirectStrategy(new StoreFragementRedirectStrategy());
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
		HttpContext localContext = createHttpContext();
		HttpResponse response = execute(request, localContext);
		HttpHost host = (HttpHost) localContext.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
		HttpUriRequest req = (HttpUriRequest) localContext.getAttribute(ExecutionContext.HTTP_REQUEST);
		URI location = (URI) localContext.getAttribute(StoreFragementRedirectStrategy.HTTP_LOCATION);
		try {
			URI root = new URI(host.getSchemeName(), null, host.getHostName(), host.getPort(), "/", null, null);
			URI systemId = root.resolve(req.getURI());
			if (location != null) {
				systemId = systemId.resolve(location);
			}
			int code = response.getStatusLine().getStatusCode();
			if (code < 200 || code >= 300)
				throw ResponseException.create(response, systemId.toASCIIString());
			return new HttpUriResponse(systemId.toASCIIString(), response);
		} catch (URISyntaxException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	protected RequestDirector createClientRequestDirector(
			HttpRequestExecutor requestExec, ClientConnectionManager conman,
			ConnectionReuseStrategy reustrat,
			ConnectionKeepAliveStrategy kastrat, HttpRoutePlanner rouplan,
			HttpProcessor httpProcessor, HttpRequestRetryHandler retryHandler,
			RedirectStrategy redirectStrategy,
			AuthenticationStrategy targetAuthStrategy,
			AuthenticationStrategy proxyAuthStrategy,
			UserTokenHandler userTokenHandler, HttpParams params) {
		HttpClient client = HttpClientManager.getInstance().getClient();
		return new DelegatingRequestDirector(requestExec, httpProcessor,
				redirectStrategy, targetAuthStrategy, client, params);
	}

    @Override
	protected ClientConnectionManager createClientConnectionManager() {
		return HttpClientManager.getInstance().getClient().getConnectionManager();
	}

	@Override
	protected HttpParams createHttpParams() {
		SyncBasicHttpParams params = new SyncBasicHttpParams();
		params.setParameter(COOKIE_POLICY, CookiePolicy.RFC_2109);
		params.setBooleanParameter(HANDLE_AUTHENTICATION, false);
		params.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, true);
		return params;
	}

	@Override
	protected BasicHttpProcessor createHttpProcessor() {
        BasicHttpProcessor httpproc = new BasicHttpProcessor();
        httpproc.addInterceptor(new RequestDefaultHeaders());
        // Required protocol interceptors
        httpproc.addInterceptor(new RequestContent());
        httpproc.addInterceptor(new RequestTargetHost());
        // Recommended protocol interceptors
        httpproc.addInterceptor(new RequestClientConnControl());
        httpproc.addInterceptor(new RequestUserAgent());
        httpproc.addInterceptor(new RequestExpectContinue());
        // HTTP state management interceptors
        httpproc.addInterceptor(new RequestAddCookies());
        httpproc.addInterceptor(new ResponseProcessCookies());
        // HTTP authentication interceptors
        httpproc.addInterceptor(new RequestAuthCache());
        httpproc.addInterceptor(new RequestTargetAuthentication());
        httpproc.addInterceptor(new RequestProxyAuthentication());
        httpproc.addInterceptor(new RequestAddOrigin(origin));
        GUnzipInterceptor gunzip = new GUnzipInterceptor();
		httpproc.addRequestInterceptor(gunzip);
        httpproc.addResponseInterceptor(gunzip);
        return httpproc;
    }

}
