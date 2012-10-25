package org.callimachusproject.client;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class UnavailableHttpClient extends AbstractHttpClient {
	private static final BasicHttpResponse _503 = new BasicHttpResponse(HttpVersion.HTTP_1_1, 503, "Service Disconnected");
	private static final ClientConnectionManager manager = new BasicClientConnectionManager();
	static {
		manager.shutdown();
	}

	@Override
	public HttpResponse execute(HttpHost arg0, HttpRequest arg1,
			HttpContext arg2) throws IOException, ClientProtocolException {
		return _503;
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		return manager;
	}

	@Override
	public HttpParams getParams() {
		return new BasicHttpParams();
	}

}
