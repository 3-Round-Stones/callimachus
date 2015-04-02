/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.rewrite;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.HeaderGroup;
import org.openrdf.annotations.Iri;
import org.openrdf.annotations.Type;
import org.openrdf.http.object.fluid.Fluid;
import org.openrdf.http.object.fluid.FluidBuilder;
import org.openrdf.http.object.fluid.FluidException;
import org.openrdf.http.object.fluid.FluidType;
import org.openrdf.repository.object.traits.ObjectMessage;

public class ProxyPostAdvice extends ProxyGetAdvice {
	private final Map<Method, Integer> bodyIndices = new HashMap<Method, Integer>(
			1);
	private String bodyIri;
	private FluidType bodyFluidType;

	public ProxyPostAdvice(String[] bindingNames, FluidType[] bindingTypes,
			URITemplate[] replacers, Method method) {
		super(bindingNames, bindingTypes, replacers, method);
		Annotation[][] panns = method.getParameterAnnotations();
		java.lang.reflect.Type[] gtypes = method.getGenericParameterTypes();
		for (int i = 0; i < panns.length; i++) {
			if (bindingNames[i] == null) {
				for (Annotation ann : panns[i]) {
					if (ann instanceof Type) {
						bodyIndices.put(method, i);
						String[] media = ((Type) ann).value();
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

	protected HttpUriRequest createRequest(String location,
			HeaderGroup headers, HttpEntity entity, ObjectMessage message,
			FluidBuilder fb) throws IOException, FluidException {
		Object target = message.getTarget();
		Integer bodyIndex = getBodyIndex(message.getMethod());
		HttpPost req = new HttpPost(location);
		req.setHeaders(headers.getAllHeaders());
		if (entity != null) {
			req.setEntity(entity);
		} else if (bodyIndex != null) {
			Object body = message.getParameters()[bodyIndex];
			Fluid fluid = fb.consume(body, getSystemId(), bodyFluidType);
			req.setEntity(fluid.asHttpEntity());
		} else if (target instanceof FileObject
				&& headers.containsHeader("Content-Type")) {
			FileObject file = (FileObject) target;
			InputStream in = file.openInputStream();
			if (in != null) {
				if (!headers.containsHeader("Content-Location")) {
					String uri = file.toUri().toASCIIString();
					req.setHeader("Content-Location", uri);
				}
				req.setEntity(new InputStreamEntity(in));
			}
		}
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

}
