package org.callimachusproject.client;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.util.DomainNameSystemResolver;

public class GZipInterceptor implements HttpRequestInterceptor {
	private static final String hostname = DomainNameSystemResolver.getInstance().getLocalHostName();
	private static final String WARN_214 = "214 " + hostname
			+ " \"Transformation applied\"";

	@Override
	public void process(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		if (!request.containsHeader("Accept-Encoding")) {
			request.setHeader("Accept-Encoding", "gzip");
		}
		if (request instanceof HttpEntityEnclosingRequest) {
			compress((HttpEntityEnclosingRequest) request);
		}
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
