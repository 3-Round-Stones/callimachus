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
import java.lang.reflect.Method;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.client.HttpUriResponse;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.fluid.Fluid;
import org.openrdf.http.object.fluid.FluidBuilder;
import org.openrdf.http.object.fluid.FluidException;
import org.openrdf.http.object.fluid.FluidType;
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
