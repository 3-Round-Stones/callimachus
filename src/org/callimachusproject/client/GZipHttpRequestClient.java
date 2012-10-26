package org.callimachusproject.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;

public class GZipHttpRequestClient extends AbstractHttpClient {
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

	public GZipHttpRequestClient(HttpClient client) {
		this.client = client;
	}

	@Override
	public HttpResponse execute(HttpHost host, HttpRequest request,
			HttpContext context) throws IOException, ClientProtocolException {
		if (request instanceof HttpEntityEnclosingRequest) {
			compress((HttpEntityEnclosingRequest) request);
		}
		return client.execute(host, request, context);
	}

	@Override
	public ClientConnectionManager getConnectionManager() {
		return client.getConnectionManager();
	}

	@Override
	public HttpParams getParams() {
		return client.getParams();
	}

	private void compress(HttpEntityEnclosingRequest req) {
		HttpEntity entity = req.getEntity();
		if (entity == null)
			return;
		Header cache = req.getFirstHeader("Cache-Control");
		if (cache != null && cache.getValue().contains("no-transform"))
			return;
		long length = getLength(req.getFirstHeader("Content-Length"), -1);
		length = getLength(entity, length);
		boolean big = length < 0 || length > 500;
		if (!req.containsHeader("Content-Encoding") && big && isCompressable(req)) {
			req.removeHeaders("Content-MD5");
			req.removeHeaders("Content-Length");
			req.setHeader("Content-Encoding", "gzip");
			req.addHeader("Warning", WARN_214);
			req.setEntity(gzip(entity));
		}
	}

	private HttpEntity gzip(HttpEntity entity) {
		if (entity instanceof GUnzipEntity)
			return ((GUnzipEntity) entity).getEntityDelegate();
		if (entity instanceof CloseableEntity) {
			CloseableEntity centity = (CloseableEntity) entity;
			centity.setEntityDelegate(gzip(centity.getEntityDelegate()));
			return centity;
		}
		return new GZipEntity(entity);
	}

	private long getLength(Header hd, int length) {
		if (hd == null)
			return length;
		try {
			return Long.parseLong(hd.getValue());
		} catch (NumberFormatException e) {
			return length;
		}
	}

	private long getLength(HttpEntity entity, long length) {
		if (entity == null)
			return length;
		return entity.getContentLength();
	}

	private boolean isCompressable(HttpMessage msg) {
		Header contentType = msg.getFirstHeader("Content-Type");
		if (contentType == null)
			return false;
		Header encoding = msg.getFirstHeader("Content-Encoding");
		Header cache = msg.getFirstHeader("Cache-Control");
		boolean identity = encoding == null || "identity".equals(encoding.getValue());
		boolean transformable = cache == null
				|| !cache.getValue().contains("no-transform");
		String type = contentType.getValue();
		boolean compressable = type.startsWith("text/")
				|| type.startsWith("application/xml")
				|| type.startsWith("application/x-turtle")
				|| type.startsWith("application/sparql-quey")
				|| type.startsWith("application/trix")
				|| type.startsWith("application/x-trig")
				|| type.startsWith("application/postscript")
				|| type.startsWith("application/javascript")
				|| type.startsWith("application/json")
				|| type.startsWith("application/mbox")
				|| type.startsWith("application/")
				&& (type.endsWith("+xml") || type.contains("+xml;"));
		return identity && compressable && transformable;
	}

}
