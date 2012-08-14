package org.callimachusproject.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import javax.tools.FileObject;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.annotations.type;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;

public class ProxyPostAdvice extends ProxyGetAdvice {
	private int bodyIndex;
	private Type bodyType;
	private String[] bodyMedia;

	public ProxyPostAdvice(String[] bindingNames, Substitution[] replacers,
			Method method) {
		super(bindingNames, replacers, method);
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

	protected HttpRequest createRequest(String location, Header[] headers,
			Object target, Object[] parameters, FluidBuilder fb)
			throws IOException, FluidException {
		if (bodyType == null && target instanceof FileObject && contains(headers, "Content-Type")) {
			FileObject file = (FileObject)target;
			InputStream in = file.openInputStream();
			if (in != null) {
				BasicHttpEntityEnclosingRequest req;
				req = new BasicHttpEntityEnclosingRequest("POST", location);
				req.setHeaders(headers);
				req.setEntity(new InputStreamEntity(in, -1));
				return req;
			}
		}
		if (bodyType == null) {
			BasicHttpRequest req = new BasicHttpRequest("POST", location);
			req.setHeaders(headers);
			return req;
		}
		BasicHttpEntityEnclosingRequest req;
		req = new BasicHttpEntityEnclosingRequest("POST", location);
		req.setHeaders(headers);
		Object body = parameters[bodyIndex];
		Fluid fluid = fb.consume(body, getSystemId(), bodyType, bodyMedia);
		req.setEntity(fluid.asHttpEntity());
		return req;
	}

	private boolean contains(Header[] headers, String string) {
		for (Header hd :headers) {
			if (hd.getName().equalsIgnoreCase(string))
				return true;
		}
		return false;
	}

}
