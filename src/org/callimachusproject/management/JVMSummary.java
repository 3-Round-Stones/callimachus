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
package org.callimachusproject.management;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

public class JVMSummary implements JVMSummaryMXBean {

	public String[] showVMSummary() throws Exception {
		int i = 0;
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
		w.println("Classes loaded:\t" + mx.getLoadedClassCount());
		w.println("Total loaded:\t" + mx.getTotalLoadedClassCount());
		return sw.toString();
	}

	private String getOSUsage() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter w = new PrintWriter(sw, true);
		OperatingSystemMXBean mx = ManagementFactory.getOperatingSystemMXBean();
		w.print("Name:\t");
		w.println(mx.getName());
		w.print("Arch:\t");
		w.println(mx.getArch());
		w.print("Version:\t");
		w.println(mx.getVersion());
		w.print("Load average:\t");
		w.println(mx.getSystemLoadAverage());
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
			w.print((int) (root.getTotalSpace() / 1024 / 1024));
			w.println("m");
			w.print("Free:\t");
			w.print((int) (root.getFreeSpace() / 1024 / 1024));
			w.println("m");
			w.print("Usable:\t");
			w.print((int) (root.getUsableSpace() / 1024 / 1024));
			w.println("m");
		}
		return sw.toString();
	}

	private String getSystemProperties() {
		StringWriter sw = new StringWriter();
		PrintWriter w = new PrintWriter(sw, true);
		Properties sysProps = System.getProperties();
		ArrayList<String> keyList = new ArrayList<String>(
				sysProps.stringPropertyNames());
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
