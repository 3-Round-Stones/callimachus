/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.server.util;

import info.aduna.concurrent.locks.Lock;
import info.aduna.concurrent.locks.LockManager;
import info.aduna.concurrent.locks.Properties;

public class LockCleanupManager {
	private final boolean prefWrite;
	private final LockManager activeWriter;
	private final LockManager activeReaders;
	private volatile boolean writeRequested = false;

	public LockCleanupManager(boolean prefWrite) {
		this.prefWrite = prefWrite;
		boolean trace = Properties.lockTrackingEnabled();
		activeWriter = new LockManager(trace);
		activeReaders = new LockManager(trace);
	}

	public boolean isActiveLock() {
		return activeReaders.isActiveLock() || activeWriter.isActiveLock();
	}

	/**
	 * Gets a read lock. This method blocks when a write lock is in use or has
	 * been requested until the write lock is released.
	 */
	public Lock getReadLock(String ref)
		throws InterruptedException
	{
		if (!prefWrite)
			return getNextReadLock(ref);
		while (true) {
			Lock lock = tryReadLock(ref);
			if (lock != null)
				return lock;
			waitForActiveWriter();
		}
	}

	/**
	 * Gets an exclusive write lock. This method blocks when the write lock is in
	 * use or has already been requested until the write lock is released. This
	 * method also block when read locks are active until all of them are
	 * released.
	 */
	public Lock getWriteLock(String ref)
		throws InterruptedException
	{
		if (prefWrite)
			return getNextWriteLock(ref);
		while (true) {
			Lock lock = tryWriteLock(ref);
			if (lock != null)
				return lock;
			waitForActiveWriter();
			waitForActiveReaders();
		}
	}

	/**
	 * Gets a read lock. This method blocks when a write lock is in use or has
	 * been requested until the write lock is released.
	 */
	private synchronized Lock getNextReadLock(String ref)
		throws InterruptedException
	{
		// Wait for the writer to finish
		while (isWriterActive()) {
			waitForActiveWriter();
		}
	
		return createReadLock(ref);
	}

	/**
	 * Gets an exclusive write lock. This method blocks when the write lock is in
	 * use or has already been requested until the write lock is released. This
	 * method also block when read locks are active until all of them are
	 * released.
	 */
	private synchronized Lock getNextWriteLock(String ref)
		throws InterruptedException
	{
		writeRequested = true;
		try {
			// Wait for the write lock to be released
			while (isWriterActive()) {
				waitForActiveWriter();
			}
	
			// Wait for the read locks to be released
			while (isReaderActive()) {
				waitForActiveReaders();
			}
	
			return createWriteLock(ref);
		} finally {
			writeRequested = false;
		}
	}

	/**
	 * Gets a read lock, if available. This method will return <tt>null</tt> if
	 * the read lock is not immediately available.
	 */
	private Lock tryReadLock(String ref) {
		if (writeRequested || isWriterActive()) {
			return null;
		}
		synchronized (this) {
			if (isWriterActive()) {
				return null;
			}
	
			return createReadLock(ref);
		}
	}

	/**
	 * Gets an exclusive write lock, if available. This method will return
	 * <tt>null</tt> if the write lock is not immediately available.
	 */
	private Lock tryWriteLock(String ref) {
		if (isWriterActive() || isReaderActive())
			return null;
		synchronized (this) {
			if (isWriterActive() || isReaderActive()) {
				return null;
			}
	
			return createWriteLock(ref);
		}
	}

	/**
	 * If a writer is active
	 */
	private boolean isWriterActive() {
		return activeWriter.isActiveLock();
	}

	/**
	 * If one or more readers are active
	 */
	private boolean isReaderActive() {
		return activeReaders.isActiveLock();
	}

	/**
	 * Blocks current thread until after the writer lock is released (if active).
	 * 
	 * @throws InterruptedException
	 */
	private void waitForActiveWriter()
		throws InterruptedException
	{
		activeWriter.waitForActiveLocks();
	}

	/**
	 * Blocks current thread until there are no reader locks active.
	 * 
	 * @throws InterruptedException
	 */
	private void waitForActiveReaders()
		throws InterruptedException
	{
		activeReaders.waitForActiveLocks();
	}

	/**
	 * Creates a new Lock for reading and increments counter for active readers.
	 * The lock is tracked if lock tracking is enabled. This method is not thread
	 * safe itself, the calling method is expected to handle synchronization
	 * issues.
	 * 
	 * @return a read lock.
	 */
	private Lock createReadLock(String ref) {
		return activeReaders.createLock(ref);
	}

	/**
	 * Creates a new Lock for writing. The lock is tracked if lock tracking is
	 * enabled. This method is not thread safe itself for performance reasons,
	 * the calling method is expected to handle synchronization issues.
	 * 
	 * @return a write lock.
	 */
	private Lock createWriteLock(String ref) {
		return activeWriter.createLock(ref);
	}
}
