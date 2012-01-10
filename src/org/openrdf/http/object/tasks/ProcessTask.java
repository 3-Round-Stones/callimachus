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

import org.openrdf.http.object.model.Filter;
import org.openrdf.http.object.model.Handler;
import org.openrdf.http.object.model.Request;
import org.openrdf.http.object.model.ResourceOperation;
import org.openrdf.http.object.model.Response;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Invokes the operation and submits the response when this task is run.
 * 
 * @author James Leigh
 * 
 */
public class ProcessTask extends Task {
	private Logger logger = LoggerFactory.getLogger(ProcessTask.class);
	private ResourceOperation op;
	private Handler handler;
	private CountDownLatch latch = new CountDownLatch(1);
	private boolean content;
	private int generation;

	public ProcessTask(Request request, Filter filter,
			ResourceOperation operation, Handler handler) {
		this(request, filter, operation, handler, 1);
	}

	public ProcessTask(Request request, Filter filter,
			ResourceOperation operation, Handler handler, int generation) {
		super(request, filter);
		this.op = operation;
		this.handler = handler;
		this.generation = generation;
		assert generation > 0;
	}

	@Override
	public int getGeneration() {
		return generation;
	}

	public void verified() {
		latch.countDown();
	}

	public void awaitVerification(long time, TimeUnit unit)
			throws InterruptedException {
		latch.await(time, unit);
	}

	public void perform() throws Exception {
		try {
			handle();
		} finally {
			verified();
		}
	}

	@Override
	public void abort() {
		verified();
		super.abort();
		try {
			op.cleanup();
		} catch (IOException e) {
			logger.error(e.toString(), e);
		} catch (RepositoryException e) {
			logger.error(e.toString(), e);
		}
	}

	@Override
	public void cleanup() {
		super.cleanup();
		try {
			if (!content) {
				op.cleanup();
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
		} catch (RepositoryException e) {
			logger.error(e.toString(), e);
		}
	}

	@Override
	protected void close() {
		if (!content) {
			op.close();
		}
	}

	private void handle() throws Exception {
		op.begin();
		Response resp = handler.verify(op);
		if (resp == null) {
			verified();
			resp = handler.handle(op);
			if (resp.getStatusCode() >= 400) {
				op.rollback();
			} else if (!op.isSafe()) {
				op.commit();
			}
			if (resp.isContent() && !resp.isException()) {
				content = true;
				resp.onClose(new Runnable() {
					public void run() {
						try {
							op.cleanup();
						} catch (IOException e) {
							logger.error(e.toString(), e);
						} catch (RepositoryException e) {
							logger.error(e.toString(), e);
						}
					}
				});
				submitResponse(resp);
			} else {
				op.cleanup();
				submitResponse(resp);
			}
		} else {
			submitResponse(resp);
		}
	}
}
