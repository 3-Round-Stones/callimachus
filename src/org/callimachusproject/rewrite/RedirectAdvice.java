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

import java.lang.reflect.Method;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpResponse;
import org.openrdf.http.object.fluid.Fluid;
import org.openrdf.http.object.fluid.FluidBuilder;
import org.openrdf.http.object.fluid.FluidType;
import org.openrdf.repository.object.traits.ObjectMessage;

public class RedirectAdvice extends RewriteAdvice {
	private final StatusLine status;

	public RedirectAdvice(String[] bindingNames, FluidType[] bindingTypes,
			Substitution[] replacers, StatusLine status, Method method) {
		super(bindingNames, bindingTypes, replacers, method);
		this.status = status;
	}

	protected Fluid service(String location, Header[] headers,
			ObjectMessage message, FluidBuilder fb) {
		if (location == null)
			return fb.consume(null, null, HttpResponse.class, "message/http");
		HttpResponse resp = new BasicHttpResponse(status);
		resp.setHeader("Location", location);
		return fb.consume(resp, null, HttpResponse.class, "message/http");
	}

}
