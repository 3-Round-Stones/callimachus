package org.callimachusproject.server.client;

import java.io.IOException;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.util.EntityUtils;

public class HTTPServiceUnavailable implements HTTPService {

	private static final BasicHttpResponse _503 = new BasicHttpResponse(HttpVersion.HTTP_1_1, 503, "Service Unavailable");

	@Override
	public HttpResponse service(HttpRequest request) throws IOException {
		if (request instanceof HttpEntityEnclosingRequest) {
			EntityUtils.consume(((HttpEntityEnclosingRequest) request).getEntity());
		}
		return _503;
	}

}
