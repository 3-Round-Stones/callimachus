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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.rmi.UnmarshalException;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.TimeZone;
import java.util.concurrent.ThreadFactory;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.datatype.DatatypeFactory;

import org.callimachusproject.cli.Command;
import org.callimachusproject.cli.CommandSet;
import org.callimachusproject.logging.CalliLogger;
import org.callimachusproject.logging.CalliLoggerMXBean;
import org.callimachusproject.server.CallimachusServer;
import org.callimachusproject.server.HTTPObjectAgentMXBean;
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
	public static final String NAME = Version.getInstance().getVersion();
	private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
	private static final ThreadFactory tfactory = new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "Callimachus-Configure-Setup-Queue" + Integer.toHexString(r.hashCode()));
			t.setDaemon(true);
			return t;
		}
	};

	private static final CommandSet commands = new CommandSet(NAME);
	static {
		commands.require("pid").arg("file").desc(
				"File to read the server process id to monitor");
		commands.option("dump").arg("directory").desc(
				"Use the directory to dump the server status in the given directory");
		commands.option("reset").desc("Empty any cache on the server");
		commands.option("log").optional("name").desc(
				"Print log statements from loggers with these names");
		commands.option("stop").desc(
				"Use the PID file to shutdown the server");
		commands.option("h", "help").desc(
				"Print help (this message) and exit");
		commands.option("v", "version").desc(
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
					} catch (Throwable e) {
						println(e);
					}
				}
			}));
			synchronized (monitor) {
				monitor.wait();
			}
		} catch (ClassNotFoundException e) {
			System.err.print("Missing jar with: ");
			System.err.println(e.toString());
			System.exit(5);
		} catch (Throwable e) {
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
	private MBeanServerConnection mbsc;
	private CalliLoggerMXBean logger;
	private HTTPObjectAgentMXBean server;
	private boolean reset;
	private boolean stop;
	private String dump;
	private String[] log;
	private NotificationListener listener;

	public ServerMonitor() {
		super();
	}

	public ServerMonitor(String pidFile) throws Throwable {
		setPidFile(pidFile);
	}

	public void init(String[] args) {
		try {
			Command line = commands.parse(args);
			if (line.isParseError()) {
				line.printParseError();
				System.exit(2);
				return;
			} else if (line.has("help")) {
				line.printHelp();
				System.exit(0);
				return;
			} else if (line.has("version")) {
				line.printCommandName();
				System.exit(0);
				return;
			} else {
				if (line.has("dump")) {
					dump = line.get("dump") + File.separatorChar;
				}
				if (line.has("log")) {
					log = line.getAll("log");
					if (log == null || log.length == 0) {
						log = new String[]{""};
					}
				}
				reset = line.has("reset");
				stop = line.has("stop");
				String pid = line.get("pid");
				setPidFile(pid);
			}
		} catch (Throwable e) {
			println(e);
			System.err.println("Arguments: " + Arrays.toString(args));
			System.exit(1);
		}
	}

	public void start() throws Throwable {
		if (dump != null) {
			dumpService(dump);
		}
		if (reset) {
			resetCache();
		}
		if (stop) {
			destroyService();
		}
		if (log == null) {
			System.exit(0);
		} else {
			logNotifications(log);
		}
	}

	public void stop() throws Throwable {
		try {
			if (listener != null) {
				mbsc.removeNotificationListener(getMXLoggerName(), listener);
				logger.stopNotifications();
			}
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	public void destroy() throws Exception {
		// nothing to destroy
	}

	public void logNotifications(String[] log) throws Throwable {
		listener = new NotificationListener() {
			public void handleNotification(Notification note, Object handback) {
				synchronized (this) {
					System.out.println(note.getMessage());
					Object userData = note.getUserData();
					if (userData != null) {
						System.out.println(userData);
					}
				}
			}
		};
		try {
			mbsc.addNotificationListener(getMXLoggerName(), listener, null, null);
			for (String name : log) {
				logger.startNotifications(name);
			}
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	public boolean destroyService() throws Throwable {
		try {
			if (!server.isRunning())
				return false;
			try {
				try {
					server.stop();
				} finally {
					server.destroy();
				}
				info("Callimachus server has stopped");
				return true;
			} catch (UnmarshalException e) {
				if (!(e.getCause() instanceof IOException))
					throw e;
				// remote JVM has terminated
				info("Callimachus server has shutdown");
				return true;
			}
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	public void resetCache() throws Throwable {
		try {
			server.resetCache();
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	public void dumpService(String dir) throws Throwable {
		GregorianCalendar now = new GregorianCalendar(
				TimeZone.getTimeZone("UTC"));
		DatatypeFactory df = DatatypeFactory.newInstance();
		String stamp = df.newXMLGregorianCalendar(now).toXMLFormat();
		stamp = stamp.replaceAll("[^0-9]", "");
		// execute remote VM command
		executeVMCommand(vm, "remoteDataDump", dir + "threads-" + stamp
				+ ".tdump");
		heapDump(vm, dir + "heap-" + stamp + ".hprof");
		executeVMCommand(vm, "heapHisto", dir + "heap-" + stamp + ".histo");
		// dump callimachus info
		connectionDump(mbsc, dir + "server-" + stamp + ".csv");
		poolDump(mbsc, dir + "pool-" + stamp + ".tdump");
		traceDump(mbsc, dir + "trace-" + stamp + ".txt");
		summaryDump(mbsc, dir + "summary-" + stamp + ".txt");
		netStatistics(dir + "netstat-" + stamp + ".txt");
	}

	private void setPidFile(String pid) throws Throwable {
		vm = getRemoteVirtualMachine(pid);
		mbsc = getMBeanConnection(vm);
		server = JMX.newMXBeanProxy(mbsc, getMXServerName(),
				HTTPObjectAgentMXBean.class);
		logger = JMX
				.newMXBeanProxy(mbsc, getMXLoggerName(), CalliLoggerMXBean.class);
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
		info(hprof);
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
		info(filename);
	}

	private void connectionDump(MBeanServerConnection mbsc, String filename)
			throws MalformedObjectNameException, IOException {
		HTTPObjectAgentMXBean server = JMX.newMXBeanProxy(mbsc,
				getMXServerName(), HTTPObjectAgentMXBean.class);
		server.connectionDumpToFile(filename);
		info(filename);
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
		info(filename);
	}

	private void traceDump(MBeanServerConnection mbsc2, String filename) throws IOException {
		logger.traceDumpToFile(filename);
		info(filename);
	}

	private void summaryDump(MBeanServerConnection mbsc, String filename)
			throws Throwable {
		try {
			String[] summary = logger.showVMSummary();
			PrintWriter w = new PrintWriter(filename);
			try {
				for (String line : summary) {
					w.println(line);
				}
			} finally {
				w.close();
			}
			info(filename);
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	private Object getRemoteVirtualMachine(String pidFile)
			throws Throwable {
		Class<?> VM = Class.forName("com.sun.tools.attach.VirtualMachine");
		Method attach = VM.getDeclaredMethod("attach", String.class);
		String pid = IOUtil.readString(new File(pidFile)).trim();
		// attach to the target application
		info("Connecting to " + pid);
		try {
			return attach.invoke(null, pid);
		} catch (InvocationTargetException e) {
			throw e.getCause();
		}
	}

	private void netStatistics(String fileName) throws IOException {
		FileOutputStream out = new FileOutputStream(fileName);
		try {
			// Not all processes could be identified
			if (exec(out, new ByteArrayOutputStream(), "netstat", "-tnpo")) {
				exec(out, System.err, "netstat", "-st");
				info(fileName);
			} else {
				new File(fileName).delete();
			}
		} catch (IOException e) {
			// netstat not installed
			new File(fileName).delete();
		} finally {
			out.close();
		}
	}

	private boolean exec(OutputStream stdout, OutputStream stderr, String... command) throws IOException {
		ProcessBuilder process = new ProcessBuilder(command);
		Process p = process.start();
		Thread tin = transfer(p.getInputStream(), stdout);
		Thread terr = transfer(p.getErrorStream(), stderr);
		p.getOutputStream().close();
		try {
			int ret = p.waitFor();
			if (tin != null) {
				tin.join();
			}
			if (terr != null) {
				terr.join();
			}
			return ret == 0;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private Thread transfer(final InputStream in, final OutputStream out) {
		if (in == null)
			return null;
		Thread thread = tfactory.newThread(new Runnable() {
			public void run() {
				try {
					try {
						int read;
						byte[] buf = new byte[1024];
						while ((read = in.read(buf)) >= 0) {
							out.write(buf, 0, read);
						}
					} finally {
						in.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
		thread.start();
		return thread;
	}

	private void info(String message) {
		System.err.println(message);
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
		return new ObjectName(pkg + ":type=" + CallimachusServer.class.getSimpleName());
	}

	private ObjectName getMXLoggerName() throws MalformedObjectNameException {
		String pkg = Server.class.getPackage().getName();
		return new ObjectName(pkg + ":type=" + CalliLogger.class.getSimpleName());
	}

}
