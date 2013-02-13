package org.callimachusproject.management;

public interface LogEmitterMXBean {

	void startNotifications(String prefix);

	void stopNotifications();

	void logAll(String prefix);

	void logInfo(String prefix);

	void logWarn(String prefix);

}
