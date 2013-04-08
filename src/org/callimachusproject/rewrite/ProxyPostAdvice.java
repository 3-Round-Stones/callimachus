package org.callimachusproject.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.callimachusproject.annotations.type;
import org.callimachusproject.client.StreamEntity;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidException;
import org.callimachusproject.fluid.FluidType;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.object.traits.ObjectMessage;

public class ProxyPostAdvice extends ProxyGetAdvice {
	private final Map<Method, Integer> bodyIndices = new HashMap<Method, Integer>(
			1);
	private String bodyIri;
	private FluidType bodyFluidType;

	public ProxyPostAdvice(String[] bindingNames, FluidType[] bindingTypes,
			Substitution[] replacers, Method method) {
		super(bindingNames, bindingTypes, replacers, method);
		Annotation[][] panns = method.getParameterAnnotations();
		Type[] gtypes = method.getGenericParameterTypes();
		for (int i = 0; i < panns.length; i++) {
			if (bindingNames[i] == null) {
				for (Annotation ann : panns[i]) {
					if (ann instanceof type) {
						bodyIndices.put(method, i);
						String[] media = ((type) ann).value();
						bodyFluidType = new FluidType(gtypes[i], media);
						for (Annotation bann : panns[i]) {
							if (bann instanceof Iri) {
								bodyIri = ((Iri) bann).value();
							}
						}
					}
				}
			}
		}
	}

	protected HttpUriRequest createRequest(String location, Header[] headers,
			ObjectMessage message, FluidBuilder fb) throws IOException,
			FluidException {
		Object target = message.getTarget();
		Integer bodyIndex = getBodyIndex(message.getMethod());
		if (bodyIndex == null && target instanceof FileObject
				&& contains(headers, "Content-Type")) {
			FileObject file = (FileObject) target;
			InputStream in = file.openInputStream();
			if (in != null) {
				HttpPost req = new HttpPost(location);
				req.setHeaders(headers);
				if (!contains(headers, "Content-Location")) {
					String uri = file.toUri().toASCIIString();
					req.setHeader("Content-Location", uri);
				}
				req.setEntity(new StreamEntity(in));
				return req;
			}
		}
		if (bodyIndex == null) {
			HttpPost req = new HttpPost(location);
			req.setHeaders(headers);
			return req;
		}
		HttpPost req = new HttpPost(location);
		req.setHeaders(headers);
		Object body = message.getParameters()[bodyIndex];
		Fluid fluid = fb.consume(body, getSystemId(), bodyFluidType);
		req.setEntity(fluid.asHttpEntity());
		return req;
	}

	private synchronized Integer getBodyIndex(Method method) {
		if (bodyFluidType == null)
			return null;
		Integer ret = bodyIndices.get(method);
		if (ret != null)
			return ret;
		Annotation[][] panns = method.getParameterAnnotations();
		for (int i = panns.length - 1; i >= 0; i--) {
			for (Annotation ann : panns[i]) {
				if (ann instanceof Iri) {
					if (((Iri) ann).value().equals(bodyIri)) {
						bodyIndices.put(method, i);
						return i;
					}
				}
			}
		}
		return null;
	}

	private boolean contains(Header[] headers, String string) {
		for (Header hd : headers) {
			if (hd.getName().equalsIgnoreCase(string))
				return true;
		}
		return false;
	}

}
