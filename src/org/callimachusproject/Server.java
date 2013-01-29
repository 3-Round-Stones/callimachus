/*
 * Portions Copyright (c) 2011-13 3 Round Stones Inc., Some Rights Reserved
 * Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
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
package org.callimachusproject;

import static org.openrdf.repository.manager.RepositoryProvider.getRepositoryIdOfRepository;
import static org.openrdf.repository.manager.RepositoryProvider.getRepositoryManagerOfRepository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.callimachusproject.cli.Command;
import org.callimachusproject.cli.CommandSet;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.logging.LoggerBean;
import org.callimachusproject.server.CallimachusRepository;
import org.callimachusproject.server.CallimachusServer;
import org.callimachusproject.server.HTTPObjectPolicy;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line tool for launching the server.
 * 
 * @author James Leigh
 * 
 */
public class Server {
	private static final String CHANGES_PATH = "../changes/";
	private static final String ERROR_XPL_PATH = "pipelines/error.xpl";
	public static final String NAME = Version.getInstance().getVersion();

	private static final CommandSet commands = new CommandSet(NAME);
	static {
		commands.option("n", "name").arg("name").desc("Server name");
		commands.option("o", "origin").arg("http").desc(
				"The scheme, hostname and port ( http://localhost:8080 )");
		commands.option("p", "port").arg("number").desc(
						"Port the server should listen on");
		commands.option("s", "sslport").arg("number").desc(
				"Secure port the server should listen on");
		commands.require("r", "repository").arg("url").desc(
				"The Sesame repository url (relative file: or http:)");
		commands.option("d", "dir").arg("directory").desc(
				"Directory used for data storage and retrieval");
		commands.option("trust").desc(
				"Allow all server code to read, write, and execute all files and directories "
						+ "according to the file system's ACL");
		commands.option("pid").arg("file").desc(
				"File to store current process id");
		commands.option("q", "quiet").desc(
				"Don't print status messages to standard output.");
		commands.option("h", "help").desc(
				"Print help (this message) and exit");
		commands.option("v", "version").desc(
				"Print version information and exit");
	}

	public static void main(String[] args) {
		try {
			Command line = commands.parse(args);
			Server server = new Server();
			if (line.has("pid")) {
				storeProcessIdentifier(line.get("pid"));
			}
			server.init(args);
			server.start();
			Thread.sleep(1000);
			if (server.server.isRunning() && !line.has("quiet")) {
				System.out.println();
				System.out.println(server.getClass().getSimpleName()
						+ " is listening on port " + server.getPort()
						+ " for " + server.toString() + "/");
				System.out.println("Repository: " + server.getRepository());
				System.out.println("Origin: " + server.toString());
			} else if (!server.server.isRunning()) {
				System.err.println(server.getClass().getSimpleName()
						+ " could not be started.");
				System.exit(7);
			}
		} catch (ClassNotFoundException e) {
			System.err.print("Missing jar with: ");
			System.err.println(e.toString());
			System.exit(5);
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

	private static void storeProcessIdentifier(String pidFile)
			throws IOException {
		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		String pid = bean.getName().replaceAll("@.*", "");
		File file = new File(pidFile);
		file.getParentFile().mkdirs();
		FileWriter writer = new FileWriter(file);
		try {
			writer.append(pid);
		} finally {
			writer.close();
		}
		file.deleteOnExit();
	}

	private CallimachusServer server;
	private int[] ports = new int[0];
	private int[] sslports = new int[0];

	public String toString() {
		if (server == null)
			return super.toString();
		return server.toString();
	}

	public Integer getPort() {
		if (ports.length > 0)
			return ports[0];
		if (sslports.length > 0)
			return sslports[0];
		return null;
	}

	public CallimachusRepository getRepository() {
		return server.getRepository();
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
			} else if (line.has("quiet")) {
				try {
					logStdout();
				} catch (SecurityException e) {
					// ignore
				}
			}
			init(line);
		} catch (Exception e) {
			println(e);
			System.err.println("Arguments: " + Arrays.toString(args));
			System.exit(1);
		}
	}

	public void start() throws Exception {
		server.start();
	}

	public void stop() throws Exception {
		if (server != null) {
			server.stop();
		}
	}

	public void destroy() throws Exception {
		if (server != null) {
			server.getRepository().shutDown();
			server.destroy();
		}
		unregisterMBean();
	}

	private void logStdout() {
		System.setOut(new PrintStream(new OutputStream() {
			private int ret = "\r".getBytes()[0];
			private int newline = "\n".getBytes()[0];
			private Logger logger = LoggerFactory.getLogger("stdout");
			private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	
			public synchronized void write(int b) throws IOException {
				if (b == ret || b == newline) {
					if (buffer.size() > 0) {
						logger.info(buffer.toString());
						buffer.reset();
					}
				} else {
					buffer.write(b);
				}
			}
		}, true));
		System.setErr(new PrintStream(new OutputStream() {
			private int ret = "\r".getBytes()[0];
			private int newline = "\n".getBytes()[0];
			private Logger logger = LoggerFactory.getLogger("stderr");
			private ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	
			public synchronized void write(int b) throws IOException {
				if (b == ret || b == newline) {
					if (buffer.size() > 0) {
						logger.warn(buffer.toString());
						buffer.reset();
					}
				} else {
					buffer.write(b);
				}
			}
		}, true));
	}

	private void init(Command line) throws Exception {
		String rurl = getRepositoryUrl(line);
		Repository repository = RepositoryProvider.getRepository(rurl);
		File dataDir = repository.getDataDir();
		if (line.has("dir")) {
			dataDir = new File(line.get("dir")).getCanonicalFile();
		}
		if (dataDir == null) {
			RepositoryManager manager = getRepositoryManagerOfRepository(rurl);
			if (manager instanceof LocalRepositoryManager) {
				String id = getRepositoryIdOfRepository(rurl);
				dataDir = ((LocalRepositoryManager) manager).getRepositoryDir(id);
			} else {
				dataDir = new File("").getCanonicalFile();
			}
		}
		File cacheDir = new File(dataDir, "cache");
		File in = new File(cacheDir, "client");
		HTTPObjectClient.setCacheDirectory(in);
		server = new CallimachusServer(repository, dataDir);
		if (line.has("port")) {
			String[] values = line.getAll("port");
			ports = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				ports[i] = Integer.parseInt(values[i]);
			}
		}
		if (line.has("sslport")) {
			String[] values = line.getAll("sslport");
			sslports = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				sslports[i] = Integer.parseInt(values[i]);
			}
		}
		if (!line.has("port") && !line.has("sslport")) {
			ports = new int[] { 8080 };
		}
		boolean primary = true;
		if (line.has("origin")) {
			for (String o : line.getAll("origin")) {
				if (primary) {
					server.setChangesPath(o, CHANGES_PATH);
					server.setErrorPipe(o, ERROR_XPL_PATH);
					primary = false;
				}
				server.addOrigin(o);
			}
		}
		if (line.has("name")) {
			server.setServerName(line.get("name"));
		}
		if (!line.has("trust")) {
			applyPolicy(line, repository, dataDir);
		}
		server.listen(ports, sslports);
		registerMBean(server);
	}

	private ObjectName getMXServerName() throws MalformedObjectNameException {
		String pkg = Server.class.getPackage().getName();
		return new ObjectName(pkg + ":type=" + CallimachusServer.class.getSimpleName());
	}

	private ObjectName getMXLoggerName() throws MalformedObjectNameException {
		String pkg = Server.class.getPackage().getName();
		return new ObjectName(pkg + ":type=Logger");
	}

	private void registerMBean(CallimachusServer server) throws InstanceAlreadyExistsException,
			MBeanRegistrationException, NotCompliantMBeanException,
			MalformedObjectNameException {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.registerMBean(new LoggerBean(), getMXLoggerName());
			mbs.registerMBean(server, getMXServerName());
		} catch (Exception e) {
			// ignore
		}
	}

	private void unregisterMBean() {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.unregisterMBean(getMXLoggerName());
			mbs.unregisterMBean(getMXServerName());
		} catch (Exception e) {
			// ignore
		}
	}

	private String getRepositoryUrl(Command line)
			throws RepositoryException, RepositoryConfigException {
		if (line.has("repository")) {
			String url = line.get("repository");
			Repository repository = RepositoryProvider.getRepository(url);
			if (repository != null)
				return url;
			throw new IllegalStateException("No repository found");
		} else {
			throw new IllegalArgumentException("Option -r is required");
		}
	}

	private void applyPolicy(Command line, Repository repository, File dir) throws IOException {
		if (!line.has("trust")) {
			List<File> directories = new ArrayList<File>();
			directories.add(dir);
			if (repository.getDataDir() != null) {
				directories.add(repository.getDataDir().getParentFile());
			}
			File[] write = directories.toArray(new File[directories.size()]);
			HTTPObjectPolicy.apply(new String[0], write);
		}
	}

}
