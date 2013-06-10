/*
 * Copyright 2013, 3 Round Stones Inc., Some rights reserved.
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

import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class InlineExecutorService extends AbstractExecutorService {
	private static class CompletedFuture<T> implements Future<T> {
		private final T result;
		private final Throwable throwable;

		public CompletedFuture() {
			this.result = null;
			this.throwable = null;
		}

		public CompletedFuture(T result) {
			this.result = result;
			this.throwable = null;
		}

		public CompletedFuture(Throwable throwable) {
			this.result = null;
			this.throwable = throwable;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public T get() throws ExecutionException {
			if (throwable != null)
				throw new ExecutionException(throwable);
			return result;
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException,
				ExecutionException, TimeoutException {
			return get();
		}
	};

	private final ThreadLocal<Boolean> foreground;
	private final ExecutorService delegate;

	public InlineExecutorService(ThreadLocal<Boolean> foreground,
			ExecutorService delegate) {
		this.foreground = foreground;
		this.delegate = delegate;
	}

	public void execute(Runnable command) {
		delegate.execute(command);
	}

	public void shutdown() {
		delegate.shutdown();
	}

	public List<Runnable> shutdownNow() {
		return delegate.shutdownNow();
	}

	public boolean isShutdown() {
		return delegate.isShutdown();
	}

	public boolean isTerminated() {
		return delegate.isTerminated();
	}

	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return delegate.awaitTermination(timeout, unit);
	}

	public <T> Future<T> submit(Callable<T> task) {
		if (isForeground()) {
			try {
				return new CompletedFuture<T>(task.call());
			} catch (Exception e) {
				return new CompletedFuture<T>(e);
			}
		} else {
			return delegate.submit(task);
		}
	}

	public <T> Future<T> submit(Runnable task, T result) {
		if (isForeground()) {
			try {
				task.run();
				return new CompletedFuture<T>(result);
			} catch (Exception e) {
				return new CompletedFuture<T>(e);
			}
		} else {
			return delegate.submit(task, result);
		}
	}

	public Future<?> submit(Runnable task) {
		if (isForeground()) {
			try {
				task.run();
				return new CompletedFuture<Void>();
			} catch (Exception e) {
				return new CompletedFuture<Void>(e);
			}
		} else {
			return delegate.submit(task);
		}
	}

	private boolean isForeground() {
		Boolean bool = foreground.get();
		return bool != null && bool;
	}

}
