/*
 * Copyright 2010, Zepheira LLC Some rights reserved.
 * Copyright (c) 2011 Talis Inc., Some rights reserved.
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
package org.callimachusproject.server.handlers;

import java.util.Enumeration;

import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.model.ResourceOperation;
import org.callimachusproject.server.model.Response;
import org.callimachusproject.server.util.HTTPDateFormat;

/**
 * Response with 304 and 412 when resource has not been modified.
 * 
 * @author James Leigh
 * 
 */
public class ModifiedSinceHandler implements Handler {
	private final Handler delegate;
	private long reset = System.currentTimeMillis() / 1000 * 1000;
	private HTTPDateFormat format = new HTTPDateFormat();

	public ModifiedSinceHandler(Handler delegate) {
		this.delegate = delegate;
	}

	public void invalidate() {
		reset = System.currentTimeMillis() / 1000 * 1000;
	}

	public Response verify(ResourceOperation req) throws Exception {
		String method = req.getMethod();
		String contentType = req.getResponseContentType();
		String cache = req.getResponseCacheControl();
		String entityTag = req.getEntityTag(req.revision(), cache, contentType);
		if (req.isSafe() && req.isNoValidate()) {
			return delegate.verify(req);
		} else {
			Response resp;
			String tag = modifiedSince(req, entityTag);
			if ("GET".equals(method) || "HEAD".equals(method)) {
				if (tag == null) {
					return delegate.verify(req);
				}
				resp = new Response().notModified();
			} else if (tag == null) {
				return delegate.verify(req);
			} else {
				resp = new Response().preconditionFailed();
			}
			if (tag.length() == 0)
				return resetModified(resp);
			return resetModified(resp.header("ETag", tag));
		}
	}

	public Response handle(ResourceOperation req) throws Exception {
		return resetModified(delegate.handle(req));
	}

	private Response resetModified(Response resp) {
		if (reset > 0 && reset > resp.lastModified()) {
			resp.lastModified(reset, format.format(reset));
		}
		return resp;
	}

	private String modifiedSince(ResourceOperation req, String entityTag) {
		long lastModified = Math.max(reset, req.getLastModified());
		boolean notModified = false;
		try {
			if (lastModified > 0) {
				long modified = req.getDateHeader("If-Modified-Since");
				notModified = modified > 0;
				if (notModified && modified < lastModified)
					return null;
			}
		} catch (IllegalArgumentException e) {
			// invalid date header
		}
		Enumeration matchs = req.getHeaderEnumeration("If-None-Match");
		boolean mustMatch = matchs.hasMoreElements();
		if (mustMatch) {
			String match = null;
			while (matchs.hasMoreElements()) {
				match = (String) matchs.nextElement();
				if (match(entityTag, match))
					return match;
			}
		}
		if (!notModified || mustMatch)
			return null;
		return "";
	}

	private boolean match(String tag, String match) {
		if (tag == null)
			return false;
		if ("*".equals(match))
			return true;
		if (match.startsWith("W/") && !tag.startsWith("W/")) {
			match = match.substring(2);
		}
		if (match.equals(tag))
			return true;
		int md = match.indexOf('-');
		int td = tag.indexOf('-');
		if (td >= 0 && md >= 0)
			return false;
		if (md < 0) {
			md = match.lastIndexOf('"');
		}
		if (td < 0) {
			td = tag.lastIndexOf('"');
		}
		int mq = match.indexOf('"');
		int tq = tag.indexOf('"');
		if (mq < 0 || tq < 0 || md < 0 || td < 0)
			return false;
		return match.substring(mq, md).equals(tag.substring(tq, td));
	}

}
