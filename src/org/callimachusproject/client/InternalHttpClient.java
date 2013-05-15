package org.callimachusproject.client;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RequestDirector;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class InternalHttpClient extends OverloadedHttpClient {
	private ConcurrentMap<HttpHost, RequestDirector> proxies = new ConcurrentHashMap<HttpHost, RequestDirector>();
	private final HttpClient client;

	public InternalHttpClient(HttpClient client) {
		this.client = client;
	}

	public RequestDirector getProxy(HttpHost host) {
		return proxies.get(host);
	}

	public RequestDirector setProxy(HttpHost host, RequestDirector proxy) {
		if (proxy == null) {
			return proxies.remove(host);
		} else {
			return proxies.put(host, proxy);
		}
	}

	public boolean removeProxy(HttpHost host, RequestDirector proxy) {
		return proxies.remove(host, proxy);
	}

	public boolean removeProxy(RequestDirector proxy) {
		return proxies.values().removeAll(Collections.singleton(proxy));
	}

	public HttpResponse execute(HttpHost host, HttpRequest request,
			HttpContext context) throws IOException {
		if (host != null) {
			RequestDirector proxy = proxies.get(host);
			if (proxy != null) {
				try {
					return proxy.execute(host, request, context);
				} catch (final HttpException httpException) {
					throw new ClientProtocolException(httpException);
				}
			}
		}
		return client.execute(host, request, context);
	}

	public ClientConnectionManager getConnectionManager() {
		return client.getConnectionManager();
	}

	public HttpParams getParams() {
		return client.getParams();
	}

}
