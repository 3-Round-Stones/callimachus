package org.openrdf.http.object.util;

import info.aduna.concurrent.locks.Lock;
import info.aduna.concurrent.locks.ReadWriteLockManager;
import info.aduna.concurrent.locks.WritePrefReadWriteLockManager;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockCleanupManager {
	private static class LockTrace {
		Lock lock;
		WeakReference<Lock> weak;
		String ref;
		Throwable stack;
	}

	private final Logger logger = LoggerFactory
			.getLogger(LockCleanupManager.class);
	private final ReadWriteLockManager delegate = new WritePrefReadWriteLockManager();
	private final Set<LockTrace> locks = new HashSet<LockTrace>();
	private final boolean prefWrite;
	private int waitingWriters = 0;
	private int waitingReaders = 0;

	public LockCleanupManager(boolean prefWrite) {
		this.prefWrite = prefWrite;
	}

	public Lock getReadLock(String ref) throws InterruptedException {
		Lock lock = null;
		synchronized (delegate) {
			waitingReaders++;
		}
		try {
			boolean first = true;
			while (lock == null) {
				synchronized (delegate) {
					while (prefWrite && waitingWriters > 0) {
						// Wait for any writing threads to finish
						delegate.wait();
					}
					lock = delegate.tryReadLock();
					if (lock == null) {
						delegate.wait(1000);
					}
				}
				if (lock == null) {
					if (first) {
						first = false;
					} else {
						releaseAbanded();
					}
				}
			}
		} finally {
			synchronized (delegate) {
				waitingReaders--;
				delegate.notifyAll();
			}
		}
		return track(lock, ref, new Throwable());
	}

	public Lock getWriteLock(String ref) throws InterruptedException {
		Lock lock = null;
		synchronized (delegate) {
			waitingWriters++;
		}
		try {
			boolean first = true;
			while (lock == null) {
				synchronized (delegate) {
					while (!prefWrite && waitingReaders > 0) {
						delegate.wait();
					}
					lock = delegate.tryWriteLock();
					if (lock == null) {
						delegate.wait(1000);
					}
				}
				if (lock == null) {
					if (first) {
						first = false;
					} else {
						releaseAbanded();
					}
				}
			}
		} finally {
			synchronized (delegate) {
				waitingWriters--;
				delegate.notifyAll();
			}
		}
		return track(lock, ref, new Throwable());
	}

	private void releaseAbanded() {
		System.gc();
		Thread.yield();
		synchronized (locks) {
			if (!locks.isEmpty()) {
				LockTrace[] ar = new LockTrace[locks.size()];
				for (LockTrace trace : locks.toArray(ar)) {
					if (trace.lock.isActive() && trace.weak.get() == null) {
						String msg = "Lock " + trace.ref + " abandoned";
						logger.warn(msg, trace.stack);
						trace.lock.release();
					}
				}
			}
		}
	}

	private Lock track(final Lock lock, String ref, Throwable stack) {
		final LockTrace trace = new LockTrace();
		trace.lock = lock;
		trace.ref = ref;
		trace.stack = stack;
		Lock weakLock = new Lock() {
			public boolean isActive() {
				return lock.isActive();
			}

			public void release() {
				try {
					lock.release();
				} finally {
					synchronized (locks) {
						locks.remove(trace);
					}
				}
			}
		};
		trace.weak = new WeakReference<Lock>(weakLock);
		synchronized (locks) {
			locks.add(trace);
		}
		return weakLock;
	}

}
