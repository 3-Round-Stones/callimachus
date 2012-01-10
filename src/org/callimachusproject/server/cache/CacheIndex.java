/*
 * Copyright (c) 2009-2010, James Leigh and Zepheira LLC Some rights reserved.
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
package org.callimachusproject.server.cache;

import info.aduna.net.ParsedURI;

import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.callimachusproject.server.util.LockCleanupManager;

/**
 * Manages multiple cache instances by URL.
 */
public class CacheIndex extends
		LinkedHashMap<String, Reference<CachedRequest>> {
	private static final long serialVersionUID = -833236420826697261L;
	private final LockCleanupManager locker;
	private File dir;
	private int maxCapacity;
	private boolean aggressive;

	public CacheIndex(File dir, int maxCapacity, LockCleanupManager locker) {
		super(maxCapacity, 0.75f, true);
		this.dir = dir;
		this.maxCapacity = maxCapacity;
		this.locker = locker;
	}

	public LockCleanupManager getLockManager() {
		return locker;
	}

	public int getMaxCapacity() {
		return maxCapacity;
	}

	public void setMaxCapacity(int maxCapacity) {
		this.maxCapacity = maxCapacity;
	}

	public boolean isAggressive() {
		return aggressive;
	}

	public void setAggressive(boolean aggressive) {
		this.aggressive = aggressive;
	}

	public void invalidate(String... locations) throws IOException,
			InterruptedException {
		List<String> urls = new ArrayList(locations.length);
		if (locations.length == 0) {
			File[] files = dir.listFiles();
			if (files == null)
				return;
			for (File file : files) {
				String url = CachedRequest.getURL(file);
				if (url == null)
					continue;
				urls.add(url);
			}
		} else {
			for (String location : locations) {
				if (location == null)
					continue;
				ParsedURI parsed = new ParsedURI(location);
				File base = new File(dir, safe(parsed.getAuthority()));
				File path = new File(base, safe(parsed.getPath()));
				StringBuilder sb = new StringBuilder(64);
				sb.append(parsed.getScheme());
				sb.append("://");
				sb.append(parsed.getAuthority());
				sb.append(parsed.getPath());
				String code = Integer.toHexString(sb.toString().hashCode());
				File dir = new File(path, '$' + code);
				File[] files = dir.listFiles();
				if (files == null)
					continue;
				for (File file : files) {
					String url = CachedRequest.getURL(file);
					if (url == null)
						continue;
					urls.add(url);
				}
			}
		}
		for (String url : urls) {
			findCachedRequest(url).stale();
		}
	}

	public synchronized CachedRequest findCachedRequest(String url)
			throws IOException {
		CachedRequest index;
		Reference<CachedRequest> ref = get(url);
		if (ref == null) {
			index = new CachedRequest(getFile(url), locker);
			put(url, new SoftReference<CachedRequest>(index));
		} else {
			index = ref.get();
			if (index == null) {
				index = new CachedRequest(getFile(url), locker);
				put(url, new SoftReference<CachedRequest>(index));
			}
		}
		return index;
	}

	@Override
	public void clear() {
		Collection<Entry<String, Reference<CachedRequest>>> entrySet;
		synchronized (this) {
			entrySet = new ArrayList(entrySet());
		}
		for (Map.Entry<String, Reference<CachedRequest>> e : entrySet) {
			remove(e);
		}
	}

	@Override
	protected boolean removeEldestEntry(
			Map.Entry<String, Reference<CachedRequest>> eldest) {
		if (aggressive || size() <= maxCapacity)
			return false;
		CachedRequest index = eldest.getValue().get();
		if (index != null && index.inUse())
			return false;
		return remove(eldest);
	}

	private boolean remove(Map.Entry<String, Reference<CachedRequest>> entry) {
		CachedRequest index;
		synchronized (this) {
			index = entry.getValue().get();
			if (index == null) {
				File file = getFile(entry.getKey());
				CachedRequest.delete(file);
				deldirs(file.getParentFile());
				super.remove(entry.getKey());
				return true;
			}
		}
		synchronized (index) {
			index.delete();
			deldirs(index.getDirectory().getParentFile());
			synchronized (this) {
				super.remove(entry.getKey());
			}
		}
		return true;
	}

	private void deldirs(File file) {
		if (file != null && file.delete()) {
			deldirs(file.getParentFile());
		}
	}

	private File getFile(String url) {
		ParsedURI parsed = new ParsedURI(url);
		File base = new File(dir, safe(parsed.getAuthority()));
		File dir = new File(base, safe(parsed.getPath()));
		String uri;
		int idx = url.lastIndexOf('?');
		if (idx > 0) {
			uri = url.substring(0, idx);
		} else {
			uri = url;
		}
		String identity = '$' + Integer.toHexString(uri.hashCode());
		String name = Integer.toHexString(url.hashCode());
		return new File(new File(dir, identity), '$' + name);
	}

	private String safe(String path) {
		if (path == null)
			return "";
		path = path.replace('/', File.separatorChar);
		path = path.replace('\\', File.separatorChar);
		path = path.replace(':', File.separatorChar);
		path = path.replaceAll("[^a-zA-Z0-9/\\\\]", "_");
		return path.toLowerCase();
	}

}
