package org.callimachusproject.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class GUnZipHttpResponseClient extends AbstractHttpClient {
	private static String hostname;
	static {
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = "AliBaba";
		}
	}
	private static String WARN_214 = "214 " + hostname
			+ " \"Transformation applied\"";
	private final HttpClient client;

	public GUnZipHttpResponseClient(HttpClient client) {
		this.client = client;
	}

	@Override
	public HttpResponse execute(HttpHost host, HttpRequest request,
			HttpContext context) throws IOException, ClientProtocolException {
		acceptEncoding(request);
		HttpResponse resp = client.execute(host, request, context);
		return removeEncoding(request, resp);
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		return client.getConnectionManager();
	}

	@Override
	public HttpParams getParams() {
		return client.getParams();
	}

	private void acceptEncoding(HttpRequest req) {
		if (!req.containsHeader("Accept-Encoding")) {
			req.setHeader("Accept-Encoding", "gzip");
		}
	}

	private HttpResponse removeEncoding(HttpRequest req, HttpResponse resp) {
		if (resp == null)
			return resp;
		HttpEntity entity = resp.getEntity();
		if (entity == null)
			return resp;
		Header cache = req.getFirstHeader("Cache-Control");
		if (cache != null && cache.getValue().contains("no-transform"))
			return resp;
		Header encoding = resp.getFirstHeader("Content-Encoding");
		if (encoding != null && "gzip".equals(encoding.getValue())) {
			resp.removeHeaders("Content-MD5");
			resp.removeHeaders("Content-Length");
			resp.setHeader("Content-Encoding", "identity");
			resp.addHeader("Warning", WARN_214);
			resp.setEntity(gunzip(entity));
		}
		return resp;
	}

	private HttpEntity gunzip(HttpEntity entity) {
		if (entity instanceof GZipEntity)
			return ((GZipEntity) entity).getEntityDelegate();
		if (entity instanceof CloseableEntity) {
			CloseableEntity centity = (CloseableEntity) entity;
			centity.setEntityDelegate(gunzip(centity.getEntityDelegate()));
			return centity;
		}
		return new GUnzipEntity(entity);
	}

}
