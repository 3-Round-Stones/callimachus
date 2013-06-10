/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
 * Copyright (c) 2011, 3 Round Stones Inc., Some rights reserved.
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
package org.callimachusproject.server.chain;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;
import org.callimachusproject.util.DomainNameSystemResolver;

/**
 * Add a Server header to the response.
 */
public class ServerNameFilter implements HttpResponseInterceptor {
	private static final String PROTOCOL = "1.1";
	private static final String HOSTNAME = DomainNameSystemResolver.getInstance().getLocalHostName();

	private String name;
	private String via;
	private Integer port;

	public ServerNameFilter(String name) {
		setServerName(name);
	}

	public String getServerName() {
		return name;
	}

	public void setServerName(String name) {
		this.name = name;
		setVia();
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
		setVia();
	}

	@Override
	public void process(HttpResponse response, HttpContext context)
			throws HttpException, IOException {
		if (name != null) {
			if (response.containsHeader("Server")) {
				response.addHeader("Via", getVia());
			} else {
				response.setHeader("Server", name);
			}
		}
	}

	private synchronized String getVia() {
		return via;
	}

	private synchronized void setVia() {
		StringBuilder sb = new StringBuilder();
		sb.append(PROTOCOL).append(" ").append(HOSTNAME);
		if (port != null && port != 80 && port != 443) {
			sb.append(":").append(port); 
		}
		sb.append(" (").append(name).append(")");
		via = sb.toString();
	}

}
