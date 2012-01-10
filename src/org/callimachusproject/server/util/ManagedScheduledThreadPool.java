/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
 * Copyright (c) 2011, Talis Inc., Some rights reserved.
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

import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exposes ScheduledThreadPoolExecutor properties in an MXBean.
 * 
 * @author James Leigh
 * 
 */
public class ManagedScheduledThreadPool extends ManagedThreadPool implements
		ScheduledExecutorService {
	private final Logger logger = LoggerFactory
			.getLogger(ManagedScheduledThreadPool.class);

	public ManagedScheduledThreadPool(String name, boolean daemon) {
		this(1, name, daemon, new ScheduledThreadPoolExecutor.AbortPolicy());
	}

	public ManagedScheduledThreadPool(int corePoolSize, String name,
			boolean daemon) {
		this(corePoolSize, name, daemon,
				new ScheduledThreadPoolExecutor.AbortPolicy());
	}

	public ManagedScheduledThreadPool(int corePoolSize, String name,
			boolean daemon, RejectedExecutionHandler handler) {
		super(new ScheduledThreadPoolExecutor(corePoolSize, handler),
				new NamedThreadFactory(name, daemon));
	}

	public synchronized void interruptWorkers() throws InterruptedException {
		int corePoolSize = getCorePoolSize();
		ThreadFactory factory = getDelegate().getThreadFactory();
		RejectedExecutionHandler handler = getDelegate()
				.getRejectedExecutionHandler();
		try {
			logger.info("Terminating {} {} threads", getActiveCount(),
					toString());
			getDelegate().shutdown();
			if (!getDelegate().awaitTermination(1, TimeUnit.MINUTES)) {
				logger.info("Could not terminate {} {} threads",
						getActiveCount(), toString());
			}
		} finally {
			setDelegate(new ScheduledThreadPoolExecutor(corePoolSize, factory,
					handler));
		}
	}

	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay,
			TimeUnit unit) {
		return getDelegate().schedule(callable, delay, unit);
	}

	public ScheduledFuture<?> schedule(Runnable command, long delay,
			TimeUnit unit) {
		return getDelegate().schedule(command, delay, unit);
	}

	public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
			long initialDelay, long period, TimeUnit unit) {
		return getDelegate().scheduleAtFixedRate(command, initialDelay, period,
				unit);
	}

	public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
			long initialDelay, long delay, TimeUnit unit) {
		return getDelegate().scheduleWithFixedDelay(command, initialDelay,
				delay, unit);
	}

	@Override
	protected synchronized ScheduledThreadPoolExecutor getDelegate() {
		return (ScheduledThreadPoolExecutor) super.getDelegate();
	}

}
