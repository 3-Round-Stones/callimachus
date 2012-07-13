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
package org.callimachusproject.server.tasks;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;

import org.callimachusproject.server.CallimachusRepository;
import org.callimachusproject.server.model.Filter;
import org.callimachusproject.server.model.Handler;
import org.callimachusproject.server.model.Request;
import org.callimachusproject.server.util.ManagedExecutors;
import org.callimachusproject.xslt.XSLTransformer;

/**
 * Executes tasks in a thread pool or in the current thread.
 * 
 * @author James Leigh
 * 
 */
public class TaskFactory {

	private static final Executor executor;
	private static final Executor foreground;
	static {
		Comparator<Runnable> cmp = new Comparator<Runnable>() {
			public int compare(Runnable o1, Runnable o2) {
				Task t1 = (Task) o1;
				Task t2 = (Task) o2;
				if (t1.getGeneration() < t2.getGeneration())
					return -1;
				if (t1.getGeneration() > t2.getGeneration())
					return 1;
				if (t1.isStorable() && !t2.isStorable())
					return -1;
				if (!t1.isStorable() && t2.isStorable())
					return 1;
				if (t1.isSafe() && !t2.isSafe())
					return -1;
				if (!t1.isSafe() && t2.isSafe())
					return 1;
				if (t1.getReceivedOn() < t2.getReceivedOn())
					return -1;
				if (t1.getReceivedOn() > t2.getReceivedOn())
					return 1;
				return System.identityHashCode(t1)
						- System.identityHashCode(t2);
			};
		};
		BlockingQueue<Runnable> queue = new PriorityBlockingQueue<Runnable>(32,
				cmp);
		executor = ManagedExecutors.newAntiDeadlockThreadPool(queue,
				"HttpHandler");
		foreground = new Executor() {
			public void execute(Runnable command) {
				Thread.yield();
				command.run();
			}
		};
	}

	private CallimachusRepository repo;
	private Filter filter;
	private Handler handler;
	private XSLTransformer transformer;

	public TaskFactory(CallimachusRepository repository,
			Filter filter, Handler handler) {
		this.repo = repository;
		this.filter = filter;
		this.handler = handler;
	}

	public String getErrorXSLT() {
		return transformer.getSystemId();
	}

	public void setErrorXSLT(String url) {
		this.transformer = new XSLTransformer(url);
	}

	public Task createBackgroundTask(Request req) {
		Task task = new TriageTask(repo, req, filter, handler);
		task.setErrorXSLT(transformer);
		if (req.isStorable()) {
			task.setExecutor(executor);
			executor.execute(task);
		} else {
			task.setExecutor(executor);
			task.run();
		}
		return task;
	}

	public Task createForegroundTask(Request req) {
		Task task = new TriageTask(repo, req, filter, handler);
		task.setErrorXSLT(transformer);
		task.setExecutor(foreground);
		task.run();
		return task;
	}

}
