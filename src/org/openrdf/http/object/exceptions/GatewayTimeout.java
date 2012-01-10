/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.openrdf.http.object.exceptions;


/**
 * The server, while acting as a gateway or proxy, did not receive a timely
 * response from the upstream server specified by the URI (e.g. HTTP, FTP, LDAP)
 * or some other auxiliary server (e.g. DNS) it needed to access in attempting
 * to complete the request.
 */
public class GatewayTimeout extends ResponseException {
	private static final long serialVersionUID = -7878209025109522123L;

	public GatewayTimeout() {
		super("Gateway Timeout");
	}

	public GatewayTimeout(String message, Throwable cause) {
		super(message, cause);
	}

	public GatewayTimeout(String message) {
		super(message);
	}

	public GatewayTimeout(Throwable cause) {
		super(cause);
	}

	public GatewayTimeout(String message, String stack) {
		super(message, stack);
	}

	@Override
	public int getStatusCode() {
		return 504;
	}

	@Override
	public boolean isCommon() {
		return false;
	}

}
