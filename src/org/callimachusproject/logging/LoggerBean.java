package org.callimachusproject.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;

public class LoggerBean extends NotificationBroadcasterSupport implements
		LoggerMXBean {
	private Formatter formatter;
	private Handler nh;
	private Handler ch;

	private final static class NotificationFormatter extends Formatter {
		public String format(LogRecord record) {
			StringBuffer sb = new StringBuffer();
			String message = formatMessage(record);
			sb.append(record.getLevel().getLocalizedName());
			sb.append(": ");
			sb.append(message);
			sb.append("\r\n");
			if (record.getThrown() != null) {
				try {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					record.getThrown().printStackTrace(pw);
					pw.close();
					sb.append(sw.toString());
				} catch (Exception ex) {
				}
			}
			return sb.toString();
		}
	}

	public final class NotificationHandler extends Handler {

		@Override
		public void publish(LogRecord record) {
			String type = record.getLevel().toString();
			String source = record.getLoggerName();
			long sequenceNumber = record.getSequenceNumber();
			long timeStamp = record.getMillis();
			if (source.startsWith("javax.management") || source.startsWith("sun.rmi"))
				return; // recursive
			String message = getFormatter().formatMessage(record);
			Notification note = new Notification(type, source, sequenceNumber,
					timeStamp, message);
			if (record.getThrown() != null) {
				try {
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					record.getThrown().printStackTrace(pw);
					pw.close();
					note.setUserData(sw.toString());
				} catch (Exception ex) {
				}
			}
			sendNotification(note);
		}

		@Override
		public void close() throws SecurityException {
			// no-op
		}

		@Override
		public void flush() {
			// no-op
		}

	}

	public LoggerBean() {
		formatter = new NotificationFormatter();
		nh = new NotificationHandler();
		nh.setFormatter(formatter);
		nh.setLevel(Level.ALL);
		ch = new ConsoleHandler();
		ch.setFormatter(formatter);
		ch.setLevel(Level.ALL);
	}

	@Override
	public void startConsole(String fragment) {
		boolean found = false;
		Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (name.contains(fragment)) {
				Logger logger = Logger.getLogger(name);
				logger.removeHandler(ch);
				logger.addHandler(ch);
				found = true;
			}
		}
		if (!found)
			throw new IllegalArgumentException("Not such logger");
	}

	@Override
	public void stopConsole() {
		Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			Logger.getLogger(name).removeHandler(ch);
		}
	}

	@Override
	public void startNotifications(String fragment) {
		boolean found = false;
		Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (name.contains(fragment)) {
				Logger logger = Logger.getLogger(name);
				logger.removeHandler(nh);
				logger.addHandler(nh);
				found = true;
			}
		}
		if (!found)
			throw new IllegalArgumentException("Not such logger");
	}

	@Override
	public void stopNotifications() {
		Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			Logger.getLogger(name).removeHandler(nh);
		}
	}

	@Override
	public void logAll(String fragment) {
		boolean found = false;
		Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (name.contains(fragment)) {
				Logger.getLogger(name).setLevel(Level.ALL);
				found = true;
			}
		}
		if (!found)
			throw new IllegalArgumentException("Not such logger");
	}

	@Override
	public void logInfo(String fragment) {
		boolean found = false;
		Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (name.contains(fragment)) {
				Logger.getLogger(name).setLevel(Level.INFO);
				found = true;
			}
		}
		if (!found)
			throw new IllegalArgumentException("Not such logger");
	}

}
