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

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthOption;
import org.apache.http.auth.AuthProtocolState;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.ContextAwareAuthScheme;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.AuthenticationStrategy;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.util.Asserts;

public class HttpAuthenticator {

    private final Log log = LogFactory.getLog(getClass());

	private final AuthenticationStrategy targetAuthStrategy = TargetAuthenticationStrategy.INSTANCE;
	private final AuthenticationStrategy proxyAuthStrategy = ProxyAuthenticationStrategy.INSTANCE;

    public void generateAuthResponse(final HttpRoute route,
            final HttpRequest request, final HttpContext context)
            throws HttpException, IOException {
        if (!request.containsHeader(AUTH.WWW_AUTH_RESP)) {
            final AuthState targetAuthState = this.getTargetAuthState(context);
            if (this.log.isDebugEnabled()) {
                this.log.debug("Target auth state: " + targetAuthState.getState());
            }
            this.generateAuthResponse(request, targetAuthState, context);
        }
        if (!request.containsHeader(AUTH.PROXY_AUTH_RESP) && !route.isTunnelled()) {
            final AuthState proxyAuthState = this.getProxyAuthState(context);
            if (this.log.isDebugEnabled()) {
                this.log.debug("Proxy auth state: " + proxyAuthState.getState());
            }
            this.generateAuthResponse(request, proxyAuthState, context);
        }
    }

    public boolean needAuthentication(
            final HttpRoute route,
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context) throws HttpException, IOException {
        final AuthState targetAuthState = getTargetAuthState(context);
        final AuthState proxyAuthState = getProxyAuthState(context);
        HttpHost target = (HttpHost) context.getAttribute(HttpCoreContext.HTTP_TARGET_HOST);
        if (target == null) {
            target = route.getTargetHost();
        }
        HttpHost proxy = route.getProxyHost();
        if (this.needAuthentication(target, proxy, targetAuthState,
                proxyAuthState, response, context)) {
            // discard previous auth headers
            request.removeHeaders(AUTH.WWW_AUTH_RESP);
            request.removeHeaders(AUTH.PROXY_AUTH_RESP);
            return true;
        } else {
            return false;
        }
    }

    private boolean isAuthenticationRequested(
            final HttpHost host,
            final HttpResponse response,
            final AuthenticationStrategy authStrategy,
            final AuthState authState,
            final HttpContext context) {
        if (authStrategy.isAuthenticationRequested(host, response, context)) {
            this.log.debug("Authentication required");
            if (authState.getState() == AuthProtocolState.SUCCESS) {
                authStrategy.authFailed(host, authState.getAuthScheme(), context);
            }
            return true;
        } else {
            switch (authState.getState()) {
            case CHALLENGED:
            case HANDSHAKE:
                this.log.debug("Authentication succeeded");
                authState.setState(AuthProtocolState.SUCCESS);
                authStrategy.authSucceeded(host, authState.getAuthScheme(), context);
                break;
            case SUCCESS:
                break;
            default:
                authState.setState(AuthProtocolState.UNCHALLENGED);
            }
            return false;
        }
    }

    private boolean handleAuthChallenge(
            final HttpHost host,
            final HttpResponse response,
            final AuthenticationStrategy authStrategy,
            final AuthState authState,
            final HttpContext context) {
        try {
            if (this.log.isDebugEnabled()) {
                this.log.debug(host.toHostString() + " requested authentication");
            }
            final Map<String, Header> challenges = authStrategy.getChallenges(host, response, context);
            if (challenges.isEmpty()) {
                this.log.debug("Response contains no authentication challenges");
                return false;
            }

            final AuthScheme authScheme = authState.getAuthScheme();
            switch (authState.getState()) {
            case FAILURE:
                return false;
            case SUCCESS:
                authState.reset();
                break;
            case CHALLENGED:
            case HANDSHAKE:
                if (authScheme == null) {
                    this.log.debug("Auth scheme is null");
                    authStrategy.authFailed(host, null, context);
                    authState.reset();
                    authState.setState(AuthProtocolState.FAILURE);
                    return false;
                }
            case UNCHALLENGED:
                if (authScheme != null) {
                    final String id = authScheme.getSchemeName();
                    final Header challenge = challenges.get(id.toLowerCase(Locale.US));
                    if (challenge != null) {
                        this.log.debug("Authorization challenge processed");
                        authScheme.processChallenge(challenge);
                        if (authScheme.isComplete()) {
                            this.log.debug("Authentication failed");
                            authStrategy.authFailed(host, authState.getAuthScheme(), context);
                            authState.reset();
                            authState.setState(AuthProtocolState.FAILURE);
                            return false;
                        } else {
                            authState.setState(AuthProtocolState.HANDSHAKE);
                            return true;
                        }
                    } else {
                        authState.reset();
                        // Retry authentication with a different scheme
                    }
                }
            }
            final Queue<AuthOption> authOptions = authStrategy.select(challenges, host, response, context);
            if (authOptions != null && !authOptions.isEmpty()) {
                if (this.log.isDebugEnabled()) {
                    this.log.debug("Selected authentication options: " + authOptions);
                }
                authState.setState(AuthProtocolState.CHALLENGED);
                authState.update(authOptions);
                return true;
            } else {
                return false;
            }
        } catch (final MalformedChallengeException ex) {
            if (this.log.isWarnEnabled()) {
                this.log.warn("Malformed challenge: " +  ex.getMessage());
            }
            authState.reset();
            return false;
        }
    }

    private void generateAuthResponse(
            final HttpRequest request,
            final AuthState authState,
            final HttpContext context) throws HttpException, IOException {
        AuthScheme authScheme = authState.getAuthScheme();
        Credentials creds = authState.getCredentials();
        switch (authState.getState()) {
        case FAILURE:
            return;
        case SUCCESS:
            ensureAuthScheme(authScheme);
            if (authScheme.isConnectionBased()) {
                return;
            }
            break;
        case CHALLENGED:
            final Queue<AuthOption> authOptions = authState.getAuthOptions();
            if (authOptions != null) {
                while (!authOptions.isEmpty()) {
                    final AuthOption authOption = authOptions.remove();
                    authScheme = authOption.getAuthScheme();
                    creds = authOption.getCredentials();
                    authState.update(authScheme, creds);
                    if (this.log.isDebugEnabled()) {
                        this.log.debug("Generating response to an authentication challenge using "
                                + authScheme.getSchemeName() + " scheme");
                    }
                    try {
                        final Header header = doAuth(authScheme, creds, request, context);
                        request.addHeader(header);
                        break;
                    } catch (final AuthenticationException ex) {
                        if (this.log.isWarnEnabled()) {
                            this.log.warn(authScheme + " authentication error: " + ex.getMessage());
                        }
                    }
                }
                return;
            } else {
                ensureAuthScheme(authScheme);
            }
        }
        if (authScheme != null) {
            try {
                final Header header = doAuth(authScheme, creds, request, context);
                request.addHeader(header);
            } catch (final AuthenticationException ex) {
                if (this.log.isErrorEnabled()) {
                    this.log.error(authScheme + " authentication error: " + ex.getMessage());
                }
            }
        }
    }

    private void ensureAuthScheme(final AuthScheme authScheme) {
        Asserts.notNull(authScheme, "Auth scheme");
    }

    @SuppressWarnings("deprecation")
    private Header doAuth(
            final AuthScheme authScheme,
            final Credentials creds,
            final HttpRequest request,
            final HttpContext context) throws AuthenticationException {
        if (authScheme instanceof ContextAwareAuthScheme) {
            return ((ContextAwareAuthScheme) authScheme).authenticate(creds, request, context);
        } else {
            return authScheme.authenticate(creds, request);
        }
    }

    private boolean needAuthentication(final HttpHost target, HttpHost proxy,
            final AuthState targetAuthState, final AuthState proxyAuthState,
            final HttpResponse response, final HttpContext context) {
        if (this.targetAuthStrategy != null
                && this.isAuthenticationRequested(target, response,
                        this.targetAuthStrategy, targetAuthState, context)) {
            return this.handleAuthChallenge(target, response,
                    this.targetAuthStrategy, targetAuthState, context);
        }
        if (this.isAuthenticationRequested(proxy, response,
                this.proxyAuthStrategy, proxyAuthState, context)) {
            // if proxy is not set use target host instead
            if (proxy == null) {
                proxy = target;
            }
            return this.handleAuthChallenge(proxy, response,
                    this.proxyAuthStrategy, proxyAuthState, context);
        }
        return false;
    }

    private AuthState getTargetAuthState(final HttpContext context) {
        AuthState targetAuthState = (AuthState) context.getAttribute(HttpClientContext.TARGET_AUTH_STATE);
        if (targetAuthState == null) {
            targetAuthState = new AuthState();
            context.setAttribute(HttpClientContext.TARGET_AUTH_STATE, targetAuthState);
        }
        return targetAuthState;
    }

    private AuthState getProxyAuthState(final HttpContext context) {
        AuthState proxyAuthState = (AuthState) context.getAttribute(HttpClientContext.PROXY_AUTH_STATE);
        if (proxyAuthState == null) {
            proxyAuthState = new AuthState();
            context.setAttribute(HttpClientContext.PROXY_AUTH_STATE, proxyAuthState);
        }
        return proxyAuthState;
    }

}
