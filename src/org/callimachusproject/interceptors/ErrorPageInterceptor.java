/*
 * Copyright (c) 2015 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.interceptors;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.chain.HttpRequestChainInterceptor;
import org.openrdf.http.object.helpers.ObjectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorPageInterceptor implements HttpRequestChainInterceptor {
	private static final int MAX_PAGE_SIZE = 1024 * 1024; // 1MiB
	private final Logger logger = LoggerFactory.getLogger(ErrorPageInterceptor.class);

	@Override
	public HttpResponse intercept(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		return null;
	}

	@Override
	public void process(HttpRequest request, HttpResponse response,
			HttpContext context) throws HttpException, IOException {
		if (response.getStatusLine().getStatusCode() >= 400 && isHtml(response)) {
			String uri = request.getRequestLine().getUri();
			response.setEntity(format(uri, response.getEntity(), context));
		}
	}

	private boolean isHtml(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		return isHtml(response.getFirstHeader("Content-Type")) || entity != null && isHtml(entity.getContentType());
	}

	private boolean isHtml(Header contentType) {
		return contentType != null && contentType.getValue().startsWith("text/html");
	}

	private HttpEntity format(String reqUri, HttpEntity entity, HttpContext context) throws IOException {
		Header contentType = entity.getContentType();
		BufferedInputStream bin = new BufferedInputStream(entity.getContent(), MAX_PAGE_SIZE);
		bin.mark(MAX_PAGE_SIZE);
		long skipped = bin.skip(MAX_PAGE_SIZE);
		if (skipped < MAX_PAGE_SIZE) {
			ObjectContext ctx = ObjectContext.adapt(context);
			CalliObject target = (CalliObject) ctx.getResourceTarget().getTargetObject();
			try {
				DetachedRealm realm = target.getRealm();
				int qidx = reqUri.indexOf('?');
				String qs = qidx < 0 ? "" : reqUri.substring(qidx + 1);
				bin.reset();
				InputStream page = realm.transformErrorPage(bin, target.toString(), qs);
				return entity(page, contentType);
			} catch (OpenRDFException e) {
				logger.error(e.toString(), e);
			} catch (IOException e) {
				logger.error(e.toString(), e);
			}
		}
		bin.reset();
		return entity(bin, contentType);
	}

	private InputStreamEntity entity(InputStream bin, Header contentType) {
		InputStreamEntity replacement = new InputStreamEntity(bin);
		replacement.setContentType(contentType);
		return replacement;
	}

}
