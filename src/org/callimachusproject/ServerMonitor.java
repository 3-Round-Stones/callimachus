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
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ThreadFactory;

import javax.management.InstanceNotFoundException;
import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.xml.datatype.DatatypeFactory;

import org.callimachusproject.cli.Command;
import org.callimachusproject.cli.CommandSet;
import org.callimachusproject.concurrent.ManagedThreadPool;
import org.callimachusproject.concurrent.ThreadPoolMXBean;
import org.callimachusproject.io.ChannelUtil;
import org.callimachusproject.management.CalliServer;
import org.callimachusproject.management.CalliServerMXBean;
import org.callimachusproject.management.JVMSummary;
import org.callimachusproject.management.JVMSummaryMXBean;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.repository.CalliRepositoryMXBean;
import org.callimachusproject.server.WebServer;
import org.callimachusproject.server.WebServerMXBean;

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
		} else {
			e.printStackTrace(System.err);
		}
	}

	private Object vm;
	private MBeanServerConnection mbsc;
	private JVMSummaryMXBean summary;
	private WebServerMXBean webService;
	private CalliServerMXBean server;
	private boolean reset;
	private boolean stop;

	public ServerMonitor() {
		super();
	}

	public ServerMonitor(String pidFile) throws Throwable {
		setPidFile(pidFile);
	}

	public void init(String[] args) {
		try {
			Command line = commands.parse(args);
			if (line.has("help")) {
				line.printHelp();
				System.exit(0);
				return;
			} else if (line.has("version")) {
				line.printCommandName();
				System.exit(0);
				return;
			} else if (line.isParseError()) {
				line.printParseError();
				System.exit(2);
				return;
			} else {
				setPidFile(line.get("pid"));
				reset = line.has("reset");
				stop = line.has("stop");
				if (line.has("dump")) {
					dumpService(line.get("dump") + File.separatorChar);
				}
			}
		} catch (Throwable e) {
			println(e);
			System.err.println("Arguments: " + Arrays.toString(args));
			System.exit(1);
		}
	}

	public void start() throws Throwable {
		if (reset) {
			resetCache();
		}
		if (stop) {
			destroyService();
		}
		System.exit(0);
	}

	public void stop() throws Throwable {
		// nothing to stop
	}

	public void destroy() throws Exception {
		// nothing to destroy
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
			} catch (UndeclaredThrowableException e) {
				if (e.getCause() instanceof InstanceNotFoundException) {
					// remote MBean has unregistered
					info("Callimachus server has been destroyed");
					return true;
				}
				throw e;
			} catch (UnmarshalException e) {
				if (e.getCause() instanceof IOException) {
					// remote JVM has terminated
					info("Callimachus server has shutdown");
					return true;
				}
				throw e;
			}
		} catch (UndeclaredThrowableException e) {
			throw e.getCause();
		}
	}

	public void resetCache() throws Throwable {
		try {
			webService.resetCache();
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
		topStatistics(dir + "top-" + stamp + ".txt");
	}

	private void setPidFile(String pid) throws Throwable {
		vm = getRemoteVirtualMachine(pid);
		mbsc = getMBeanConnection(vm);
		for (ObjectName name : getObjectNames(CalliServer.class, mbsc)) {
			server = JMX.newMXBeanProxy(mbsc, name, CalliServerMXBean.class);
		}
		for (ObjectName name : getObjectNames(WebServer.class, mbsc)) {
			webService = JMX.newMXBeanProxy(mbsc, name,
					WebServerMXBean.class);
		}
		for (ObjectName name : getObjectNames(JVMSummary.class, mbsc)) {
			summary = JMX.newMXBeanProxy(mbsc, name, JVMSummaryMXBean.class);
		}
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
		for (ObjectName name : getObjectNames(WebServer.class, mbsc)) {
			WebServerMXBean server = JMX.newMXBeanProxy(mbsc, name,
					WebServerMXBean.class);
			server.connectionDumpToFile(filename);
			info(filename);
		}
	}

	private Set<ObjectName> getObjectNames(Class<?> mclass,
			MBeanServerConnection mbsc) throws IOException,
			MalformedObjectNameException {
		ObjectName name = new ObjectName("*:type=" + mclass.getSimpleName() + ",*");
		for (Class<?> mx : mclass.getInterfaces()) {
			if (mx.getName().endsWith("Bean")) {
				QueryExp instanceOf = Query.isInstanceOf(Query.value(mclass.getName()));
				return mbsc.queryNames(name, instanceOf);
			}
		}
		throw new AssertionError(mclass.getSimpleName() + " does not have an interface that ends with Bean");
	}

	private void poolDump(MBeanServerConnection mbsc, String filename)
			throws MalformedObjectNameException, IOException {
		boolean empty = true;
		for (ObjectName mon : getObjectNames(ManagedThreadPool.class, mbsc)) {
			ThreadPoolMXBean pool = JMX.newMXBeanProxy(mbsc, mon,
					ThreadPoolMXBean.class);
			pool.threadDumpToFile(filename);
			empty = false;
		}
		if (!empty) {
			info(filename);
		}
	}

	private void traceDump(MBeanServerConnection mbsc, String filename)
			throws IOException, MalformedObjectNameException {
		for (ObjectName name : getObjectNames(CalliRepository.class, mbsc)) {
			CalliRepositoryMXBean repo = JMX.newMXBeanProxy(mbsc, name,
					CalliRepositoryMXBean.class);
			PrintWriter w = new PrintWriter(filename);
			try {
				for (String line : repo.showTraceSummary()) {
					w.println(line);
				}
			} finally {
				w.close();
			}
			info(filename);
		}
	}

	private void summaryDump(MBeanServerConnection mbsc, String filename)
			throws Throwable {
		try {
			if (this.summary != null) {
				String[] summary = this.summary.showVMSummary();
				PrintWriter w = new PrintWriter(filename);
				try {
					for (String line : summary) {
						w.println(line);
					}
				} finally {
					w.close();
				}
				info(filename);
			}
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

	private void topStatistics(String fileName) throws IOException {
		FileOutputStream out = new FileOutputStream(fileName);
		try {
			if (exec(out, System.err, "top", "-bn", "1")) {
				info(fileName);
			} else {
				new File(fileName).delete();
			}
		} catch (IOException e) {
			// top not installed
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

}
