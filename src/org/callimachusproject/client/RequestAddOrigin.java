package org.callimachusproject.client;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.protocol.HttpContext;

public class RequestAddOrigin implements HttpRequestInterceptor {
	private final String origin;

	public RequestAddOrigin(String origin) {
		this.origin = origin;
	}

	@Override
	public void process(HttpRequest request, HttpContext context)
			throws HttpException, IOException {
		request.addHeader("Origin", origin);
	}

}
