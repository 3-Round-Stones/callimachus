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
import java.lang.reflect.Type;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHttpResponse;
import org.callimachusproject.annotations.type;
import org.callimachusproject.fluid.Fluid;
import org.callimachusproject.fluid.FluidBuilder;
import org.callimachusproject.fluid.FluidFactory;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.advice.Advice;
import org.openrdf.repository.object.traits.ObjectMessage;

public class MissingAdvice implements Advice {
	private final StatusLine status;
	private final Type returnType;
	private final String[] returnMedia;

	public MissingAdvice(StatusLine status, Method method) {
		this.status = status;
		this.returnType = method.getGenericReturnType();
		this.returnMedia = getMediaType(method);
	}

	public Object intercept(ObjectMessage message) throws Exception {
		Object target = message.getTarget();
		ObjectConnection con = null;
		if (target instanceof RDFObject) {
			con = ((RDFObject) target).getObjectConnection();
		}
		FluidBuilder fb = FluidFactory.getInstance().builder(con);
		return service(fb).as(returnType, returnMedia);
	}

	protected Fluid service(FluidBuilder fb) {
		HttpResponse resp = new BasicHttpResponse(status);
		return fb.consume(resp, null, HttpResponse.class, "message/http");
	}

	private String[] getMediaType(Method method) {
		if (method.isAnnotationPresent(type.class))
			return method.getAnnotation(type.class).value();
		return new String[0];
	}

}
