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
package org.callimachusproject.concurrent;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.State;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exposes ThreadPoolExecutor properties in an MXBean.
 * 
 * @author James Leigh
 * 
 */
public class ManagedThreadPool implements ExecutorService, ThreadPoolMXBean {
	private final Logger logger = LoggerFactory.getLogger(ManagedThreadPool.class);
	private ThreadPoolExecutor delegate;
	private final NamedThreadFactory threads;

	protected ManagedThreadPool(String name, boolean daemon) {
		this(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), name, daemon,
				new ThreadPoolExecutor.AbortPolicy());
	}

	protected ManagedThreadPool(int nThreads, String name, boolean daemon) {
		this(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
				new SynchronousQueue<Runnable>(), name, daemon,
				new ThreadPoolExecutor.AbortPolicy());
	}

	protected ManagedThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, String name, boolean daemon) {
		this(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue,
				name, daemon, new ThreadPoolExecutor.AbortPolicy());
	}

	protected ManagedThreadPool(int corePoolSize, int maximumPoolSize,
			long keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue, String name, boolean daemon,
			RejectedExecutionHandler handler) {
		this(new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
				keepAliveTime, unit, workQueue, handler),
				new NamedThreadFactory(name, daemon));
	}

	protected ManagedThreadPool(ThreadPoolExecutor delegate,
			NamedThreadFactory factory) {
		this.threads = factory;
		delegate.setThreadFactory(factory);
		setDelegate(delegate);
	}

	public String getName() {
		return threads.toString();
	}

	@Override
	public String toString() {
		return threads.toString();
	}

	public void setCorePoolSize(int corePoolSize) {
		if (getCorePoolSize() > corePoolSize)
			logger.info("Increasing {} thread  pool size to {}", toString(),
					corePoolSize);
		getDelegate().setCorePoolSize(corePoolSize);
	}

	public void shutdown() {
		getDelegate().shutdown();
	}

	public List<Runnable> shutdownNow() {
		List<Runnable> tasks = getDelegate().shutdownNow();
		return tasks;
	}

	public synchronized void interruptWorkers() throws InterruptedException {
		int corePoolSize = getCorePoolSize();
		int maximumPoolSize = getMaximumPoolSize();
		long keepAliveTime = getKeepAliveTime();
		TimeUnit unit = TimeUnit.SECONDS;
		BlockingQueue<Runnable> workQueue = getDelegate().getQueue();
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
			setDelegate(new ThreadPoolExecutor(corePoolSize, maximumPoolSize,
					keepAliveTime, unit, workQueue, factory, handler));
		}
	}

	public void clearQueue() {
		getQueue().clear();
	}

	public long getKeepAliveTime() {
		return getDelegate().getKeepAliveTime(TimeUnit.SECONDS);
	}

	public void setKeepAliveTime(long seconds) {
		getDelegate().setKeepAliveTime(seconds, TimeUnit.SECONDS);
	}

	public void threadDumpToFile(String outputFile) throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(outputFile, true));
		try {
			writer.print("Name:\t");
			writer.println(getName());
			writer.print("Active:\t");
			writer.println(getActiveCount());
			writer.print("Task count:\t");
			writer.println(getTaskCount());
			writer.print("Completed task count:\t");
			writer.println(getCompletedTaskCount());
			writer.print("Pool size:\t");
			writer.println(getPoolSize());
			writer.print("Core pool size:\t");
			writer.println(getCorePoolSize());
			writer.print("Largest pool size:\t");
			writer.println(getLargestPoolSize());
			writer.print("Maximum pool size:\t");
			writer.println(getMaximumPoolSize());
			writer.print("Keep alive Time:\t");
			writer.println(getKeepAliveTime());
			writer.print("Queue size:\t");
			writer.println(getQueueSize());
			writer.print("Queue remaining capacity:\t");
			writer.println(getQueueRemainingCapacity());
			writer.print("Allow core thread time out:\t");
			writer.println(isAllowsCoreThreadTimeOut());
			writer.print("Continue existing periodic tasks after shutdown:\t");
			writer.println(isContinueExistingPeriodicTasksAfterShutdownPolicy());
			writer.print("Execute existing delayed tasks after shutdown:\t");
			writer.println(isExecuteExistingDelayedTasksAfterShutdownPolicy());
			writer.print("Shutdown:\t");
			writer.println(isShutdown());
			writer.print("Terminating:\t");
			writer.println(isTerminating());
			writer.print("Terminated:\t");
			writer.println(isTerminated());
			writer.println();
			for (String trace : getActiveStackDump()) {
				writer.println(trace);
				writer.println();
			}
			for (String pending : getQueueDescription()) {
				writer.println(pending);
			}
			writer.println();
			writer.println();
		} finally {
			writer.close();
		}
		logger.info("Thread pool dump: {}", outputFile);
	}

	public String[] getActiveStackDump() {
		ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
		List<String> result = new ArrayList<String>();
		for (ThreadInfo info : getLiveThreadInfo(Integer.MAX_VALUE)) {
			if (!isWaitingForNewTask(info)) {
				StringWriter writer = new StringWriter();
				PrintWriter s = new PrintWriter(writer);
				printThreadInfo(info, mxBean, s);
				printStackTrace(info.getStackTrace(), info, s);
				printLockInfo(info.getLockedSynchronizers(), s);
				s.flush();
				result.add(writer.toString());
			}
		}
		return result.toArray(new String[result.size()]);
	}

	public ThreadInfo[] getLiveThreadInfo(int maxDepth) {
		ThreadMXBean bean = ManagementFactory.getThreadMXBean();
		Thread[] liveThreads = threads.getLiveThreads();
		ThreadInfo[] result = new ThreadInfo[liveThreads.length];
		for (int i = 0; i < liveThreads.length; i++) {
			result[i] = bean.getThreadInfo(liveThreads[i].getId(), maxDepth);
		}
		return result;
	}

	public String[] getQueueDescription() {
		Object[] tasks = getQueue().toArray();
		String[] result = new String[tasks.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = tasks[i].toString();
		}
		return result;
	}

	public void runAllInQueue() {
		Runnable task;
		while ((task = getQueue().poll()) != null) {
			task.run();
		}
	}

	public void runNextInQueue() {
		Runnable task = getQueue().poll();
		if (task != null) {
			task.run();
		}
	}

	public int getQueueRemainingCapacity() {
		return getQueue().remainingCapacity();
	}

	public int getQueueSize() {
		return getQueue().size();
	}

	public boolean isAllowsCoreThreadTimeOut() {
		return getDelegate().allowsCoreThreadTimeOut();
	}

	public boolean isContinueExistingPeriodicTasksAfterShutdownPolicy() {
		return false;
	}

	public boolean isExecuteExistingDelayedTasksAfterShutdownPolicy() {
		return false;
	}

	public void setAllowCoreThreadTimeOut(boolean allow) {
		getDelegate().allowCoreThreadTimeOut(allow);
	}

	public void setContinueExistingPeriodicTasksAfterShutdownPolicy(
			boolean policy) {
		throw new UnsupportedOperationException();
	}

	public void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean policy) {
		throw new UnsupportedOperationException();
	}

	public void startAllCoreThreads() {
		getDelegate().prestartAllCoreThreads();
	}

	public void startCoreThread() {
		getDelegate().prestartCoreThread();
	}

	public int getActiveCount() {
		return getDelegate().getActiveCount();
	}

	public long getCompletedTaskCount() {
		return getDelegate().getCompletedTaskCount();
	}

	public int getCorePoolSize() {
		return getDelegate().getCorePoolSize();
	}

	public int getPoolSize() {
		return getDelegate().getPoolSize();
	}

	public long getTaskCount() {
		return getDelegate().getTaskCount();
	}

	public boolean isTerminating() {
		return getDelegate().isTerminating();
	}

	public int getLargestPoolSize() {
		return getDelegate().getLargestPoolSize();
	}

	public int getMaximumPoolSize() {
		return getDelegate().getMaximumPoolSize();
	}

	public void purge() {
		getDelegate().purge();
	}

	public void setMaximumPoolSize(int maximumPoolSize) {
		getDelegate().setMaximumPoolSize(maximumPoolSize);
	}

	public boolean awaitTermination(long timeout, TimeUnit unit)
			throws InterruptedException {
		return getDelegate().awaitTermination(timeout, unit);
	}

	public boolean isShutdown() {
		return getDelegate().isShutdown();
	}

	public boolean isTerminated() {
		return getDelegate().isTerminated();
	}

	public void execute(Runnable command) {
		getDelegate().execute(command);
	}

	public <T> List<Future<T>> invokeAll(
			Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return getDelegate().invokeAll(tasks, timeout, unit);
	}

	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
			throws InterruptedException {
		return getDelegate().invokeAll(tasks);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
			long timeout, TimeUnit unit) throws InterruptedException,
			ExecutionException, TimeoutException {
		return getDelegate().invokeAny(tasks, timeout, unit);
	}

	public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
			throws InterruptedException, ExecutionException {
		return getDelegate().invokeAny(tasks);
	}

	public <T> Future<T> submit(Callable<T> task) {
		return getDelegate().submit(task);
	}

	public <T> Future<T> submit(Runnable task, T result) {
		return getDelegate().submit(task, result);
	}

	public Future<?> submit(Runnable task) {
		return getDelegate().submit(task);
	}

	protected synchronized ThreadPoolExecutor getDelegate() {
		return delegate;
	}

	protected synchronized void setDelegate(ThreadPoolExecutor delegate) {
		this.delegate = delegate;
	}

	private BlockingQueue<Runnable> getQueue() {
		return getDelegate().getQueue();
	}

	private boolean isWaitingForNewTask(ThreadInfo info) {
		if (info.getThreadState() == State.RUNNABLE)
			return false;
		StackTraceElement[] stack = info.getStackTrace();
		if (stack.length < 4)
			return false;
		for (int i = stack.length - 1; i >=0;i--) {
			StackTraceElement trace = stack[i];
			String cname = trace.getClassName();
			if (cname.startsWith(ThreadPoolExecutor.class.getName())) {
				if ("getTask".equals(trace.getMethodName()))
					return true;
			} else if (!Thread.class.getName().equals(cname)) {
				return false;
			}
		}
		return false;
	}

	private void printThreadInfo(ThreadInfo threadInfo, ThreadMXBean mxBean,
			PrintWriter writer) {
		writer.println("    native=" + threadInfo.isInNative() + ", suspended="
				+ threadInfo.isSuspended() + ", block="
				+ threadInfo.getBlockedCount() + ", wait="
				+ threadInfo.getWaitedCount());
		writer.println("    lock="
				+ threadInfo.getLockName()
				+ " owned by "
				+ threadInfo.getLockOwnerName()
				+ " ("
				+ threadInfo.getLockOwnerId()
				+ "), cpu="
				+ (mxBean.getThreadCpuTime(threadInfo.getThreadId()) / 1000000L)
				+ ", user="
				+ (mxBean.getThreadUserTime(threadInfo.getThreadId()) / 1000000L));
	}

	private void printStackTrace(StackTraceElement[] stacktrace,
			ThreadInfo threadInfo, PrintWriter writer) {
		MonitorInfo[] monitors = threadInfo.getLockedMonitors();
		for (int i = 0; i < stacktrace.length; i++) {
			StackTraceElement ste = stacktrace[i];
			writer.println("\tat " + ste.toString());
			for (MonitorInfo mi : monitors) {
				if (mi.getLockedStackDepth() == i) {
					writer.println("\t  - locked " + mi);
				}
			}
		}
		writer.println();
	}

	private void printLockInfo(LockInfo[] locks, PrintWriter s) {
		s.println("\tLocked synchronizers: count = " + locks.length);
		for (LockInfo li : locks) {
			s.println("\t  - " + li);
		}
	}

}
