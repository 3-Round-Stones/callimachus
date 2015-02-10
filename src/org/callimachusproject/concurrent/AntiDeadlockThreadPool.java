/*
 * Copyright (c) 2010, Zepheira LLC, Some rights reserved.
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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * {@link ThreadPoolExecutor} that will increase the number of threads when the
 * queue has not changed in five seconds.
 * 
 * @author James Leigh
 * 
 */
public class AntiDeadlockThreadPool extends ManagedThreadPool {
	private static ScheduledExecutorService scheduler = ManagedExecutors
			.getInstance().getTimeoutThreadPool();
	private final long delay;
	private final TimeUnit delayUnit;
	private int corePoolSize;
	private int maximumPoolSize;
	private BlockingQueue<Runnable> queue;
	private ScheduledFuture<?> schedule;

	public AntiDeadlockThreadPool(int corePoolSize, int maximumPoolSize,
			BlockingQueue<Runnable> queue, String name) {
		this(corePoolSize, maximumPoolSize, queue, name, 5, TimeUnit.SECONDS);
	}

	public AntiDeadlockThreadPool(int corePoolSize, int maximumPoolSize,
			BlockingQueue<Runnable> queue, String name, long delay,
			TimeUnit delayUnit) {
		super(corePoolSize, maximumPoolSize, 60L, TimeUnit.MINUTES, queue,
				name, true);
		this.queue = queue;
		this.corePoolSize = corePoolSize;
		this.maximumPoolSize = maximumPoolSize;
		this.setAllowCoreThreadTimeOut(true);
		this.delay = delay;
		this.delayUnit = delayUnit;
	}

	@Override
	public synchronized void execute(Runnable command) {
		try {
			super.execute(command);
		} finally {
			checkQueue();
		}
	}

	@Override
	public Future<?> submit(Runnable task) {
		try {
			return super.submit(task);
		} finally {
			checkQueue();
		}
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		try {
			return super.submit(task, result);
		} finally {
			checkQueue();
		}
	}

	@Override
	public synchronized <T> Future<T> submit(Callable<T> task) {
		try {
			return super.submit(task);
		} finally {
			checkQueue();
		}
	}

	private void checkQueue() {
		if (corePoolSize >= maximumPoolSize)
			return;
		final Runnable top = queue.peek();
		if (schedule == null && top != null) {
			schedule = scheduler.scheduleWithFixedDelay(new Runnable() {
				private Runnable previous = top;

				public String toString() {
					return "check for starving tasks";
				}

				public void run() {
					synchronized (AntiDeadlockThreadPool.this) {
						Runnable peek = queue.peek();
						if (peek == null || corePoolSize >= maximumPoolSize) {
							if (schedule != null) {
								schedule.cancel(false);
								schedule = null;
							}
						} else if (previous == peek) {
							setCorePoolSize(++corePoolSize);
						} else {
							previous = peek;
						}
					}
				}
			}, delay, delay, delayUnit);
		}
	}

}
