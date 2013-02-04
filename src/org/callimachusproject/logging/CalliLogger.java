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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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

import org.callimachusproject.logging.trace.MethodCall;
import org.callimachusproject.logging.trace.Trace;
import org.callimachusproject.logging.trace.TraceAnalyser;
import org.slf4j.LoggerFactory;

import com.sun.management.OperatingSystemMXBean;

/**
 * MXBean to control jdk logging using a simple interface.
 * 
 * @author James Leigh
 * 
 */
public class CalliLogger extends NotificationBroadcasterSupport implements
		CalliLoggerMBean {
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

	public CalliLogger() {
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
		setLoggerLevel(fragment, Level.ALL);
	}

	@Override
	public void logInfo(String fragment) {
		setLoggerLevel(fragment, Level.INFO);
	}

	@Override
	public void logWarn(String fragment) {
		setLoggerLevel(fragment, Level.WARNING);
	}

	public synchronized String[] getLoggingProperties() throws IOException {
		String fileName = System.getProperty("java.util.logging.config.file");
		if (fileName == null || !new File(fileName).exists())
			return new String[0];
		FileReader fileReader = new FileReader(fileName);
		BufferedReader bufferedReader = new BufferedReader(fileReader);
		List<String> lines = new ArrayList<String>();
		String line = null;
		while ((line = bufferedReader.readLine()) != null) {
		    lines.add(line);
		}
		bufferedReader.close();
		return lines.toArray(new String[lines.size()]);
	}

	public synchronized void setLoggingProperties(String[] lines)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintWriter writer = new PrintWriter(out);
		try {
			for (String line : lines) {
				writer.println(line);
			}
		} finally {
			writer.close();
		}
		byte[] data = out.toByteArray();
		LogManager.getLogManager().readConfiguration(new ByteArrayInputStream(data));
		String fileName = System.getProperty("java.util.logging.config.file");
		if (fileName != null) {
			FileOutputStream stream = new FileOutputStream(fileName);
			try {
				stream.write(data);
			} finally {
				stream.close();
			}
		}
	}

    public String[] showVMSummary() throws Exception {
    	int i=0;
    	String[] result = new String[8];
		result[i++] = getJVMVersion();
		result[i++] = getOSUsage();
		result[i++] = getMemoryUsage();
		result[i++] = getRuntimeUsage();
		result[i++] = getClassUsage();
		result[i++] = getFileSystemUsage();
		result[i++] = getSystemProperties();
		result[i++] = getVariables();
		return result;
	}

	@Override
	public String[] traceActiveCalls() {
		Trace[] threads = MethodCall.getActiveCallTraces();
		String[] result = new String[threads.length];
		for (int i=0; i<threads.length; i++) {
			StringWriter sw = new StringWriter();
			PrintWriter w = new PrintWriter(sw);
		
			Trace call = threads[i];
			print(call, w);
			w.flush();
			result[i] = sw.toString();
		}
		return result;
	}

	@Override
	public void resetTraceAnalyser() {
		TraceAnalyser analyser = TraceAnalyser.getInstance();
		analyser.reset();
	}

	@Override
	public void traceDumpToFile(String outputFile) throws IOException {
		TraceAnalyser analyser = TraceAnalyser.getInstance();
		Trace[] traces1 = MethodCall.getActiveCallTraces();
		Trace[] traces2 = analyser.getTracesByAverageTime();
		Trace[] traces3 = analyser.getTracesByTotalTime();
		Set<Trace> set = new LinkedHashSet<Trace>(traces1.length + traces2.length + traces3.length);
		addEach(traces1, set);
		addEach(traces2, set);
		addEach(traces3, set);
		PrintWriter writer = new PrintWriter(new FileWriter(outputFile, true));
		try {
			for (Trace trace : set) {
				print(trace, writer);
				writer.println();
			}
			writer.println();
			writer.println();
		} finally {
			writer.close();
		}
		LoggerFactory.getLogger(CalliLogger.class).info("Call trace dump: {}", outputFile);
	}

	private void addEach(Trace[] traces, Set<Trace> set) {
		set.addAll(Arrays.asList(traces));
		for (Trace call : traces) {
			Trace parent = call;
			while ((parent = parent.getPreviousTrace()) != null) {
				set.remove(parent);
			}
		}
	}

	private static void print(Trace call, PrintWriter w) {
		if (call.getPreviousTrace() != null) {
			print(call.getPreviousTrace(), w);
		}
		for (String assign : call.getAssignments()) {
			w.println(assign);
		}
		w.println(call.toString());
	}

	private void setLoggerLevel(String fragment, Level level) {
		boolean found = false;
		Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (name.contains(fragment)) {
				Logger logger = Logger.getLogger(name);
				logger.setLevel(level);
				setHandlerLevel(logger, level);
				found = true;
			}
		}
		if (!found)
			throw new IllegalArgumentException("Not such logger");
	}

	private void setHandlerLevel(Logger logger, Level level) {
		if (logger.getParent() != null) {
			setHandlerLevel(logger.getParent(), level);
		}
		Handler[] handlers = logger.getHandlers();
		if (handlers != null) {
			for (Handler handler : handlers) {
				if (handler.getLevel().intValue() > level.intValue()) {
					handler.setLevel(level);
				}
			}
		}
	}

	private String getJVMVersion() {
		StringWriter sw = new StringWriter();
		PrintWriter w = new PrintWriter(sw, true);
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
		w.print("Server:\t");
		w.println(org.callimachusproject.Version.getInstance().getVersion());
		w.print("User:\t");
		w.println(System.getProperty("user.name"));
		return sw.toString();
	}

	private String getRuntimeUsage() throws DatatypeConfigurationException {
		StringWriter sw = new StringWriter();
		PrintWriter w = new PrintWriter(sw, true);
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
		return sw.toString();
	}

	private String getClassUsage() {
		StringWriter sw = new StringWriter();
		PrintWriter w = new PrintWriter(sw, true);
		ClassLoadingMXBean mx = ManagementFactory.getClassLoadingMXBean();
		w.println("Classes loaded:\t" +mx.getLoadedClassCount());
		w.println("Total loaded:\t" + mx.getTotalLoadedClassCount());
		return sw.toString();
	}

	private String getOSUsage() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter w = new PrintWriter(sw, true);
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
		return sw.toString();
	}

	private String getMemoryUsage() {
		StringWriter sw = new StringWriter();
		PrintWriter w = new PrintWriter(sw, true);
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
		return sw.toString();
	}

	private String getFileSystemUsage() {
		StringWriter sw = new StringWriter();
		PrintWriter w = new PrintWriter(sw, true);
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
		return sw.toString();
	}

	private String getSystemProperties() {
		StringWriter sw = new StringWriter();
		PrintWriter w = new PrintWriter(sw, true);
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
		return sw.toString();
	}

	private String getVariables() {
		StringWriter sw = new StringWriter();
		PrintWriter w = new PrintWriter(sw, true);
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
		return sw.toString();
	}

}
