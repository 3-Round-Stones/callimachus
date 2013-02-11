/*
 * Copyright (c) 2010, Zepheira LLC Some rights reserved.
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

import java.io.IOException;
import java.lang.management.ThreadInfo;

/**
 * Interface to manage ThreadPools from MXBeans.
 *
 * @author James Leigh
 **/
public interface ThreadPoolMXBean {

	String getName();

	String[] getActiveStackDump();

	void threadDumpToFile(String outputFile) throws IOException;

	ThreadInfo[] getLiveThreadInfo(int maxDepth);

	String[] getQueueDescription();

	int getQueueSize();

	int getQueueRemainingCapacity();

	void clearQueue();

	void runNextInQueue();

	void runAllInQueue();

	boolean isContinueExistingPeriodicTasksAfterShutdownPolicy();

	void setContinueExistingPeriodicTasksAfterShutdownPolicy(boolean policy);

	boolean isExecuteExistingDelayedTasksAfterShutdownPolicy();

	void setExecuteExistingDelayedTasksAfterShutdownPolicy(boolean policy);

	void setAllowCoreThreadTimeOut(boolean allow);

	boolean isAllowsCoreThreadTimeOut();

	int getActiveCount();

	long getCompletedTaskCount();

	int getLargestPoolSize();

	int getPoolSize();

	long getTaskCount();

	boolean isShutdown();

	boolean isTerminated();

	boolean isTerminating();

	void startAllCoreThreads();

	void startCoreThread();

	void purge();

	int getCorePoolSize();

	void setCorePoolSize(int size);

	long getKeepAliveTime();

	void setKeepAliveTime(long seconds);

	int getMaximumPoolSize();

	void setMaximumPoolSize(int size);

	void shutdown();

	void interruptWorkers() throws InterruptedException;
}
