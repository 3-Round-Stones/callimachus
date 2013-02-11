package org.callimachusproject.concurrent;

public interface ManagedThreadPoolListener {

	void threadPoolStarted(String name, ManagedThreadPool pool);

	void threadPoolTerminated(String name);

}
