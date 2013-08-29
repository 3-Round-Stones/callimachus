package org.callimachusproject.client;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.impl.execchain.RequestAbortedException;
import org.apache.http.util.EntityUtils;

public class AuthenticationClientExecChain implements ClientExecChain {
	private final HttpAuthenticator authenticator;
	private final ClientExecChain delegate;

	public AuthenticationClientExecChain(ClientExecChain delegate) {
		this.authenticator = new HttpAuthenticator();
		this.delegate = delegate;
	}

	public CloseableHttpResponse execute(final HttpRoute route,
			final HttpRequestWrapper original, final HttpClientContext context,
			final HttpExecutionAware execAware) throws IOException,
			HttpException {
		final HttpRequestWrapper request = HttpRequestWrapper.wrap(original);
		final RequestConfig config = context.getRequestConfig();

		CloseableHttpResponse response = null;
		while (true) {
			if (execAware != null && execAware.isAborted()) {
				throw new RequestAbortedException("Request aborted");
			}

			this.authenticator.generateAuthResponse(route, request, context);

			response = delegate.execute(route, request, context, execAware);

			if (config.isAuthenticationEnabled()
					&& authenticator.needAuthentication(route, request,
							response, context)) {
				EntityUtils.consume(response.getEntity());
			} else {
				return response;
			}
		}
	}

}
