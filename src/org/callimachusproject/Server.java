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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.Arrays;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.callimachusproject.cli.Command;
import org.callimachusproject.cli.CommandSet;
import org.callimachusproject.concurrent.ManagedExecutors;
import org.callimachusproject.concurrent.ManagedThreadPool;
import org.callimachusproject.concurrent.ManagedThreadPoolListener;
import org.callimachusproject.management.BackupTool;
import org.callimachusproject.management.CalliKeyStore;
import org.callimachusproject.management.CalliServer;
import org.callimachusproject.management.CalliServer.ServerListener;
import org.callimachusproject.management.JVMSummary;
import org.callimachusproject.management.LogEmitter;
import org.callimachusproject.management.LoggingProperties;
import org.callimachusproject.management.SetupTool;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.server.HTTPObjectPolicy;
import org.callimachusproject.server.WebServer;
import org.callimachusproject.setup.CallimachusConf;
import org.callimachusproject.util.MailProperties;
import org.callimachusproject.util.SystemProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line tool for launching the server.
 * 
 * @author James Leigh
 * 
 */
public class Server {
	public static final String NAME = Version.getInstance().getVersion();

	private static final CommandSet commands = new CommandSet(NAME);
	static {
		commands.option("b", "basedir").arg("directory")
				.desc("Base directory used for local storage");
		commands.option("trust").desc(
				"Allow all server code to read, write, and execute all files and directories "
						+ "according to the file system's ACL");
		commands.option("pid").arg("file")
				.desc("File to store current process id");
		commands.option("q", "quiet").desc(
				"Don't print status messages to standard output.");
		commands.option("h", "help").desc("Print help (this message) and exit");
		commands.option("v", "version").desc(
				"Print version information and exit");
	}

	public static void main(String[] args) {
		try {
			Command line = commands.parse(args);
			if (line.has("pid")) {
				RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
				String pid = bean.getName().replaceAll("@.*", "");
				File file = new File(line.get("pid"));
				file.getParentFile().mkdirs();
				FileWriter writer = new FileWriter(file);
				try {
					writer.append(pid);
				} finally {
					writer.close();
				}
				file.deleteOnExit();
			}
			Server node = new Server();
			node.init(args);
			node.start();
			node.await();
		} catch (Throwable e) {
			while (e.getCause() != null) {
				e = e.getCause();
			}
			System.err.println("Arguments: " + Arrays.toString(args));
			System.err.println(e.toString());
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	private final Logger logger = LoggerFactory.getLogger(Server.class);
	private CalliServer node;

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
			File baseDir = new File(".");
			if (line.has("basedir")) {
				baseDir = new File(line.get("basedir"));
			}
			File backupDir = SystemProperties.getBackupDir();
			File carFile = SystemProperties.getWebappCarFile();
			File configFile = SystemProperties.getConfigFile();
			File defaultsFile = SystemProperties.getConfigDefaultsFile();
			File repositoryConfig = SystemProperties.getRepositoryConfigFile();
			ManagedExecutors.getInstance().addListener(
					new ManagedThreadPoolListener() {
						public void threadPoolStarted(String name,
								ManagedThreadPool pool) {
							registerMBean(name, pool, ManagedThreadPool.class);

						}

						public void threadPoolTerminated(String name) {
							unregisterMBean(name, ManagedThreadPool.class);
						}
					});
			CallimachusConf conf = new CallimachusConf(configFile, defaultsFile);
			SetupTool tool = new SetupTool(baseDir, repositoryConfig, carFile,
					conf);
			node = new CalliServer(tool, new ServerListener() {
				public void serverStarted(WebServer server) {
					registerMBean(server, WebServer.class);
					registerMBean(server.getRepository(), CalliRepository.class);
				}

				public void serverStopping(WebServer server) {
					unregisterMBean(CalliRepository.class);
					unregisterMBean(WebServer.class);
				}
			});
			registerMBean(node, CalliServer.class);
			registerMBean(new JVMSummary(), JVMSummary.class);
			registerMBean(new LogEmitter(), LogEmitter.class);
			registerMBean(LoggingProperties.getInstance(), LoggingProperties.class);
			registerMBean(MailProperties.getInstance(), MailProperties.class);
			registerMBean(new BackupTool(baseDir, backupDir), BackupTool.class);
			File etc = new File(baseDir, "etc");
			registerMBean(new CalliKeyStore(etc), CalliKeyStore.class);
			registerMBean(tool, SetupTool.class);
			if (!line.has("trust")) {
				HTTPObjectPolicy.apply(
						new String[] { carFile.getAbsolutePath(),
								defaultsFile.getAbsolutePath() }, configFile,
						repositoryConfig, backupDir, new File(baseDir,
								"repositories"));
			}
			node.init();
		} catch (Throwable e) {
			while (e.getCause() != null) {
				e = e.getCause();
			}
			System.err.println("Arguments: " + Arrays.toString(args));
			System.err.println(e.toString());
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	public void start() throws Exception {
		node.start();
	}

	public void await() throws InterruptedException {
		synchronized (node) {
			while (node.isRunning()) {
				node.wait();
			}
		}
	}

	public void stop() throws Exception {
		node.stop();
	}

	public void destroy() throws Exception {
		try {
			node.destroy();
		} finally {
			unregisterMBean(CalliServer.class);
			unregisterMBean(JVMSummary.class);
			unregisterMBean(LogEmitter.class);
			unregisterMBean(LoggingProperties.class);
			unregisterMBean(MailProperties.class);
			unregisterMBean(BackupTool.class);
			unregisterMBean(CalliKeyStore.class);
			unregisterMBean(SetupTool.class);
			ManagedExecutors.getInstance().cleanup();
		}
	}

	private <T> void registerMBean(T bean, Class<T> beanClass) {
		registerMBean(null, bean, beanClass);
	}

	private void unregisterMBean(Class<?> beanClass) {
		unregisterMBean(null, beanClass);
	}

	private <T> void registerMBean(String name, T bean, Class<T> beanClass) {
		try {
			ObjectName oname = getMBeanName(name, beanClass);
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.registerMBean(bean, oname);
		} catch (Exception e) {
			logger.error(e.toString(), e);
		}
	}

	private void unregisterMBean(String name, Class<?> beanClass) {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.unregisterMBean(getMBeanName(name, beanClass));
		} catch (Exception e) {
			// ignore
		}
	}

	private ObjectName getMBeanName(String name, Class<?> beanClass)
			throws MalformedObjectNameException {
		String pkg = Server.class.getPackage().getName();
		String simple = beanClass.getSimpleName();
		StringBuilder sb = new StringBuilder();
		sb.append(pkg).append(":type=").append(simple);
		if (name != null) {
			sb.append(",name=").append(name);
		}
		return new ObjectName(sb.toString());
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
}
