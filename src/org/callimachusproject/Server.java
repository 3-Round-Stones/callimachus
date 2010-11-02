/*
 * Copyright (c) 2010, Zepheira LLC Some rights reserved.
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
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.rmi.UnmarshalException;
import java.util.Properties;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import net.contentobjects.jnotify.JNotify;
import net.contentobjects.jnotify.JNotifyException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.callimachusproject.logging.LoggerBean;
import org.openrdf.http.object.ConnectionBean;
import org.openrdf.http.object.HTTPObjectAgentMXBean;
import org.openrdf.http.object.HTTPObjectPolicy;
import org.openrdf.http.object.client.HTTPObjectClient;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line tool for launching the server.
 * 
 * @author James Leigh
 * 
 */
public class Server implements HTTPObjectAgentMXBean {
	public static final String NAME;
	private static final String BRAND = "Callimachus Project Server";
	private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
	private static final String VERSION_PATH = "/META-INF/callimachusproject.properties";
	private static final String VERSION;
	static {
		Properties properties = new Properties();
		InputStream in = Server.class.getResourceAsStream(VERSION_PATH);
		if (in != null) {
			try {
				properties.load(in);
			} catch (IOException e) {
				// ignore
			}
		}
		String version = properties.getProperty("version");
		if (version == null) {
			version = "devel";
		}
		VERSION = version;
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			String name = Server.class.getPackage().getName() + ":type=log";
			mbs.registerMBean(new LoggerBean(), new ObjectName(name));
		} catch (Exception e) {
			// ignore
		}
		NAME = BRAND + '/' + VERSION;
	}
	private static final String REPOSITORY_TEMPLATE = "META-INF/templates/callimachus-config.ttl";

	private static final Options options = new Options();
	static {
		options.addOption("n", "name", true, "Server name");
		options.addOption("a", "authority", true,
				"The hostname and port ( localhost:8080 )");
		options
				.addOption("p", "port", true,
						"Port the server should listen on");
		options.addOption("r", "repository", true,
				"The existing repository url (relative file: or http:)");
		options.addOption("d", "dir", true,
				"Directory used for data storage and retrieval");
		options.addOption("u", "update", false,
				"If the server should reload all web resources");
		options.addOption("trust", false,
				"Allow all server code to read, write, and execute all files and directories "
						+ "according to the file system's ACL");
		Option fromOpt = new Option("from", true,
				"Email address for the human user who controls this server");
		fromOpt.setOptionalArg(true);
		options.addOption(fromOpt);
		options.addOption("pid", true,
				"File to store current process id or process id to stop");
		options.addOption("stop", false,
				"Use the PID file to shutdown the server");
		options.addOption("q", "quiet", false,
				"Don't print status messages to standard output.");
		options.addOption("h", "help", false,
				"Print help (this message) and exit");
		options.addOption("v", "version", false,
				"Print version information and exit");
	}

	public static void main(String[] args) {
		try {
			CommandLine line = new GnuParser().parse(options, args);
			if (line.hasOption("stop")) {
				if (line.hasOption("pid")) {
					destroyService(args);
				} else {
					System.out.println("Missing required pid option.");
					System.exit(1);
				}
			} else {
				Server server = new Server();
				if (line.hasOption("pid")) {
					MBeanServer mbs = ManagementFactory
							.getPlatformMBeanServer();
					mbs.registerMBean(server, getObjectName());
					initService(args);
				}
				server.init(args);
				server.start();
				Thread.sleep(1000);
				if (server.isRunning() && !line.hasOption('q')) {
					System.out.println();
					System.out.println(server.getClass().getSimpleName()
							+ " is listening on port " + server.getPort()
							+ " for http://" + server.getAuthority() + "/");
					System.out.println("Repository: " + server.getRepository());
					System.out.println("Webapps: " + server.getWebappsDir());
					System.out.println("Authority: " + server.getAuthority());
				} else if (!server.isRunning()) {
					System.err.println(server.getClass().getSimpleName()
							+ " could not be started.");
					System.exit(1);
				}
			}
		} catch (ClassNotFoundException e) {
			System.err.print("Missing jar with: ");
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (Exception e) {
			if (e.getMessage() != null) {
				System.err.println(e.getMessage());
			} else {
				e.printStackTrace(System.err);
			}
			System.exit(1);
		}
	}

	private static void logStdout() {
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

	private static void initService(String[] args) throws Exception {
		CommandLine line = new GnuParser().parse(options, args);
		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		String pid = bean.getName().replaceAll("@.*", "");
		File file = new File(line.getOptionValue("pid"));
		file.getParentFile().mkdirs();
		FileWriter writer = new FileWriter(file);
		try {
			writer.append(pid);
		} finally {
			writer.close();
		}
		file.deleteOnExit();
	}

	private static void destroyService(String[] args) throws Exception {
		Class<?> VM = Class.forName("com.sun.tools.attach.VirtualMachine");
		Method attach = VM.getDeclaredMethod("attach", String.class);
		Method getAgentProperties = VM.getDeclaredMethod("getAgentProperties");
		Method getSystemProperties = VM
				.getDeclaredMethod("getSystemProperties");
		Method loadAgent = VM.getDeclaredMethod("loadAgent", String.class);

		CommandLine line = new GnuParser().parse(options, args);
		File file = new File(line.getOptionValue("pid"));
		String pid = IOUtil.readString(file);

		// attach to the target application
		Object vm = attach.invoke(null, pid);

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
		MBeanServerConnection mbsc = connector.getMBeanServerConnection();
		ObjectName objectName = getObjectName();
		HTTPObjectAgentMXBean server = JMX.newMXBeanProxy(mbsc, objectName,
				HTTPObjectAgentMXBean.class);
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

	private static ObjectName getObjectName()
			throws MalformedObjectNameException {
		String pkg = Server.class.getPackage().getName();
		String name = pkg + ":type=" + Server.class.getSimpleName();
		ObjectName objectName = new ObjectName(name);
		return objectName;
	}

	private CallimachusServer server;
	private int port = 8080;

	public String getAuthority() {
		if (server == null)
			return null;
		return server.getAuthority();
	}

	public int getPort() {
		return port;
	}

	public ObjectRepository getRepository() {
		if (server == null)
			return null;
		return server.getRepository();
	}

	public File getWebappsDir() {
		if (server == null)
			return null;
		return server.getWebappsDir();
	}

	public int getCacheCapacity() {
		return server.getCacheCapacity();
	}

	public void setCacheCapacity(int capacity) {
		server.setCacheCapacity(capacity);
	}

	public int getCacheSize() {
		return server.getCacheSize();
	}

	public String getFrom() {
		return server.getFrom();
	}

	public void setFrom(String from) {
		server.setFrom(from);
	}

	public String getName() {
		return server.getName();
	}

	public void setName(String name) {
		server.setName(name);
	}

	public void invalidateCache() throws Exception {
		server.invalidateCache();
	}

	public boolean isCacheAggressive() {
		return server.isCacheAggressive();
	}

	public void setCacheAggressive(boolean cacheAggressive) {
		server.setCacheAggressive(cacheAggressive);
	}

	public boolean isCacheDisconnected() {
		return server.isCacheDisconnected();
	}

	public void setCacheDisconnected(boolean cacheDisconnected) {
		server.setCacheDisconnected(cacheDisconnected);
	}

	public boolean isCacheEnabled() {
		return server.isCacheEnabled();
	}

	public void setCacheEnabled(boolean cacheEnabled) {
		server.setCacheEnabled(cacheEnabled);
	}

	public void resetCache() throws Exception {
		server.resetCache();
	}

	public ConnectionBean[] getConnections() {
		return server.getConnections();
	}

	public void resetConnections() throws IOException {
		server.resetConnections();
	}

	public void poke() {
		server.poke();
	}

	public void init(String[] args) {
		try {
			CommandLine line = new GnuParser().parse(options, args);
			if (line.hasOption('h') || line.getArgs().length > 0) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("[options]", options);
				System.exit(0);
				return;
			} else if (line.hasOption('v')) {
				System.out.println(NAME);
				System.exit(0);
				return;
			} else if (line.hasOption('q')) {
				try {
					logStdout();
				} catch (SecurityException e) {
					// ignore
				}
			}
			init(line);
		} catch (Exception e) {
			if (e.getMessage() != null) {
				System.err.println(e.getMessage());
			} else {
				e.printStackTrace(System.err);
			}
			System.exit(2);
		}
	}

	public void start() throws Exception {
		server.start();
	}

	public String getStatus() {
		return server.getStatus();
	}

	public boolean isRunning() {
		if (server == null)
			return false;
		return server.isRunning();
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
	}

	private void init(CommandLine line) throws IOException,
			RepositoryException, RepositoryConfigException,
			MalformedURLException, RDFParseException, RDFHandlerException,
			GraphUtilException, Exception, JNotifyException {
		File dir = new File("").getCanonicalFile();
		File webappsDir = new File(dir, "webapps");
		if (line.hasOption('d')) {
			dir = new File(line.getOptionValue('d')).getCanonicalFile();
			webappsDir = new File(dir, "webapps");
		}
		Repository repository = getRepository(line, dir);
		if (repository.getDataDir() != null) {
			dir = repository.getDataDir();
		}
		File cacheDir = new File(dir, "cache");
		File in = new File(cacheDir, "client");
		HTTPObjectClient.setInstance(in, 1024);
		if (line.hasOption("from")) {
			String from = line.getOptionValue("from");
			HTTPObjectClient.getInstance().setFrom(from == null ? "" : from);
		}
		server = new CallimachusServer(repository, webappsDir, dir);
		if (line.hasOption('p')) {
			port = Integer.parseInt(line.getOptionValue('p'));
		}
		if (line.hasOption('a')) {
			server.setAuthority(line.getOptionValue('a'));
		}
		if (line.hasOption('n')) {
			server.setServerName(line.getOptionValue('n'));
		}
		if (line.hasOption('u')) {
			server.setConditionalRequests(false);
		}
		try {
			JNotify.removeWatch(-1); // load library
		} catch (UnsatisfiedLinkError e) {
			System.err.println(e.getMessage());
		}
		webappsDir.mkdirs();
		if (!line.hasOption("trust")) {
			applyPolicy(line, repository, dir, webappsDir);
		}
		if (!line.hasOption('q')) {
			server.printStatus(System.out);
		}
		server.listen(port);
	}

	private Repository getRepository(CommandLine line, File dir)
			throws RepositoryException, RepositoryConfigException,
			MalformedURLException, IOException, RDFParseException,
			RDFHandlerException, GraphUtilException {
		if (line.hasOption('r')) {
			String url = line.getOptionValue('r');
			Repository repository = RepositoryProvider.getRepository(url);
			if (repository == null)
				throw new RepositoryConfigException("Repository Not Setup at " + url);
			return repository;
		}
		RepositoryManager manager = RepositoryProvider.getRepositoryManager(dir
				.toURI().toASCIIString());
		ClassLoader cl = Server.class.getClassLoader();
		URL url = cl.getResource(REPOSITORY_TEMPLATE);
		Graph graph = new GraphImpl();
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		rdfParser.setRDFHandler(new StatementCollector(graph));

		String base = new File(".").getAbsoluteFile().toURI().toASCIIString();
		URLConnection con = url.openConnection();
		StringBuilder sb = new StringBuilder();
		for (String mimeType : RDFFormat.TURTLE.getMIMETypes()) {
			if (sb.length() < 1) {
				sb.append(", ");
			}
			sb.append(mimeType);
		}
		con.setRequestProperty("Accept", sb.toString());
		InputStream in = con.getInputStream();
		try {
			rdfParser.parse(in, base);
		} finally {
			in.close();
		}
		Resource node = GraphUtil.getUniqueSubject(graph, RDF.TYPE,
				RepositoryConfigSchema.REPOSITORY);
		String id = GraphUtil.getUniqueObjectLiteral(graph, node,
				RepositoryConfigSchema.REPOSITORYID).stringValue();
		if (manager.hasRepositoryConfig(id))
			return manager.getRepository(id);
		RepositoryConfig config = RepositoryConfig.create(graph, node);
		config.validate();
		manager.addRepositoryConfig(config);
		return manager.getRepository(id);
	}

	private void applyPolicy(CommandLine line, Repository repository, File dir,
			File webappsDir) {
		if (!line.hasOption("trust")) {
			if (repository.getDataDir() == null) {
				HTTPObjectPolicy.apply(new String[0], dir, webappsDir);
			} else {
				File repositoriesDir = repository.getDataDir().getParentFile();
				HTTPObjectPolicy.apply(new String[0], repositoriesDir, dir,
						webappsDir);
			}
		}
	}

}
