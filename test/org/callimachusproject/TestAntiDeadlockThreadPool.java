package org.callimachusproject;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import org.callimachusproject.concurrent.AntiDeadlockThreadPool;

public class TestAntiDeadlockThreadPool extends TestCase {

	public void testExecute() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final CountDownLatch four = new CountDownLatch(4);
		AntiDeadlockThreadPool pool = new AntiDeadlockThreadPool(3, 100,
				new ArrayBlockingQueue<Runnable>(32), "HttpHandling", 100,
				TimeUnit.MILLISECONDS);
		try {
			Runnable await = new Runnable() {
				public void run() {
					try {
						four.countDown();
						latch.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			};
			pool.execute(await);
			pool.execute(await);
			pool.execute(await);
			pool.execute(await);
			assertTrue(four.await(10, TimeUnit.SECONDS));
		} finally {
			latch.countDown();
			pool.shutdownNow();
		}
	}

	public void testSubmit() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final CountDownLatch four = new CountDownLatch(4);
		AntiDeadlockThreadPool pool = new AntiDeadlockThreadPool(3, 100,
				new ArrayBlockingQueue<Runnable>(32), "HttpHandling", 100,
				TimeUnit.MILLISECONDS);
		try {
			Runnable await = new Runnable() {
				public void run() {
					try {
						four.countDown();
						latch.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			};
			pool.submit(await);
			pool.submit(await);
			pool.submit(await);
			pool.submit(await);
			assertTrue(four.await(10, TimeUnit.SECONDS));
		} finally {
			latch.countDown();
			pool.shutdownNow();
		}
	}

	public void testSubmitResult() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final CountDownLatch four = new CountDownLatch(4);
		AntiDeadlockThreadPool pool = new AntiDeadlockThreadPool(3, 100,
				new ArrayBlockingQueue<Runnable>(32), "HttpHandling", 100,
				TimeUnit.MILLISECONDS);
		try {
			Runnable await = new Runnable() {
				public void run() {
					try {
						four.countDown();
						latch.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
			};
			pool.submit(await, Boolean.TRUE);
			pool.submit(await, Boolean.TRUE);
			pool.submit(await, Boolean.TRUE);
			pool.submit(await, Boolean.TRUE);
			assertTrue(four.await(10, TimeUnit.SECONDS));
		} finally {
			latch.countDown();
			pool.shutdownNow();
		}
	}

	public void testSubmitCallable() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		final CountDownLatch four = new CountDownLatch(4);
		AntiDeadlockThreadPool pool = new AntiDeadlockThreadPool(3, 100,
				new ArrayBlockingQueue<Runnable>(32), "HttpHandling", 100,
				TimeUnit.MILLISECONDS);
		try {
			Callable<Void> await = new Callable<Void>() {
				public Void call() {
					try {
						four.countDown();
						latch.await();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					return null;
				}
			};
			pool.submit(await);
			pool.submit(await);
			pool.submit(await);
			pool.submit(await);
			assertTrue(four.await(10, TimeUnit.SECONDS));
		} finally {
			latch.countDown();
			pool.shutdownNow();
		}
	}
}
