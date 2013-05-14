package org.callimachusproject.client;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.RequestDirector;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.protocol.HttpContext;

public class UnavailableRequestDirector implements RequestDirector {
	private static final BasicHttpResponse _503 = new BasicHttpResponse(HttpVersion.HTTP_1_1, 503, "Service Disconnected");

	@Override
	public HttpResponse execute(HttpHost host, HttpRequest request,
			HttpContext ctx) throws IOException, HttpException {
		return _503;
	}

}
