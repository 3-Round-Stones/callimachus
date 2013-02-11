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
package org.callimachusproject.concurrent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Common Executors used.
 * 
 * @author James Leigh
 * 
 */
public class ManagedExecutors {
	private static final ManagedExecutors instance = new ManagedExecutors();

	public static ManagedExecutors getInstance() {
		return instance;
	}

	private final Map<String, WeakReference<? extends ManagedThreadPool>> pools = new LinkedHashMap<String, WeakReference<? extends ManagedThreadPool>>();
	private final List<ManagedThreadPoolListener> listeners = new ArrayList<ManagedThreadPoolListener>();
	private ExecutorService producerThreadPool = newCachedPool("Producer");
	private ExecutorService parserThreadPool = newCachedPool("Parser");
	private ScheduledExecutorService timeoutThreadPool = newSingleScheduler("Timeout");

	public ExecutorService getProducerThreadPool() {
		return producerThreadPool;
	}

	public ExecutorService getParserThreadPool() {
		return parserThreadPool;
	}

	public ScheduledExecutorService getTimeoutThreadPool() {
		return timeoutThreadPool;
	}

	public ExecutorService newCachedPool(String name) {
		return register(new ManagedThreadPool(name, true));
	}

	public ExecutorService newFixedThreadPool(int nThreads,
			BlockingQueue<Runnable> queue, String name) {
		return register(new ManagedThreadPool(nThreads, nThreads, 0L,
				TimeUnit.MILLISECONDS, queue, "HttpTriage", true));
	}

	public ScheduledExecutorService newSingleScheduler(String name) {
		return register(new ManagedScheduledThreadPool(name, true));
	}

	public ExecutorService newAntiDeadlockThreadPool(
			BlockingQueue<Runnable> queue, String name) {
		return newAntiDeadlockThreadPool(Runtime.getRuntime()
				.availableProcessors() * 2 + 1, Runtime.getRuntime()
				.availableProcessors() * 100, queue, name);
	}

	public ExecutorService newAntiDeadlockThreadPool(int corePoolSize,
			int maximumPoolSize, BlockingQueue<Runnable> queue, String name) {
		return register(new AntiDeadlockThreadPool(corePoolSize,
				maximumPoolSize, queue, name));
	}

	public synchronized void addListener(ManagedThreadPoolListener listener) {
		cleanup(null);
		Iterator<String> iter = pools.keySet().iterator();
		while (iter.hasNext()) {
			String name = iter.next();
			WeakReference<? extends ManagedThreadPool> ref = pools.get(name);
			ManagedThreadPool pool = ref.get();
			if (pool != null && !pool.isTerminated()) {
				listener.threadPoolStarted(name, pool);
			}
		}
		listeners.add(listener);
	}

	public synchronized void removeListener(ManagedThreadPoolListener listener) {
		listeners.remove(listener);
		cleanup(null);
	}

	public synchronized void cleanup() {
		cleanup(null);
		Iterator<String> iter = pools.keySet().iterator();
		while (iter.hasNext()) {
			String name = iter.next();
			WeakReference<? extends ManagedThreadPool> ref = pools.get(name);
			ManagedThreadPool get = ref.get();
			if (get == null || get.isTerminated() || get.isTerminating()) {
				if (get != null && !get.isTerminated() && get.isTerminating()) {
					try {
						get.awaitTermination(1, TimeUnit.HOURS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
				for (ManagedThreadPoolListener listener : listeners) {
					listener.threadPoolTerminated(name);
				}
				iter.remove();
			}
		}
	}

	private synchronized <T extends ManagedThreadPool> T register(T pool) {
		String key = pool.toString();
		assert key != null;
		cleanup(key);
		pools.put(key, new WeakReference<T>(pool));
		for (ManagedThreadPoolListener listener : listeners) {
			listener.threadPoolStarted(key, pool);
		}
		return pool;
	}

	private synchronized void cleanup(String nameToTerminate) {
		Iterator<String> iter = pools.keySet().iterator();
		while (iter.hasNext()) {
			String name = iter.next();
			WeakReference<? extends ManagedThreadPool> ref = pools.get(name);
			ManagedThreadPool get = ref.get();
			if (get == null || get.isTerminated() || name.equals(nameToTerminate)) {
				if (name.equals(nameToTerminate) && get != null && !get.isTerminated()) {
					get.shutdownNow();
					try {
						get.awaitTermination(1, TimeUnit.HOURS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						break;
					}
				}
				for (ManagedThreadPoolListener listener : listeners) {
					listener.threadPoolTerminated(name);
				}
				iter.remove();
			}
		}
	}

	private ManagedExecutors() {
		// singleton
	}
}
