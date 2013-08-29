package org.callimachusproject.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.message.BasicHttpResponse;

public class UnavailableRequestDirector implements ClientExecChain {

	@Override
	public CloseableHttpResponse execute(HttpRoute route,
			HttpRequestWrapper request, HttpClientContext clientContext,
			HttpExecutionAware execAware) throws IOException, HttpException {
		BasicHttpResponse _503 = new BasicHttpResponse(HttpVersion.HTTP_1_1, 503, "Service Disconnected");
		HttpHost target = route.getTargetHost();
		try {
			URI root = new URI(target.getSchemeName(), null, target.getHostName(), target.getPort(), "/", null, null);
			return new HttpUriResponse(root.resolve(request.getURI()).toASCIIString(), _503);
		} catch (URISyntaxException e) {
			return new HttpUriResponse(request.getURI().toASCIIString(), _503);
		}
	}

}
