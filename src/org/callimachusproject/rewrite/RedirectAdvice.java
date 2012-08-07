package org.callimachusproject.rewrite;

import java.lang.reflect.Method;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpResponse;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;

public class RedirectAdvice extends CopyAdvice {
	private final StatusLine status;

	public RedirectAdvice(String[] bindingNames,
			Substitution[] replacers, StatusLine status, Method method) {
		super(bindingNames, replacers, method);
		this.status = status;
	}

	protected Fluid service(String location, FluidBuilder fb) {
		if (location == null)
			return fb.consume(null, null, HttpResponse.class, "message/http");
		HttpResponse resp = new BasicHttpResponse(status);
		resp.setHeader("Location", location);
		return fb.consume(resp, null, HttpResponse.class, "message/http");
	}

}
