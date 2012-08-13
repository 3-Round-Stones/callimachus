package org.callimachusproject.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.annotations.type;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.callimachusproject.server.exceptions.GatewayTimeout;

public class ProxyAdvice extends RewriteAdvice {
	private final String requestMethod;
	private int bodyIndex;
	private Type bodyType;
	private String[] bodyMedia;

	public ProxyAdvice(String[] bindingNames, Substitution[] replacers,
			String requestMethod, Method method) {
		super(bindingNames, replacers, method);
		this.requestMethod = requestMethod;
		Annotation[][] panns = method.getParameterAnnotations();
		for (int i = 0; i < panns.length; i++) {
			if (bindingNames[i] == null) {
				for (Annotation ann : panns[i]) {
					if (ann instanceof type) {
						bodyIndex = i;
						bodyType = method.getGenericParameterTypes()[i];
						bodyMedia = ((type) ann).value();
					}
				}
			}
		}
	}

	protected Fluid service(String location, Object[] parameters,
			FluidBuilder fb) throws GatewayTimeout, IOException, FluidException {
		String[] returnMedia = getReturnMedia();
		if (location == null)
			return fb.media(returnMedia);
		String systemId = location;
		String redirect = systemId;
		HttpResponse resp = null;
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		for (int i = 0; i < 20 && redirect != null; i++) {
			systemId = redirect;
			HttpRequest req = createRequest(redirect, parameters, fb);
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

	private HttpRequest createRequest(String target, Object[] parameters,
			FluidBuilder fb) throws IOException, FluidException {
		if (bodyType == null)
			return new BasicHttpRequest(requestMethod, target);
		BasicHttpEntityEnclosingRequest req;
		req = new BasicHttpEntityEnclosingRequest(requestMethod, target);
		Object body = parameters[bodyIndex];
		Fluid fluid = fb.consume(body, getSystemId(), bodyType, bodyMedia);
		req.setEntity(fluid.asHttpEntity());
		return req;
	}

}
