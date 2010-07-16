package org.callimachusproject.logging;

public interface LoggerMXBean {

	void startNotifications(String prefix);

	void stopNotifications();

	void logAll(String prefix);

	void logInfo(String prefix);

	void logWarn(String prefix);
}
