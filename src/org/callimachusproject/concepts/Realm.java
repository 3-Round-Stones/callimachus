/*
 * Copyright (c) 2009, James Leigh All rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
 * Copyright (c) 2012 3 Round Stones Inc., Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.concepts;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.openrdf.annotations.Iri;
import org.openrdf.repository.RepositoryException;

/**
 * A common set of services all realms must implement.
 */
@Iri("http://callimachusproject.org/rdf/2009/framework#Realm")
public interface Realm {

	/**
	 * The script's origins that are permitted to send requests to this realm as
	 * defined in the HTTP header Access-Control-Allow-Origin.
	 * 
	 * @return a set of acceptable scheme + '://' + authority or '*' if any
	 *         script is allowed without agent credentials or an empty set if no
	 *         scripts are allowed.
	 */
	Collection<String> allowOrigin();

	/**
	 * If scripts from the given origin can use their agent's credentials.
	 * 
	 * @param origin
	 *            the scheme and authority the agent script was loaded from
	 * 
	 * @return <code>true</code> if credentials from the agent are permitted in
	 *         a request.
	 */
	boolean withAgentCredentials(String origin);

	/**
	 * Authenticates a request to determine the authenticated credential.
	 * <DL>
	 * <DT>request-target
	 * <dd>from the request line
	 * <DT>request-scheme
	 * <dd>"http" or "https"
	 * <DT>authorization
	 * <dd>HTTP request header of the same name, if present
	 * <DT>cookie
	 * <dd>HTTP request header of the same name, if present
	 * <DT>host
	 * <dd>HTTP request header of the same name, if present
	 * <DT>origin
	 * <dd>scheme and authority the agent script was loaded from (if applicable)
	 * <DT>via
	 * <dd>list of hosts or pseudonym and their HTTP version that sent or
	 * forwarded this request
	 * </DL>
	 * 
	 * @param method
	 *            The HTTP request method.
	 * @param resource
	 *            The target resource of a request.
	 * @param request
	 *            A map with the above conditional keys
	 * @return The authenticated credentials or a null result if invalid
	 *         credentials.
	 */
	String authenticateRequest(String method, Object resource,
			Map<String, String[]> request) throws RepositoryException;

	/**
	 * The response that should be returned when the request could not be
	 * authenticated.
	 * <DL>
	 * <DT>request-target
	 * <dd>from the request line
	 * <DT>request-scheme
	 * <dd>"http" or "https"
	 * <DT>authorization
	 * <dd>HTTP request header of the same name, if present
	 * <DT>cookie
	 * <dd>HTTP request header of the same name, if present
	 * <DT>host
	 * <dd>HTTP request header of the same name, if present
	 * <DT>origin
	 * <dd>scheme and authority the agent script was loaded from (if applicable)
	 * <DT>via
	 * <dd>list of hosts or pseudonym and their HTTP version that sent or
	 * forwarded this request
	 * </DL>
	 * 
	 * @param method
	 *            The HTTP request method.
	 * @param resource
	 *            The target resource of a request.
	 * @param request
	 *            A map with the above conditional keys
	 * 
	 * @return An HTTP response
	 */
	HttpResponse unauthorized(String method, Object resource,
			Map<String, String[]> request) throws Exception;

	/**
	 * Called after a request has been authenticate.
	 * <DL>
	 * <DT>request-target
	 * <dd>from the request line
	 * <DT>request-scheme
	 * <dd>"http" or "https"
	 * <DT>authorization
	 * <dd>HTTP request header of the same name, if present
	 * <DT>cookie
	 * <dd>HTTP request header of the same name, if present
	 * <DT>host
	 * <dd>HTTP request header of the same name, if present
	 * <DT>origin
	 * <dd>scheme and authority the agent script was loaded from (if applicable)
	 * <DT>via
	 * <dd>list of hosts or pseudonym and their HTTP version that sent or
	 * forwarded this request
	 * </DL>
	 * 
	 * @param credential
	 *            Response from authenticateRequest.
	 * @param method
	 *            The HTTP request method.
	 * @param resource
	 *            The target resource of a request.
	 * @param request
	 *            A map with the above conditional keys
	 * @return <code>true</code> if the credentials are authorized on this
	 *         resource
	 * @throws RepositoryException
	 */
	boolean authorizeCredential(String credential, String method,
			Object resource, Map<String, String[]> request)
			throws RepositoryException;

	/**
	 * The response that should be returned when the request is authenticated,
	 * but could not be authorised or the request originated from an invalid
	 * origin.
	 * <DL>
	 * <DT>request-target
	 * <dd>from the request line
	 * <DT>request-scheme
	 * <dd>"http" or "https"
	 * <DT>authorization
	 * <dd>HTTP request header of the same name, if present
	 * <DT>cookie
	 * <dd>HTTP request header of the same name, if present
	 * <DT>host
	 * <dd>HTTP request header of the same name, if present
	 * <DT>origin
	 * <dd>scheme and authority the agent script was loaded from (if applicable)
	 * <DT>via
	 * <dd>list of hosts or pseudonym and their HTTP version that sent or
	 * forwarded this request
	 * </DL>
	 * 
	 * @param method
	 *            The HTTP request method.
	 * @param resource
	 *            The target resource of a request.
	 * @param request
	 *            A map with the above conditional keys
	 * 
	 * @return An HTTP response
	 */
	HttpResponse forbidden(String method, Object resource,
			Map<String, String[]> request) throws Exception;

	/**
	 * Response headers that should be included in the response.
	 * <DL>
	 * <DT>request-target
	 * <dd>from the request line
	 * <DT>request-scheme
	 * <dd>"http" or "https"
	 * <DT>authorization
	 * <dd>HTTP request header of the same name, if present
	 * <DT>cookie
	 * <dd>HTTP request header of the same name, if present
	 * <DT>host
	 * <dd>HTTP request header of the same name, if present
	 * <DT>origin
	 * <dd>scheme and authority the agent script was loaded from (if applicable)
	 * <DT>via
	 * <dd>list of hosts or pseudonym and their HTTP version that sent or
	 * forwarded this request
	 * </DL>
	 * 
	 * @param method
	 *            The HTTP request method.
	 * @param resource
	 *            The target resource of a request.
	 * @param request
	 *            A map with the above conditional keys
	 * 
	 * @return Set of HTTP headers
	 */
	HttpMessage authenticationInfo(String method, Object resource,
			Map<String, String[]> request);

	/**
	 * The realm used for authentication
	 */
	@Iri("http://callimachusproject.org/rdf/2009/framework#authentication")
	Set<AuthenticationManager> getCalliAuthentications();

	@Iri("http://callimachusproject.org/rdf/2009/framework#authentication")
	void setCalliAuthentications(
			Set<? extends AuthenticationManager> authentications);

	/**
	 * The RDFa template used when an agent is forbidden from the requested
	 * resource.
	 */
	@Iri("http://callimachusproject.org/rdf/2009/framework#forbidden")
	Page getCalliForbidden();

	@Iri("http://callimachusproject.org/rdf/2009/framework#forbidden")
	void setCalliForbidden(Page forbidden);

	/**
	 * The RDFa template used when an agent is aunothorized from the requested
	 * resource.
	 */
	@Iri("http://callimachusproject.org/rdf/2009/framework#unauthorized")
	Page getCalliUnauthorized();

	@Iri("http://callimachusproject.org/rdf/2009/framework#unauthorized")
	void setCalliUnauthorized(Page unauthorized);
}
