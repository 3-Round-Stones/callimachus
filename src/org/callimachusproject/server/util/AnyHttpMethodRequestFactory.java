package org.callimachusproject.server.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestFactory;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;

public class AnyHttpMethodRequestFactory implements HttpRequestFactory {
	private static final Set<String> NO_ENTITY_METHODS = new HashSet<String>(
			Arrays.asList("GET", "HEAD", "OPTIONS", "DELETE", "TRACE",
					"CONNECT"));

	public HttpRequest newHttpRequest(final RequestLine requestline)
			throws MethodNotSupportedException {
		if (requestline == null) {
			throw new IllegalArgumentException("Request line may not be null");
		}
		String method = requestline.getMethod();
		if (NO_ENTITY_METHODS.contains(method)) {
			return new BasicHttpRequest(requestline);
		} else {
			return new BasicHttpEntityEnclosingRequest(requestline);
		}
	}

	public HttpRequest newHttpRequest(final String method, final String uri)
			throws MethodNotSupportedException {
		if (NO_ENTITY_METHODS.contains(method)) {
			return new BasicHttpRequest(method, uri);
		} else {
			return new BasicHttpEntityEnclosingRequest(method, uri);
		}
	}

}
