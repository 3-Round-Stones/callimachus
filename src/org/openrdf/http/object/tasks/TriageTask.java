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
package org.openrdf.http.object.tasks;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.Request;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks to see if this request is already cached.
 * 
 * @author James Leigh
 * 
 */
public final class TriageTask extends Task {
	private Logger logger = LoggerFactory.getLogger(TriageTask.class);
	private Request req;
	private Filter filter;
	private Handler handler;
	private ObjectRepository repository;
	private CountDownLatch latch = new CountDownLatch(1);

	public TriageTask(ObjectRepository repository,
			Request request, Filter filter, Handler handler) {
		super(request, filter);
		this.repository = repository;
		this.req = request;
		this.filter = filter;
		this.handler = handler;
	}

	@Override
	public int getGeneration() {
		return 0;
	}

	public void awaitVerification(long time, TimeUnit unit)
			throws InterruptedException {
		latch.await(time, unit);
		super.awaitVerification(time, unit);
	}

	@Override
	public void cleanup() {
		latch.countDown();
		try {
			req.cleanup();
		} catch (IOException e) {
			logger.error(e.toString(), e);
		} catch (RepositoryException e) {
			logger.error(e.toString(), e);
		}
	}

	@Override
	protected void close() {
		latch.countDown();
		req.close();
	}

	void perform() throws Exception {
		HttpResponse resp = filter.intercept(req);
		if (resp == null) {
			req = filter.filter(req);
			ResourceOperation op = new ResourceOperation(req,
					repository);
			bear(new ProcessTask(req, filter, op, handler));
			latch.countDown();
		} else {
			cleanup();
			submitResponse(resp);
		}
	}
}
