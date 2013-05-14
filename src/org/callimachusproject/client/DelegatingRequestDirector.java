/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.callimachusproject.client;

import static org.apache.http.client.params.ClientPNames.COOKIE_POLICY;
import static org.apache.http.client.params.ClientPNames.HANDLE_AUTHENTICATION;
import static org.apache.http.client.params.CookiePolicy.IGNORE_COOKIES;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.auth.AuthState;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.RedirectException;
import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.RequestDirector;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.impl.client.ClientParamsStack;
import org.apache.http.impl.client.EntityEnclosingRequestWrapper;
import org.apache.http.impl.client.HttpAuthenticator;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.impl.conn.ConnectionShutdownException;
import org.apache.http.impl.conn.SchemeRegistryFactory;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.util.EntityUtils;

/**
 * Handles authentication and redirect while delegating to a {@link RequestDirector}.
 * <p>
 * The following parameters can be used to customize the behavior of this
 * class:
 * <ul>
 *  <li>{@link org.apache.http.params.CoreProtocolPNames#PROTOCOL_VERSION}</li>
 *  <li>{@link org.apache.http.params.CoreProtocolPNames#STRICT_TRANSFER_ENCODING}</li>
 *  <li>{@link org.apache.http.params.CoreProtocolPNames#HTTP_ELEMENT_CHARSET}</li>
 *  <li>{@link org.apache.http.params.CoreProtocolPNames#USE_EXPECT_CONTINUE}</li>
 *  <li>{@link org.apache.http.params.CoreProtocolPNames#WAIT_FOR_CONTINUE}</li>
 *  <li>{@link org.apache.http.params.CoreProtocolPNames#USER_AGENT}</li>
 *  <li>{@link org.apache.http.auth.params.AuthPNames#CREDENTIAL_CHARSET}</li>
 *  <li>{@link org.apache.http.client.params.ClientPNames#COOKIE_POLICY}</li>
 *  <li>{@link org.apache.http.client.params.ClientPNames#HANDLE_AUTHENTICATION}</li>
 *  <li>{@link org.apache.http.client.params.ClientPNames#HANDLE_REDIRECTS}</li>
 *  <li>{@link org.apache.http.client.params.ClientPNames#MAX_REDIRECTS}</li>
 *  <li>{@link org.apache.http.client.params.ClientPNames#ALLOW_CIRCULAR_REDIRECTS}</li>
 *  <li>{@link org.apache.http.client.params.ClientPNames#VIRTUAL_HOST}</li>
 *  <li>{@link org.apache.http.client.params.ClientPNames#DEFAULT_HOST}</li>
 *  <li>{@link org.apache.http.client.params.ClientPNames#DEFAULT_HEADERS}</li>
 *  <li>{@link org.apache.http.client.params.ClientPNames#CONN_MANAGER_TIMEOUT}</li>
 * </ul>
 *
 * @since 4.0
 */
@NotThreadSafe // e.g. managedConn
public class DelegatingRequestDirector implements RequestDirector {

    /** The request executor. */
    protected final HttpRequestExecutor requestExec;

    /** The HTTP protocol processor. */
    protected final HttpProcessor httpProcessor;

    /** The redirect strategy. */
    private final RedirectStrategy redirectStrategy;

    /** The target authentication handler. */
    private final AuthenticationStrategy targetAuthStrategy;

    /** The HTTP parameters. */
    private final HttpParams params;

    private final AuthState targetAuthState;

    private final HttpAuthenticator authenticator;

    private final HttpClient delegate;

    private int redirectCount;

    private int maxRedirects;

    public DelegatingRequestDirector(
            final HttpRequestExecutor requestExec,
            final HttpProcessor httpProcessor,
            final RedirectStrategy redirectStrategy,
            final AuthenticationStrategy targetAuthStrategy,
            final HttpClient delegate,
            final HttpParams params) {
        if (requestExec == null) {
            throw new IllegalArgumentException
                ("Request executor may not be null.");
        }
        if (httpProcessor == null) {
            throw new IllegalArgumentException
                ("HTTP protocol processor may not be null.");
        }
        if (redirectStrategy == null) {
            throw new IllegalArgumentException
                ("Redirect strategy may not be null.");
        }
        if (targetAuthStrategy == null) {
            throw new IllegalArgumentException
                ("Target authentication strategy may not be null.");
        }
        if (params == null) {
            throw new IllegalArgumentException
                ("HTTP parameters may not be null");
        }
        this.authenticator     = new HttpAuthenticator();
        this.requestExec        = requestExec;
        this.httpProcessor      = httpProcessor;
        this.redirectStrategy   = redirectStrategy;
        this.targetAuthStrategy = targetAuthStrategy;
        this.delegate			= delegate;
        this.params             = params;

        this.redirectCount = 0;
        this.targetAuthState = new AuthState();
        this.maxRedirects = this.params.getIntParameter(ClientPNames.MAX_REDIRECTS, 100);
    }


	// non-javadoc, see interface ClientRequestDirector
	public HttpResponse execute(HttpHost target, final HttpRequest request,
	                            final HttpContext context)
	    throws HttpException, IOException {
	
	    context.setAttribute(ClientContext.TARGET_AUTH_STATE, targetAuthState);
	
	    RequestWrapper wrapper = wrapRequest(request);
	    wrapper.setParams(params);
		if (target == null) {
			target = (HttpHost) wrapper.getParams().getParameter(
					ClientPNames.DEFAULT_HOST);
			if (target == null) {
				throw new IllegalStateException(
						"Target host must not be null, or set in parameters.");
			}
		}
	
	    boolean done = false;
	    try {
	        HttpResponse response = null;
	        while (!done) {
	            // In this loop, the wrapper and target may be replaced by a
	            // followup request and host. The target passed
	            // in the method arguments will be replaced. The original
	            // request is still available in 'request'.
	
	            response = null;
	
                URI requestURI = wrapper.getURI();
                if (requestURI.isAbsolute()) {
                    target = URIUtils.extractHost(requestURI);
                }
	
	            // Reset headers on the request wrapper
	            wrapper.resetHeaders();
	            // Re-write request URI if needed
	            rewriteRequestURI(wrapper);
	
	            // Populate the execution context
	            context.setAttribute(ExecutionContext.HTTP_TARGET_HOST, target);
                context.setAttribute(ExecutionContext.HTTP_CONNECTION, createConnection(target));
	
	            // Run request protocol interceptors
	            requestExec.preProcess(wrapper, httpProcessor, context);
	
	            response = tryExecute(target, wrapper, context);
	            if (response == null) {
	                // Need to start over
	                continue;
	            }
	
	            // Run response protocol interceptors
	            response.setParams(params);
	            requestExec.postProcess(response, httpProcessor, context);
	
	            RequestWrapper followup = handleResponse(wrapper, response, context);
	            if (followup == null) {
	                done = true;
	            } else {
	                // Make sure the response body is fully consumed, if present
	                HttpEntity entity = response.getEntity();
	                EntityUtils.consume(entity);
	                wrapper = followup;
	            }
	
	        } // while not done
	
	        return response;
	
	    } catch (ConnectionShutdownException ex) {
	        InterruptedIOException ioex = new InterruptedIOException(
	                "Connection has been shut down");
	        ioex.initCause(ex);
	        throw ioex;
	    }
	} // execute


	private Object createConnection(HttpHost host) {
		return new VirtualConnection(host);
	}


	private RequestWrapper wrapRequest(final HttpRequest request)
			throws ProtocolException {
		if (request instanceof HttpEntityEnclosingRequest) {
			return new EntityEnclosingRequestWrapper(
					(HttpEntityEnclosingRequest) request);
		} else {
			return new RequestWrapper(request);
		}
	}

	private void rewriteRequestURI(final RequestWrapper request)
			throws ProtocolException {
		try {

			URI uri = request.getURI();
			// Make sure the request URI is relative
			if (uri.isAbsolute()) {
				request.setHeader("Host", uri.getAuthority());
				uri = URIUtils.rewriteURI(uri, null, true);
			} else {
				uri = URIUtils.rewriteURI(uri);
			}
			request.setURI(uri);

		} catch (URISyntaxException ex) {
			throw new ProtocolException("Invalid URI: "
					+ request.getRequestLine().getUri(), ex);
		}
	}


    private HttpResponse tryExecute(HttpHost target, RequestWrapper wrapper,
			final HttpContext context) throws IOException,
			ClientProtocolException {
		try {
			// these are all this that are handled upstream in HttpOriginClient
			HttpParams d = new BasicHttpParams();
			d.setParameter(COOKIE_POLICY, IGNORE_COOKIES);
			d.setBooleanParameter(HANDLE_AUTHENTICATION, false);
			d.setBooleanParameter(ClientPNames.HANDLE_REDIRECTS, false);
	        HttpParams p = new ClientParamsStack(null, wrapper.getParams(), d, null);
	        wrapper.setParams(p);
			return delegate.execute(target, wrapper, context);
		} finally {
			wrapper.setParams(params);
		}
	}


	/**
     * Analyzes a response to check need for a followup.
     *
     * @param roureq    the request and route.
     * @param response  the response to analayze
     * @param context   the context used for the current request execution
     *
     * @return  the followup request and route if there is a followup, or
     *          <code>null</code> if the response should be returned as is
     *
     * @throws HttpException    in case of a problem
     * @throws IOException      in case of an IO problem
     */
    private RequestWrapper handleResponse(RequestWrapper request,
                                           HttpResponse response,
                                           HttpContext context)
        throws HttpException, IOException {

        HttpParams params = request.getParams();

        if (HttpClientParams.isAuthenticating(params)) {
            HttpHost target = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
            assert target != null;
            if (target.getPort() < 0) {
                int port = getPort(target);
				target = new HttpHost(target.getHostName(), port, target.getSchemeName());
            }
            if (this.authenticator.isAuthenticationRequested(target, response,
                    this.targetAuthStrategy, this.targetAuthState, context)) {
                if (this.authenticator.authenticate(target, response,
                        this.targetAuthStrategy, this.targetAuthState, context)) {
                    // Re-try the same request with auth
                    return request;
                }
            }
        }

        if (HttpClientParams.isRedirecting(params) &&
                this.redirectStrategy.isRedirected(request, response, context)) {

            if (redirectCount >= maxRedirects) {
                throw new RedirectException("Maximum redirects ("
                        + maxRedirects + ") exceeded");
            }
            redirectCount++;

            HttpUriRequest redirect = redirectStrategy.getRedirect(request, response, context);
            HttpRequest orig = request.getOriginal();
            redirect.setHeaders(orig.getAllHeaders());

            URI uri = redirect.getURI();
            HttpHost newTarget = URIUtils.extractHost(uri);
            if (newTarget == null) {
                throw new ProtocolException("Redirect URI does not specify a valid host name: " + uri);
            }

            RequestWrapper wrapper = wrapRequest(redirect);
            wrapper.setParams(params);

            return wrapper;
        }

        return null;
    } // handleResponse


	int getPort(HttpHost target) {
		int port = target.getPort();
		if (port < 0) {
			Scheme scheme = SchemeRegistryFactory.createSystemDefault().getScheme(target);
			port = scheme.getDefaultPort();
		}
		return port;
	}


} // class DefaultClientRequestDirector
