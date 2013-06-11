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
package org.callimachusproject.server.helpers;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpResponse;
import org.apache.http.concurrent.FutureCallback;

public class DelegatingFuture implements Future<HttpResponse>, FutureCallback<HttpResponse> {
	private final FutureCallback<HttpResponse> callback;
	private Future<?> delegate;
	private HttpResponse result;
	private Throwable thrown;

	public DelegatingFuture(FutureCallback<HttpResponse> callback) {
		this.callback = callback;
	}

	public String toString() {
		return String.valueOf(result);
	}

	public synchronized Future<?> getDelegate() {
		return delegate;
	}

	public synchronized void setDelegate(Future<?> delegate) {
		this.delegate = delegate;
	}

	public synchronized void setDelegateIfNull(Future<?> delegate) {
		if (this.delegate == null) {
			this.delegate = delegate;
		}
	}

	private synchronized HttpResponse getResult() {
		return result;
	}

	private synchronized void setResult(HttpResponse result) {
		this.result = result;
	}

	private synchronized Throwable getThrown() {
		return thrown;
	}

	private synchronized void setThrown(Throwable thrown) {
		this.thrown = thrown;
	}

	public boolean isCancelled() {
		Future<?> delegate = getDelegate();
		return delegate != null && delegate.isCancelled();
	}

	public boolean isDone() {
		Future<?> delegate = getDelegate();
		return delegate != null && getDelegate().isDone();
	}

	public HttpResponse get() throws InterruptedException,
			ExecutionException {
		getDelegate().get();
		HttpResponse result = getResult();
		Throwable thrown = getThrown();
		if (thrown != null)
			throw new ExecutionException(thrown);
		return result;
	}

	public HttpResponse get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		getDelegate().get(timeout, unit);
		HttpResponse result = getResult();
		Throwable thrown = getThrown();
		if (thrown != null)
			throw new ExecutionException(thrown);
		return result;
	}

    public void completed(final HttpResponse result) {
        setResult(result);
        if (this.callback != null) {
            this.callback.completed(result);
        }
    }

    public void failed(final Exception exception) {
        setThrown(exception);
        if (this.callback != null) {
            this.callback.failed(exception);
        }
    }

    public boolean cancel(final boolean mayInterruptIfRunning) {
        if (this.callback != null) {
            this.callback.cancelled();
        }
        return getDelegate().cancel(mayInterruptIfRunning);
    }

    public boolean cancel() {
        return cancel(true);
    }

	@Override
	public void cancelled() {
        if (this.callback != null) {
            this.callback.cancelled();
        }
	}

}
