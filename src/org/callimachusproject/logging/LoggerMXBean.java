package org.callimachusproject.logging;

public interface LoggerMXBean {

	void startConsole(String prefix);

	void stopConsole();

	void startNotifications(String prefix);

	void stopNotifications();

	void logAll(String prefix);

	void logInfo(String prefix);
}
