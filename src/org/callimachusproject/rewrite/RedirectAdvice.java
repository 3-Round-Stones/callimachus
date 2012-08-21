package org.callimachusproject.rewrite;

import java.lang.reflect.Method;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpResponse;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.openrdf.repository.object.traits.ObjectMessage;

public class RedirectAdvice extends RewriteAdvice {
	private final StatusLine status;

	public RedirectAdvice(String[] bindingNames, Substitution[] replacers,
			StatusLine status, Method method) {
		super(bindingNames, replacers, method);
		this.status = status;
	}

	protected Fluid service(String location, Header[] headers,
			ObjectMessage message, FluidBuilder fb) {
		if (location == null)
			return fb.consume(null, null, HttpResponse.class, "message/http");
		HttpResponse resp = new BasicHttpResponse(status);
		resp.setHeaders(headers);
		resp.setHeader("Location", location);
		return fb.consume(resp, null, HttpResponse.class, "message/http");
	}

}
