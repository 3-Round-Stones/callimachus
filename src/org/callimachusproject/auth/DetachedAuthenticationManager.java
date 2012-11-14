/*
 * Copyright (c) 2009-2010, James Leigh, Some rights reserved.
 * Copyright (c) 2012, 3 Round Stones Inc., Some rights reserved.
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
package org.callimachusproject.auth;

import java.util.Collection;
import java.util.Map;

import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;

/**
 * Interface to authenticate user agents.
 * 
 * @author James Leigh
 * 
 */
public interface DetachedAuthenticationManager {

	String getIdentifier();

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
			Map<String, String[]> request, ObjectConnection con);

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
			Map<String, String[]> request);

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
	 * @throws OpenRDFException
	 */
	HttpMessage authenticationInfo(String method, Object resource,
			Map<String, String[]> request, ObjectConnection con)
			throws OpenRDFException;

	/**
	 * Include any Set-Cookie header to clear the session
	 * 
	 * @return set of HTTP headers in a 204 response
	 */
	HttpResponse logout(Collection<String> tokens);
}
