/*
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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.callimachusproject.logging.LoggerBean;
import org.callimachusproject.server.CallimachusServer;
import org.callimachusproject.server.ConnectionBean;
import org.callimachusproject.server.HTTPObjectAgentMXBean;
import org.callimachusproject.server.HTTPObjectPolicy;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.repository.DelegatingRepository;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.Sail;
import org.openrdf.sail.StackableSail;
import org.openrdf.sail.auditing.AuditingSail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line tool for launching the server.
 * 
 * @author James Leigh
 * 
 */
public class Server implements HTTPObjectAgentMXBean {
	private static final String CHANGE_PATH = "/change/";
	public static final String NAME = Version.getInstance().getVersion();

	private static final Options options = new Options();
	static {
		options.addOption("n", "name", true, "Server name");
		options.addOption("o", "origin", true,
				"The scheme, hostname and port ( http://localhost:8080 )");
		options.addOption("p", "port", true,
						"Port the server should listen on");
		options.addOption("s", "sslport", true,
				"Secure port the server should listen on");
		options.addOption("r", "repository", true,
				"The Sesame repository url (relative file: or http:)");
		options.getOption("repository").setRequired(true);
		options.addOption("d", "dir", true,
				"Directory used for data storage and retrieval");
		options.addOption("trust", false,
				"Allow all server code to read, write, and execute all files and directories "
						+ "according to the file system's ACL");
		Option fromOpt = new Option("from", true,
				"Email address for the human user who controls this server");
		fromOpt.setOptionalArg(true);
		options.addOption(fromOpt);
		options.addOption("pid", true,
				"File to store current process id");
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
			Server server = new Server();
			if (line.hasOption("pid")) {
				storeProcessIdentifier(line.getOptionValue("pid"));
			}
			server.init(args);
			server.start();
			Thread.sleep(1000);
			if (server.isRunning() && !line.hasOption('q')) {
				System.out.println();
				System.out.println(server.getClass().getSimpleName()
						+ " is listening on port " + server.getPort()
						+ " for " + server.getOrigin() + "/");
				System.out.println("Repository: " + server.getRepository());
				System.out.println("Origin: " + server.getOrigin());
			} else if (!server.isRunning()) {
				System.err.println(server.getClass().getSimpleName()
						+ " could not be started.");
				System.exit(1);
			}
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

	public String getOrigin() {
		if (server == null)
			return null;
		return server.getOrigin();
	}

	public Integer getPort() {
		if (ports.length > 0)
			return ports[0];
		if (sslports.length > 0)
			return sslports[0];
		return null;
	}

	public ObjectRepository getRepository() {
		if (server == null)
			return null;
		return server.getRepository();
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

	public void connectionDumpToFile(String outputFile) throws IOException {
		 server.connectionDumpToFile(outputFile);
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
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("[options]", options);
				System.exit(0);
				return;
			} else if (line.getArgs().length > 0) {
				System.err.println("Unrecognized option: " + Arrays.toString(line.getArgs()));
				System.err.println("Arguments: " + Arrays.toString(args));
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
			println(e);
			System.err.println("Arguments: " + Arrays.toString(args));
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

	private void init(CommandLine line) throws Exception {
		File dir = new File("").getCanonicalFile();
		if (line.hasOption('d')) {
			dir = new File(line.getOptionValue('d')).getCanonicalFile();
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
		server = new CallimachusServer(repository, dir);
		if (line.hasOption('p')) {
			String[] values = line.getOptionValues('p');
			ports = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				ports[i] = Integer.parseInt(values[i]);
			}
		}
		if (line.hasOption('s')) {
			String[] values = line.getOptionValues('s');
			sslports = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				sslports[i] = Integer.parseInt(values[i]);
			}
		}
		if (!line.hasOption('p') && !line.hasOption('s')) {
			ports = new int[] { 8080 };
		}
		if (line.hasOption('o')) {
			for (String o : line.getOptionValues('o')) {
				server.addOrigin(o);
			}
		}
		if (line.hasOption('n')) {
			server.setServerName(line.getOptionValue('n'));
		}
		AuditingSail sail = findAuditingSail(repository);
		if (sail != null) {
			sail.setNamespace(server.getOrigin() + CHANGE_PATH);
		}
		if (!line.hasOption("trust")) {
			applyPolicy(line, repository, dir);
		}
		server.listen(ports, sslports);
		registerMBean();
	}

	private ObjectName getMXServerName() throws MalformedObjectNameException {
		String pkg = Server.class.getPackage().getName();
		return new ObjectName(pkg + ":type=" + Server.class.getSimpleName());
	}

	private ObjectName getMXLoggerName() throws MalformedObjectNameException {
		String pkg = Server.class.getPackage().getName();
		return new ObjectName(pkg + ":type=Logger");
	}

	private void registerMBean() throws InstanceAlreadyExistsException,
			MBeanRegistrationException, NotCompliantMBeanException,
			MalformedObjectNameException {
		MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		mbs.registerMBean(new LoggerBean(), getMXLoggerName());
		mbs.registerMBean(this, getMXServerName());
	}

	private void unregisterMBean() {
		try {
			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			mbs.unregisterMBean(getMXLoggerName());
			mbs.unregisterMBean(getMXServerName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private AuditingSail findAuditingSail(Repository repository) {
		if (repository instanceof SailRepository)
			return findAuditingSail(((SailRepository) repository).getSail());
		if (repository instanceof DelegatingRepository)
			return findAuditingSail(((DelegatingRepository) repository)
					.getDelegate());
		return null;
	}

	private AuditingSail findAuditingSail(Sail sail) {
		if (sail instanceof AuditingSail)
			return (AuditingSail) sail;
		if (sail instanceof StackableSail)
			return findAuditingSail(((StackableSail) sail).getBaseSail());
		return null;
	}

	private Repository getRepository(CommandLine line, File dir)
			throws RepositoryException, RepositoryConfigException,
			MalformedURLException, IOException, RDFParseException,
			RDFHandlerException, GraphUtilException {
		if (line.hasOption('r')) {
			String url = line.getOptionValue('r');
			Repository repository = RepositoryProvider.getRepository(url);
			if (repository != null)
				return repository;
			throw new IllegalStateException("No repository found");
		} else {
			throw new IllegalArgumentException("Option -r is required");
		}
	}

	private void applyPolicy(CommandLine line, Repository repository, File dir) throws IOException {
		if (!line.hasOption("trust")) {
			List<File> directories = new ArrayList<File>();
			directories.addAll(getLoggingDirectories());
			directories.add(dir);
			if (repository.getDataDir() != null) {
				directories.add(repository.getDataDir().getParentFile());
			}
			File[] write = directories.toArray(new File[directories.size()]);
			HTTPObjectPolicy.apply(new String[0], write);
		}
	}

	private List<File> getLoggingDirectories() throws IOException {
		List<File> directories = new ArrayList<File>();
        String fname = System.getProperty("java.util.logging.config.file");
        if (fname == null) {
            fname = System.getProperty("java.home");
            if (fname == null) {
                throw new Error("Can't find java.home ??");
            }
            File f = new File(fname, "lib");
            f = new File(f, "logging.properties");
            fname = f.getCanonicalPath();
        }
        InputStream in = new FileInputStream(fname);
        try {
        	Properties properties = new Properties();
			properties.load(in);
    		String handlers = properties.getProperty("handlers");
			for (String logger : handlers.split("[\\s,]+")) {
    			String pattern = properties.getProperty(logger + ".pattern");
    			if (pattern != null) {
    				File dir = getLogPatternDirectory(pattern);
    				dir.mkdirs();
					directories.add(dir);
    			}
    		}
    		return directories;
        } finally {
            in.close();
        }
	}

	/**
     * Transform the pattern to the valid directory name, replacing any patterns.
     */
    private File getLogPatternDirectory(String pattern) {
        String tempPath = System.getProperty("java.io.tmpdir"); //$NON-NLS-1$
        boolean tempPathHasSepEnd = (tempPath == null ? false : tempPath
                .endsWith(File.separator));

        String homePath = System.getProperty("user.home"); //$NON-NLS-1$
        boolean homePathHasSepEnd = (homePath == null ? false : homePath
                .endsWith(File.separator));

        StringBuilder sb = new StringBuilder();
        pattern = pattern.replace('/', File.separatorChar);

        int cur = 0;
        int next = 0;
        char[] value = pattern.toCharArray();
        while ((next = pattern.indexOf('%', cur)) >= 0) {
            if (++next < pattern.length()) {
                switch (value[next]) {
                    case 't':
                        /*
                         * we should probably try to do something cute here like
                         * lookahead for adjacent '/'
                         */
                        sb.append(value, cur, next - cur - 1).append(tempPath);
                        if (!tempPathHasSepEnd) {
                            sb.append(File.separator);
                        }
                        break;
                    case 'h':
                        sb.append(value, cur, next - cur - 1).append(homePath);
                        if (!homePathHasSepEnd) {
                            sb.append(File.separator);
                        }
                        break;
                    case '%':
                        sb.append(value, cur, next - cur - 1).append('%');
                        break;
                    default:
                        sb.append(value, cur, next - cur - 1);
                        return new File(sb.substring(0, sb.lastIndexOf(File.separator)));
                }
                cur = ++next;
            } else {
                // fail silently
            }
        }

        sb.append(value, cur, value.length - cur);
        return new File(sb.substring(0, sb.lastIndexOf(File.separator)));
    }

}
