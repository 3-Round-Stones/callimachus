/*
 * Copyright (c) 2012 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject;

import info.aduna.io.IOUtil;

import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.UnmarshalException;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.datatype.DatatypeFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.callimachusproject.logging.LoggerMXBean;
import org.callimachusproject.server.HTTPObjectAgentMXBean;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.server.util.ManagedThreadPool;
import org.callimachusproject.server.util.ThreadPoolMXBean;

/**
 * Command line tool for monitoring the server.
 * 
 * @author James Leigh
 * 
 */
public class ServerMonitor {
	private static final String BRAND = "Callimachus Project Server";
	public static final String NAME = BRAND + '/' + Version.getVersion();
	private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";

	private static final Options options = new Options();
	static {
		options.addOption("pid", true,
				"File to read the server process id to monitor");
		options.getOption("pid").setRequired(true);
		options.addOption("dump", true,
				"Use the directory to dump the server status in the given directory");
		options.addOption("stop", false,
				"Use the PID file to shutdown the server");
		options.addOption("h", "help", false,
				"Print help (this message) and exit");
		options.addOption("v", "version", false,
				"Print version information and exit");
	}

	public static void main(String[] args) {
		try {
			final ServerMonitor monitor = new ServerMonitor();
			monitor.init(args);
			monitor.start();
			Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
				public void run() {
					try {
						monitor.stop();
						monitor.destroy();
					} catch (Exception e) {
						println(e);
					}
				}
			}));
		} catch (ClassNotFoundException e) {
			System.err.print("Missing jar with: ");
			System.err.println(e.toString());
			System.exit(1);
		} catch (Exception e) {
			println(e);
			System.err.println("Arguments: " + Arrays.toString(args));
			System.exit(1);
		}
	}

	private static void println(Throwable e) {
		Throwable cause = e.getCause();
		if (cause == null && e.getMessage() == null) {
			e.printStackTrace(System.err);
		} else if (cause != null) {
			println(cause);
		}
		System.err.println(e.toString());
	}

	private Object vm;
	private HTTPObjectAgentMXBean server;
	private boolean stop;
	private String dump;
	private MBeanServerConnection mbsc;

	public void init(String[] args) {
		try {
			CommandLine line = new GnuParser().parse(options, args);
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("[options]", options);
				System.exit(0);
				return;
			} else if (line.hasOption('v')) {
				System.out.println(NAME);
				System.exit(0);
				return;
			} else if (line.getArgs().length > 0) {
				System.err.println("Unrecognized option: "
						+ Arrays.toString(line.getArgs()));
				System.err.println("Arguments: " + Arrays.toString(args));
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("[options]", options);
				System.exit(0);
				return;
			} else if (!line.hasOption("pid")) {
				System.err.println("Missing pid option");
				System.err.println("Arguments: " + Arrays.toString(args));
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("[options]", options);
				System.exit(0);
			} else {
				String pid = line.getOptionValue("pid");
				vm = getRemoteVirtualMachine(pid);
				mbsc = getMBeanConnection(vm);
				server = JMX.newMXBeanProxy(mbsc, getMXServerName(),
						HTTPObjectAgentMXBean.class);
				if (line.hasOption("dump")) {
					dump = line.getOptionValue("dump") + File.separatorChar;
				}
				stop = line.hasOption("stop");
			}
		} catch (Exception e) {
			println(e);
			System.err.println("Arguments: " + Arrays.toString(args));
			System.exit(2);
		}
	}

	public void start() throws Exception {
		if (dump != null) {
			dumpService(dump);
		}
		if (stop) {
			destroyService();
		}
		System.exit(0);
	}

	public void stop() throws Exception {
		// nothing to stop
	}

	public void destroy() throws Exception {
		// nothing to destroy
	}

	private void destroyService() throws Exception {
		try {
			try {
				server.stop();
			} finally {
				server.destroy();
			}
			System.out.println("Callimachus server has stopped");
		} catch (UnmarshalException e) {
			if (!(e.getCause() instanceof EOFException))
				throw e;
			// remote JVM has terminated
			System.out.println("Callimachus server has shutdown");
		}
	}

	private void dumpService(String dir) throws Exception {
		GregorianCalendar now = new GregorianCalendar(
				TimeZone.getTimeZone("UTC"));
		DatatypeFactory df = DatatypeFactory.newInstance();
		String stamp = df.newXMLGregorianCalendar(now).toXMLFormat();
		// execute remote VM command
		executeVMCommand(vm, "remoteDataDump", dir + "threads-" + stamp
				+ ".tdump");
		heapDump(vm, dir + "heap-" + stamp + ".hprof");
		executeVMCommand(vm, "heapHisto", dir + "heap-" + stamp + ".histo");
		// dump callimachus info
		connectionDump(mbsc, dir + "server-" + stamp + ".csv");
		clientDump(mbsc, dir + "client-" + stamp + ".csv");
		poolDump(mbsc, dir + "pool-" + stamp + ".tdump");
		summaryDump(mbsc, dir + "summary-" + stamp + ".txt");
	}

	private void heapDump(Object vm, String hprof) throws Exception {
		String[] args = { hprof };
		Method remoteDataDump = vm.getClass().getMethod("dumpHeap",
				Object[].class);
		InputStream in = (InputStream) remoteDataDump.invoke(vm,
				new Object[] { args });
		try {
			ChannelUtil.transfer(in, System.out);
		} finally {
			in.close();
		}
		System.out.println(hprof);
	}

	private void executeVMCommand(Object vm, String cmd, String filename,
			String... args) throws Exception {
		Method remoteDataDump = vm.getClass().getMethod(cmd, Object[].class);
		InputStream in = (InputStream) remoteDataDump.invoke(vm,
				new Object[] { args });
		try {
			FileOutputStream out = new FileOutputStream(filename);
			try {
				ChannelUtil.transfer(in, out);
			} finally {
				out.close();
			}
		} finally {
			in.close();
		}
		System.out.println(filename);
	}

	private void connectionDump(MBeanServerConnection mbsc, String filename)
			throws MalformedObjectNameException, IOException {
		HTTPObjectAgentMXBean server = JMX.newMXBeanProxy(mbsc,
				getMXServerName(), HTTPObjectAgentMXBean.class);
		server.connectionDumpToFile(filename);
		System.out.println(filename);
	}

	private void poolDump(MBeanServerConnection mbsc, String filename)
			throws MalformedObjectNameException, IOException {
		ObjectName mtpp = ManagedThreadPool.getObjectNamePattern();
		ObjectName[] mons = mbsc.queryNames(mtpp, null).toArray(
				new ObjectName[0]);
		for (int i = 0; i < mons.length; i++) {
			ThreadPoolMXBean pool = JMX.newMXBeanProxy(mbsc, mons[i],
					ThreadPoolMXBean.class);
			pool.threadDumpToFile(filename);
		}
		System.out.println(filename);
	}

	private void clientDump(MBeanServerConnection mbsc, String filename)
			throws MalformedObjectNameException, IOException {
		ObjectName conp = HTTPObjectClient.getObjectNamePattern();
		ObjectName[] cons = mbsc.queryNames(conp, null).toArray(
				new ObjectName[0]);
		for (int i = 0; i < cons.length; i++) {
			HTTPObjectAgentMXBean client = JMX.newMXBeanProxy(mbsc, cons[i],
					HTTPObjectAgentMXBean.class);
			client.connectionDumpToFile(filename);
		}
		System.out.println(filename);
	}

	private void summaryDump(MBeanServerConnection mbsc, String filename)
			throws Exception {
		LoggerMXBean logger = JMX.newMXBeanProxy(mbsc, getMXLoggerName(),
				LoggerMXBean.class);
		String summary = logger.getVMSummary();
		PrintWriter w = new PrintWriter(filename);
		try {
			w.println(summary);
		} finally {
			w.close();
		}
		System.out.println(filename);
	}

	private Object getRemoteVirtualMachine(String pidFile)
			throws ClassNotFoundException, NoSuchMethodException, IOException,
			IllegalAccessException, InvocationTargetException {
		Class<?> VM = Class.forName("com.sun.tools.attach.VirtualMachine");
		Method attach = VM.getDeclaredMethod("attach", String.class);
		String pid = IOUtil.readString(new File(pidFile)).trim();
		// attach to the target application
		return attach.invoke(null, pid);
	}

	private MBeanServerConnection getMBeanConnection(Object vm)
			throws Exception {
		Method getAgentProperties = vm.getClass().getMethod(
				"getAgentProperties");
		Method getSystemProperties = vm.getClass().getMethod(
				"getSystemProperties");
		Method loadAgent = vm.getClass().getMethod("loadAgent", String.class);

		// get the connector address
		Properties properties = (Properties) getAgentProperties.invoke(vm);
		String connectorAddress = properties.getProperty(CONNECTOR_ADDRESS);

		// no connector address, so we start the JMX agent
		if (connectorAddress == null) {
			properties = (Properties) getSystemProperties.invoke(vm);
			String agent = properties.getProperty("java.home") + File.separator
					+ "lib" + File.separator + "management-agent.jar";
			loadAgent.invoke(vm, agent);

			// agent is started, get the connector address
			properties = (Properties) getAgentProperties.invoke(vm);
			connectorAddress = properties.getProperty(CONNECTOR_ADDRESS);
		}

		JMXServiceURL service = new JMXServiceURL(connectorAddress);
		JMXConnector connector = JMXConnectorFactory.connect(service);
		return connector.getMBeanServerConnection();
	}

	private ObjectName getMXServerName() throws MalformedObjectNameException {
		String pkg = Server.class.getPackage().getName();
		return new ObjectName(pkg + ":type="
				+ Server.class.getSimpleName());
	}

	private ObjectName getMXLoggerName() throws MalformedObjectNameException {
		String pkg = Server.class.getPackage().getName();
		return new ObjectName(pkg + ":type=Logger");
	}

}
