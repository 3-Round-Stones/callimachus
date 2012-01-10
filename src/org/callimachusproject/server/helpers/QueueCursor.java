/*
 * Copyright Aduna (http://www.aduna-software.com/) (c) 2008.
 *
 * Licensed under the Aduna BSD-style license.
 */
package org.callimachusproject.server.helpers;

import info.aduna.iteration.LookAheadIteration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.openrdf.query.QueryEvaluationException;

/**
 * Makes working with a queue easier by adding the methods {@link #done()} and
 * {@link #toss(Exception)} and automatically converting the exception into a
 * StoreException with an appropriate stack trace.
 * 
 * @author James Leigh
 */
public class QueueCursor<E> extends LookAheadIteration<E, QueryEvaluationException> {

	private volatile boolean done;

	private BlockingQueue<E> queue;

	private E afterLast = createAfterLast();

	private volatile Queue<Throwable> exceptions = new LinkedList<Throwable>();

	/**
	 * Creates an <tt>QueueCursor</tt> with the given (fixed) capacity and
	 * default access policy.
	 * 
	 * @param capacity
	 *        the capacity of this queue
	 */
	public QueueCursor(int capacity) {
		this(capacity, false);
	}

	/**
	 * Creates an <tt>QueueCursor</tt> with the given (fixed) capacity and the
	 * specified access policy.
	 * 
	 * @param capacity
	 *        the capacity of this queue
	 * @param fair
	 *        if <tt>true</tt> then queue accesses for threads blocked on
	 *        insertion or removal, are processed in FIFO order; if
	 *        <tt>false</tt> the access order is unspecified.
	 */
	public QueueCursor(int capacity, boolean fair) {
		this.queue = new ArrayBlockingQueue<E>(capacity, fair);
	}

	/**
	 * The next time {@link #next()} is called this exception will be thrown. If
	 * it is not a StoreException or RuntimeException it will be wrapped in a
	 * StoreException.
	 */
	public void toss(Exception exception) {
		synchronized (exceptions) {
			exceptions.add(exception);
		}
	}

	/**
	 * Adds another item to the queue, blocking while the queue is full.
	 */
	public void put(E st)
		throws InterruptedException
	{
		queue.put(st);
	}

	/**
	 * Indicates the method {@link #put(Object)} will not be called in the queue
	 * anymore.
	 */
	public void done() {
		done = true;
		try {
			queue.add(afterLast);
		}
		catch (IllegalStateException e) {
			// no thread is waiting on this queue anyway
		}
	}

	/**
	 * Returns the next item in the queue or throws an exception.
	 */
	@Override
	public E getNextElement()
		throws QueryEvaluationException
	{
		try {
			checkException();
			E take;
			if (done) {
				take = queue.poll();
			}
			else {
				take = queue.take();
				if (done) {
					done(); // in case the queue was full before
				}
			}
			if (isAfterLast(take)) {
				checkException();
				done(); // put afterLast back for others
				return null;
			}
			return take;
		}
		catch (InterruptedException e) {
			checkException();
			throw new QueryEvaluationException(e);
		}
	}

	@Override
	public void handleClose()
		throws QueryEvaluationException
	{
		checkException();
	}

	public void checkException()
		throws QueryEvaluationException
	{
		synchronized (exceptions) {
			if (!exceptions.isEmpty()) {
				try {
					throw exceptions.remove();
				}
				catch (QueryEvaluationException e) {
					List<StackTraceElement> stack = new ArrayList<StackTraceElement>();
					stack.addAll(Arrays.asList(e.getStackTrace()));
					StackTraceElement[] thisStack = new Throwable().getStackTrace();
					stack.addAll(Arrays.asList(thisStack).subList(1, thisStack.length));
					e.setStackTrace(stack.toArray(new StackTraceElement[stack.size()]));
					throw e;
				}
				catch (RuntimeException e) {
					List<StackTraceElement> stack = new ArrayList<StackTraceElement>();
					stack.addAll(Arrays.asList(e.getStackTrace()));
					StackTraceElement[] thisStack = new Throwable().getStackTrace();
					stack.addAll(Arrays.asList(thisStack));
					e.setStackTrace(stack.toArray(new StackTraceElement[stack.size()]));
					throw e;
				}
				catch (Throwable e) {
					throw new QueryEvaluationException(e);
				}
			}
		}
	}

	private boolean isAfterLast(E take) {
		return take == null || take == afterLast;
	}

	@SuppressWarnings("unchecked")
	private E createAfterLast() {
		return (E)new Object();
	}

}