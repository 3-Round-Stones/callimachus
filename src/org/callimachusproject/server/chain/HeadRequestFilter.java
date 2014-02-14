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
package org.callimachusproject.server.chain;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.EntityUtils;

public class HeadRequestFilter implements HttpResponseInterceptor {

	@Override
	public void process(HttpResponse response, HttpContext context)
			throws HttpException, IOException {
		HttpRequest request = HttpCoreContext.adapt(context).getRequest();
		HttpEntity entity = response.getEntity();
		if (request != null
				&& "HEAD".equals(request.getRequestLine().getMethod())
				&& entity != null) {
			EntityUtils.consumeQuietly(entity);
			response.setEntity(null);
		}
	}

}
