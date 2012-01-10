/*
 * Copyright 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.openrdf.http.object.cache;

import info.aduna.concurrent.locks.Lock;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.http.ProtocolVersion;
import org.apache.http.RequestLine;
import org.apache.http.message.BasicRequestLine;
import org.openrdf.http.object.model.Request;

/**
 * Clones a request that will have its response cached for later use.
 */
public class CachableRequest extends Request {
	private static final Collection<String> hidden = Arrays.asList(
			"If-None-Match", "If-Modified-Since", "If-Match",
			"If-Unmodified-Since", "If-Range", "Range", "Cache-Control");
	private Request originalRequest;
	private List<Lock> locks = new ArrayList<Lock>();

	public CachableRequest(Request request, CachedEntity stale,
			List<CachedEntity> match, Lock cacheLock) throws IOException, InterruptedException {
		super(request.clone());
		this.originalRequest = request;
		locks.add(cacheLock);
		setReceivedOn(request.getReceivedOn());
		RequestLine rl = getRequestLine();
		if ("HEAD".equals(rl.getMethod())) {
			ProtocolVersion ver = rl.getProtocolVersion();
			setRequestLine(new BasicRequestLine("GET", rl.getUri(), ver));
		}
		for (String name : hidden) {
			removeHeaders(name);
		}
		if (match != null && match.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (CachedEntity entity : match) {
				Lock against = entity.matching();
				if (against != null) {
					locks.add(against);
					sb.append(entity.getETag()).append(",");
				}
			}
			if (sb.length() > 0) {
				setHeader("If-None-Match", sb.substring(0, sb.length() - 1));
			}
		}
		if (stale != null && stale.getLastModified() != null) {
			setHeader("If-Modified-Since", stale.getLastModified());
		}
	}

	public Request getOriginalRequest() {
		return originalRequest;
	}

	@Override
	public void close() {
		releaseCachedEntities();
		super.close();
	}

	public void releaseCachedEntities() {
		for (Lock lock : locks) {
			lock.release();
		}
	}

}
