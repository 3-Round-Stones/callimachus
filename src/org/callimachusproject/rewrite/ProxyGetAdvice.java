package org.callimachusproject.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.callimachusproject.server.exceptions.GatewayTimeout;

public class ProxyGetAdvice extends RewriteAdvice {

	public ProxyGetAdvice(String[] bindingNames, Substitution[] replacers,
			Method method) {
		super(bindingNames, replacers, method);
	}

	protected Fluid service(String location, Header[] headers, Object target,
			Object[] parameters, FluidBuilder fb) throws GatewayTimeout,
			IOException, FluidException {
		String[] returnMedia = getReturnMedia();
		if (location == null)
			return fb.media(returnMedia);
		String systemId = location;
		String redirect = systemId;
		HttpResponse resp = null;
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		for (int i = 0; i < 20 && redirect != null; i++) {
			systemId = redirect;
			HttpRequest req = createRequest(redirect, headers, target,
					parameters, fb);
			if (returnMedia.length > 0) {
				for (String media : returnMedia) {
					req.addHeader("Accept", media);
				}
				req.addHeader("Accept", "*/*");
			}
			resp = client.service(req);
			redirect = client.redirectLocation(redirect, resp);
		}
		String contentType = "*/*";
		InputStream content = null;
		if (resp.getEntity() != null) {
			content = resp.getEntity().getContent();
		}
		if (resp.getFirstHeader("Content-Type") != null) {
			contentType = resp.getFirstHeader("Content-Type").getValue();
		}
		return fb.stream(content, systemId, contentType);
	}

	protected HttpRequest createRequest(String location, Header[] headers,
			Object target, Object[] parameters, FluidBuilder fb)
			throws IOException, FluidException {
		BasicHttpRequest req = new BasicHttpRequest("GET", location);
		req.setHeaders(headers);
		return req;
	}

}
