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
package org.callimachusproject.server.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * Gives new threads a common prefix.
 * 
 * @author James Leigh
 *
 */
public class NamedThreadFactory implements ThreadFactory {
	private static volatile int COUNT = 0;
	private String name;
	private boolean daemon;
	private final List<Thread> threads = new ArrayList<Thread>();

	public NamedThreadFactory(String name, boolean daemon) {
		this.name = name;
		this.daemon = daemon;
	}

	public Thread[] getLiveThreads() {
		synchronized (threads) {
			removeTerminatedThreads();
			return threads.toArray(new Thread[threads.size()]);
		}
	}

	public Thread newThread(final Runnable r) {
		Thread thread = new Thread(r, name + "-" + (++COUNT));
		if (thread.isDaemon() != daemon) {
			thread.setDaemon(daemon);
		}
		synchronized (threads) {
			removeTerminatedThreads();
			threads.add(thread);
		}
		return thread;
	}

	@Override
	public String toString() {
		return name;
	}

	private void removeTerminatedThreads() {
		Iterator<Thread> iter = threads.iterator();
		while (iter.hasNext()) {
			Thread thread = iter.next();
			if (!thread.isAlive()) {
				iter.remove();
			}
		}
	}

}
