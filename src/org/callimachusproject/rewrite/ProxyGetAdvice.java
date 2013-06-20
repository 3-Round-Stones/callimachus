package org.callimachusproject.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.callimachusproject.client.HttpUriResponse;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidType;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.traits.ObjectMessage;

public class ProxyGetAdvice extends RewriteAdvice {

	public ProxyGetAdvice(String[] bindingNames, FluidType[] bindingTypes,
			Substitution[] replacers, Method method) {
		super(bindingNames, bindingTypes, replacers, method);
	}

	protected Fluid service(String location, Header[] headers,
			ObjectMessage message, FluidBuilder fb) throws IOException,
			FluidException, ResponseException, OpenRDFException {
		String[] returnMedia = getReturnMedia();
		if (location == null)
			return fb.media(returnMedia);
		HttpUriRequest req = createRequest(location, headers, message, fb);
		if (returnMedia.length > 0) {
			for (String media : returnMedia) {
				req.addHeader("Accept", media);
			}
			req.addHeader("Accept", "*/*;q=0.1");
		}
		assert message.getTarget() instanceof CalliObject;
		CalliRepository repository = ((CalliObject) message.getTarget()).getCalliRepository();
		HttpUriResponse resp = repository.getHttpClient(getSystemId()).getResponse(req);
		String systemId = resp.getSystemId();
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

	protected HttpUriRequest createRequest(String location, Header[] headers,
			ObjectMessage message, FluidBuilder fb) throws IOException,
			FluidException {
		HttpGet req = new HttpGet(location);
		req.setHeaders(headers);
		return req;
	}

}
