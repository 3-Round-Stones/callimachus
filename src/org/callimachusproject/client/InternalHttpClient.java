package org.callimachusproject.client;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class InternalHttpClient extends AbstractHttpClient {
	private ConcurrentMap<HttpHost, HttpClient> proxies = new ConcurrentHashMap<HttpHost, HttpClient>();
	private final HttpClient client;

	public InternalHttpClient(HttpClient client) {
		this.client = client;
	}

	public HttpClient setProxy(HttpHost destination, HttpClient proxy) {
		return proxies.put(destination, proxy);
	}

	public boolean removeProxy(HttpHost destination, HttpClient proxy) {
		return proxies.remove(destination, proxy);
	}

	public HttpResponse execute(HttpHost host, HttpRequest request,
			HttpContext context) throws IOException, ClientProtocolException {
		HttpClient proxy = proxies.get(host);
		if (proxy != null)
			return proxy.execute(host, request, context);
		return client.execute(host, request, context);
	}

	public ClientConnectionManager getConnectionManager() {
		return client.getConnectionManager();
	}

	public HttpParams getParams() {
		return client.getParams();
	}

}
