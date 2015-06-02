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
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.chain.HttpRequestChainInterceptor;
import org.openrdf.http.object.client.StreamingHttpEntity;
import org.openrdf.http.object.helpers.ObjectContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcException;

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
		int code = response.getStatusLine().getStatusCode();
		if (code >= 400 && code != 401 && isHtml(response)) {
			String uri = request.getRequestLine().getUri();
			format(uri, response, context);
		}
	}

	private boolean isHtml(HttpResponse response) {
		HttpEntity entity = response.getEntity();
		return isHtml(response.getFirstHeader("Content-Type")) || entity != null && isHtml(entity.getContentType());
	}

	private boolean isHtml(Header contentType) {
		return contentType != null && contentType.getValue().startsWith("text/html");
	}

	private void format(String reqUri, HttpResponse response,
			HttpContext context) throws IOException {
		BufferedInputStream bin = bufferAll(response, MAX_PAGE_SIZE);
		if (bin == null)
			return;
		ObjectContext ctx = ObjectContext.adapt(context);
		CalliObject target = (CalliObject) ctx.getResourceTarget().getTargetObject();
		try {
			DetachedRealm realm = target.getRealm();
			int qidx = reqUri.indexOf('?');
			String qs = qidx < 0 ? "" : reqUri.substring(qidx + 1);
			bin.reset();
			override(response, realm.transformErrorPage(bin, target.toString(), qs));
			bin = null;
		} catch (OpenRDFException e) {
			logger.error(e.toString(), e);
		} catch (IOException e) {
			logger.error(e.toString(), e);
		} catch (XProcException e) {
			logger.error(e.toString(), e);
		} catch (SAXException e) {
			logger.error(e.toString(), e);
		} finally {
			if (bin != null) {
				bin.reset();
			}
		}
	}

	private BufferedInputStream bufferAll(HttpResponse res, int size)
			throws IOException {
		HttpEntity entity = res.getEntity();
		long contentLength = entity.getContentLength();
		if (contentLength > size)
			return null;
		InputStream in = entity.getContent();
		final BufferedInputStream bin = new BufferedInputStream(in, size) {
			public void close() throws IOException {
				// don't allow xerces to close stream
			}
		};
		override(res, bin);
		bin.mark(size);
		long skipped = bin.skip(size);
		bin.reset();
		if (skipped < size) {
			bin.mark(size);
			return bin;
		}
		return null; // too big
	}

	private void override(HttpResponse res, final InputStream bin) {
		HttpEntity entity = res.getEntity();
		res.setEntity(new StreamingHttpEntity(entity) {
			@Override
			protected InputStream getDelegateContent() throws IOException {
				return bin;
			}
		});
	}

}
