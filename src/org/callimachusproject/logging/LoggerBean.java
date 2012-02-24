/*
 * Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some
   Rights Reserved
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved 
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
package org.callimachusproject.logging;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import com.sun.management.OperatingSystemMXBean;

/**
 * MXBean to control jdk logging using a simple interface.
 * 
 * @author James Leigh
 * 
 */
public class LoggerBean extends NotificationBroadcasterSupport implements
		LoggerMXBean {
	private Formatter formatter;
	private Handler nh;

	public final class NotificationHandler extends Handler {

		@Override
		public void publish(LogRecord record) {
			String type = record.getLevel().toString();
			String source = record.getLoggerName();
			long sequenceNumber = record.getSequenceNumber();
			long timeStamp = record.getMillis();
			if (source.startsWith("javax.management")
					|| source.startsWith("sun.rmi"))
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
		formatter = new LogMessageFormatter();
		nh = new NotificationHandler();
		nh.setFormatter(formatter);
		nh.setLevel(Level.ALL);
	}

	@Override
	public void startNotifications(String loggerName) {
		Logger logger = LogManager.getLogManager().getLogger(loggerName);
		if (logger == null)
			throw new IllegalArgumentException("Not such logger");
		logger.removeHandler(nh);
		logger.addHandler(nh);
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

	@Override
	public void logWarn(String fragment) {
		boolean found = false;
		Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (name.contains(fragment)) {
				Logger.getLogger(name).setLevel(Level.WARNING);
				found = true;
			}
		}
		if (!found)
			throw new IllegalArgumentException("Not such logger");
	}

	public String getVMSummary() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter w = new PrintWriter(sw);
		printJVMVersion(w);
		printOSUsage(w);
		printMemoryUsage(w);
		printRuntimeUsage(w);
		printClassUsage(w);
		printFileSystemUsage(w);
		printSystemProperties(w);
		printVariables(w);
		w.flush();
		return sw.toString();
	}

	private void printJVMVersion(PrintWriter w) {
		w.print("OS:\t");
		w.print(System.getProperty("os.name"));
		w.print(" ");
		w.print(System.getProperty("os.version"));
		w.print(" (");
		w.print(System.getProperty("os.arch"));
		w.println(")");
		w.print("VM:\t");
		w.print(System.getProperty("java.vendor"));
		w.print(" ");
		w.print(System.getProperty("java.vm.name"));
		w.print(" ");
		w.println(System.getProperty("java.version"));
		w.print("Callimachus:\t");
		w.println(org.callimachusproject.Version.getVersion());
		w.print("User:\t");
		w.println(System.getProperty("user.name"));
		w.println();
	}

	private void printRuntimeUsage(PrintWriter w) throws DatatypeConfigurationException {
		RuntimeMXBean mx = ManagementFactory.getRuntimeMXBean();
		Date starttime = new Date(mx.getStartTime());
		GregorianCalendar gcal = new GregorianCalendar();
		gcal.setTime(starttime);
		DatatypeFactory df = DatatypeFactory.newInstance();
		String date = df.newXMLGregorianCalendar(gcal).toXMLFormat();
		w.println("VM start time:\t" + date);
		w.println("VM up time:\t" + mx.getUptime() + " ms");
		w.print("Available processors (cores):\t"); 
	    w.println(Runtime.getRuntime().availableProcessors());
		// the input arguments passed to the Java virtual machine
		// which does not include the arguments to the main method.
		w.println("JVM arguments:\n" + mx.getInputArguments());
		w.println("Boot class path:\n" + mx.getBootClassPath());
		w.println("Class path:\n" + mx.getClassPath());
		w.println();
	}

	private void printClassUsage(PrintWriter w) {
		ClassLoadingMXBean mx = ManagementFactory.getClassLoadingMXBean();
		w.println("Classes loaded:\t" +mx.getLoadedClassCount());
		w.println("Total loaded:\t" + mx.getTotalLoadedClassCount());
		w.println();
	}

	private void printOSUsage(PrintWriter w) throws Exception {
		OperatingSystemMXBean mx = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		w.print("Name:\t");
		w.println(mx.getName());
		w.print("Arch:\t");
		w.println(mx.getArch());
		w.print("Version:\t");
		w.println(mx.getVersion());
		w.print("Load average:\t");
		w.println(mx.getSystemLoadAverage());
		w.print("Committed memory:\t");
		w.println((mx.getCommittedVirtualMemorySize() / 1024 / 1024) + "m");
		w.print("Swap size:\t");
		w.println((mx.getTotalSwapSpaceSize() / 1024 / 1024) + "m");
		w.print("Free swap size:\t");
		w.println((mx.getFreeSwapSpaceSize() / 1024 / 1024) + "m");
		w.print("Free memory:\t");
		w.println((mx.getFreePhysicalMemorySize() / 1024 / 1024) + "m");
		w.print("Total memory:\t");
		w.println((mx.getTotalPhysicalMemorySize() / 1024 / 1024) + "m");
		w.print("Process time:\t");
		long nanoseconds = mx.getProcessCpuTime();
		long seconds = TimeUnit.SECONDS.convert(nanoseconds, TimeUnit.NANOSECONDS);
		long minutes = TimeUnit.MINUTES.convert(nanoseconds, TimeUnit.NANOSECONDS);
		long hours = TimeUnit.HOURS.convert(nanoseconds, TimeUnit.NANOSECONDS);
		w.print(hours);
		w.print(":");
		w.print(minutes - hours * 60);
		w.print(":");
		w.print(seconds - minutes * 60);
		w.println();
		w.println();
	}

	private void printMemoryUsage(PrintWriter w) {
		MemoryMXBean mx = ManagementFactory.getMemoryMXBean();
		w.print("Memory used:\t");
		Runtime runtime = Runtime.getRuntime();
		long usedMemory = runtime.totalMemory() - runtime.freeMemory();
		long maxMemory = runtime.maxMemory();

		// Memory usage (percentage)
		w.print(usedMemory * 100 / maxMemory);
		w.println("%");

		// Memory usage in MB
		w.print("Used:\t");
		w.print((int) (usedMemory / 1024 / 1024));
		w.println("m");
		w.print("Allocated:\t");
		w.print((int) (maxMemory / 1024 / 1024));
		w.println("m");
		w.print("Pending finalization:\t");
		w.println(mx.getObjectPendingFinalizationCount());
		w.println();
	}

	private void printFileSystemUsage(PrintWriter w) {
	    /* Get a list of all filesystem roots on this system */
	    File[] roots = File.listRoots();

	    /* For each filesystem root, print some info */
	    for (File root : roots) {
	      w.print("File system root:\t");
	      w.println(root.getAbsolutePath());
	      w.print("Size:\t");
	      w.print((int)(root.getTotalSpace() / 1024 / 1024));
	      w.println("m");
	      w.print("Free:\t");
	      w.print((int)(root.getFreeSpace() / 1024 / 1024));
	      w.println("m");
	      w.print("Usable:\t");
	      w.print((int)(root.getUsableSpace() / 1024 / 1024));
	      w.println("m");
	    }
		w.println();
	}

	private void printSystemProperties(PrintWriter w) {
		Properties sysProps = System.getProperties();
		ArrayList<String> keyList = new ArrayList<String>(sysProps.stringPropertyNames());
		Collections.sort(keyList);
		Iterator<String> sysPropNames = keyList.iterator();
		while (sysPropNames.hasNext()) {
			String name = sysPropNames.next();
			w.print(name);
			w.print(":\t");
			w.println(sysProps.get(name));
		}
		w.println();
	}

	private void printVariables(PrintWriter w) {
		Map<String, String> envProps = System.getenv();
		ArrayList<String> keyList = new ArrayList<String>(envProps.keySet());
		Collections.sort(keyList);
		Iterator<String> envPropNames = keyList.iterator();
		while (envPropNames.hasNext()) {
			String name = envPropNames.next();
			w.print(name);
			w.print(":\t");
			w.println(envProps.get(name));
		}
		w.println();
	}

}
