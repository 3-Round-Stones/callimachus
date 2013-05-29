package org.callimachusproject.client;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.callimachusproject.util.DomainNameSystemResolver;

public class GUnzipInterceptor implements HttpResponseInterceptor {
	private static final String hostname = DomainNameSystemResolver.getInstance().getLocalHostName();
	private static final String WARN_214 = "214 " + hostname
			+ " \"Transformation applied\"";

	@Override
	public void process(HttpResponse resp, HttpContext context)
			throws HttpException, IOException {
		HttpRequest req = (HttpRequest) context.getAttribute(HttpCoreContext.HTTP_REQUEST);
		HttpEntity entity = resp.getEntity();
		if (entity == null)
			return;
		Header cache = req.getFirstHeader("Cache-Control");
		if (cache != null && cache.getValue().contains("no-transform"))
			return;
		Header encoding = resp.getFirstHeader("Content-Encoding");
		if (encoding != null && "gzip".equals(encoding.getValue())) {
			resp.removeHeaders("Content-MD5");
			resp.removeHeaders("Content-Length");
			resp.setHeader("Content-Encoding", "identity");
			resp.addHeader("Warning", WARN_214);
			resp.setEntity(gunzip(entity));
		}
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
