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

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.callimachusproject.cli.Command;
import org.callimachusproject.cli.CommandSet;
import org.callimachusproject.concurrent.ManagedExecutors;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.setup.CallimachusSetup;
import org.callimachusproject.util.BackupTool;
import org.callimachusproject.util.CallimachusConf;
import org.callimachusproject.util.SystemProperties;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.util.ModelUtil;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line tool for setting up a new repository.
 * 
 * @author James Leigh
 * 
 */
public class Setup {
	public static final String NAME = Version.getInstance().getVersion();

	private static final CommandSet commands = new CommandSet(NAME);
	static {
		commands.require("c", "conf")
				.arg("file")
				.desc("The local etc/callimachus.conf file to read settings from");
		commands.option("b", "basedir").arg("directory")
				.desc("Base directory used for local storage");
		commands.option("k", "backups").arg("directory")
				.desc("Backup directory");
		commands.option("K", "no-backup").desc("Disable automatic backup");
		commands.option("u", "user").optional("name")
				.desc("Create the given user");
		commands.option("g", "group").arg("group path")
				.desc("Add the new user to this group (in addition to the admin group)");
		commands.option("n", "name").arg("name")
				.desc("If creating a new user use this full name");
		commands.option("e", "email").arg("addr")
				.desc("If creating a new user use this email address");
		commands.option("s", "silent").desc(
				"If the repository is already setup exit successfully");
		commands.option("h", "help").desc("Print help (this message) and exit");
		commands.option("V", "version").desc(
				"Print version information and exit");
	}

	public static void main(String[] args) {
		try {
			Setup setup = new Setup();
			setup.init(args);
			setup.start();
			setup.stop();
			setup.destroy();
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
		e.printStackTrace();
	}

	private final Logger logger = LoggerFactory.getLogger(Setup.class);
	private final ExecutorService executor = ManagedExecutors
			.getInstance().newFixedThreadPool(
					Runtime.getRuntime().availableProcessors(),
					Setup.class.getSimpleName());
	private final Set<String> groups = new HashSet<String>();
	private boolean silent;
	private File confFile;
	private File basedir;
	private BackupTool backup;
	private String name;
	private String email;
	private String username;
	private char[] password;

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
				silent = line.has("silent");
				if (line.has("basedir")) {
					basedir = new File(line.get("basedir"));
				} else {
					basedir = new File("").getCanonicalFile();
				}
				if (line.has("conf")) {
					confFile = new File(line.get("conf"));
				} else {
					confFile = new File("etc/callimachus.conf");
				}
				if (line.has("backups") && !line.has("no-backup")) {
					backup = new BackupTool(new File(line.get("backups")));
				}
				if (line.has("user") || line.has("email")) {
					this.name = line.get("name");
					this.email = line.get("email");
					String u = line.get("user");
					if (u != null && u.contains(":")) {
						username = u.substring(0, u.indexOf(':'));
						password = u.substring(u.indexOf(':') + 1).toCharArray();
						CallimachusSetup.validateName(username);
					}
					Console console = System.console();
					if (username == null || username.length() < 1) {
						if (u != null && u.length() > 0 && !u.contains(":")) {
							username = u;
						} else if (console == null) {
							Reader reader = new InputStreamReader(System.in);
							username = new BufferedReader(reader).readLine();
						} else {
							username = console.readLine("Enter a username: ");
						}
						CallimachusSetup.validateName(username);
					}
					if (email == null || email.length() < 1) {
						if (console == null) {
							Reader reader = new InputStreamReader(System.in);
							email = new BufferedReader(reader).readLine();
						} else {
							email = console.readLine("Enter an email: ");
						}
						CallimachusSetup.validateEmail(email);
					}
					if (line.has("group")) {
						groups.addAll(Arrays.asList(line.getAll("group")));
					}
				}
			}
		} catch (IllegalArgumentException e) {
			println(e);
			System.exit(1);
		} catch (Exception e) {
			println(e);
			System.err.println("Arguments: " + Arrays.toString(args));
			System.exit(1);
		}
	}

	public void start() throws Exception {
		final CallimachusConf conf = new CallimachusConf(confFile);
		boolean changed = false;
		final LocalRepositoryManager manager = RepositoryProvider
				.getRepositoryManager(basedir);
		final List<String> links = new ArrayList<String>();
		try {
			Map<String, String> idByOrigin = conf.getOriginRepositoryIDs();
			Set<String> repositoryIDs = new HashSet<String>(idByOrigin.values());
			List<Future<Boolean>> tasks = new ArrayList<Future<Boolean>>();
			for (final String id : repositoryIDs) {
				tasks.add(executor.submit(new Callable<Boolean>() {
					public Boolean call() throws Exception {
						return setupRepository(id, manager, conf, links);
					}
				}));
			}
			for (Future<Boolean> task : tasks) {
				changed |= task.get();
			}
			conf.setAppVersion(Version.getInstance().getVersionCode());
		} finally {
			// manager thinks these are initialise, so make sure they are
			for (String id : manager.getInitializedRepositoryIDs()) {
				manager.getRepository(id);
			}
		}
		if (!links.isEmpty()) {
			System.err.println("Use this URL to assign a password");
			System.err.println();
			for (String url : links) {
				System.out.println(url);
			}
			System.err.println();
		}
		if (changed || silent) {
			System.exit(0);
		} else {
			logger.warn("Repository is already setup");
			System.exit(166); // already setup
		}
	}

	public void stop() throws Exception {
		// do nothing
	}

	public void destroy() throws Exception {
		executor.shutdownNow();
	}

	Boolean setupRepository(String repositoryID,
			LocalRepositoryManager manager, CallimachusConf conf,
			Collection<String> links) throws IOException,
			MalformedURLException, OpenRDFException, NoSuchAlgorithmException {
		Set<String> webappOrigins = getWebappsInRepository(repositoryID, conf);
		File dataDir = manager.getRepositoryDir(repositoryID);
		if (backup != null && dataDir.isDirectory()) {
			backup.backup(getDefaultBackupLabel(repositoryID, conf), dataDir);
		}
		boolean changed = updateRepositoryConfig(manager, repositoryID);
		Repository repo = manager.getRepository(repositoryID);
		if (repo == null)
			throw new RepositoryConfigException(
					"Missing repository configuration for "
							+ dataDir.getAbsolutePath());
		CalliRepository repository = new CalliRepository(repo, dataDir);
		try {
			CallimachusSetup setup = new CallimachusSetup(repository);
			for (String origin : webappOrigins) {
				changed |= setup.prepareWebappOrigin(origin);
			}
			for (String origin : webappOrigins) {
				changed |= setup.createWebappOrigin(origin);
			}
			for (String origin : webappOrigins) {
				changed |= setup.finalizeWebappOrigin(origin);
			}
			if (email != null && email.length() > 0) {
				for (String origin : webappOrigins) {
					changed |= setup.createAdmin(email, username, name, null,
							origin);
					for (String group : groups) {
						changed |= setup.addUserToGroup(username, group, origin);
					}
					if (password == null || password.length < 1) {
						Set<String> reg = setup.getUserRegistrationLinks(username,
								email, origin);
						synchronized (links) {
							links.addAll(reg);
						}
					} else {
						changed |= setup.registerDigestUser(email, username,
								password, origin);
					}
				}
			}
		} finally {
			repository.shutDown();
		}
		return changed;
	}

	private Set<String> getWebappsInRepository(String repositoryID,
			CallimachusConf conf) throws IOException {
		Map<String, String> map = conf.getOriginRepositoryIDs();
		map = new HashMap<String, String>(map);
		Iterator<String> iter = map.values().iterator();
		while (iter.hasNext()) {
			if (!iter.next().equals(repositoryID)) {
				iter.remove();
			}
		}
		Set<String> webappOrigins = map.keySet();
		return webappOrigins;
	}

	private String getDefaultBackupLabel(String repositoryID, CallimachusConf conf) throws IOException {
		StringBuilder sb = new StringBuilder();
		if (repositoryID != null && repositoryID.length() > 0) {
			sb.append(repositoryID).append("-");
		}
		String version = conf.getAppVersion();
		if (version != null) {
			sb.append(version).append("_");
		}
		GregorianCalendar now = new GregorianCalendar();
		sb.append(now.get(Calendar.YEAR)).append('-');
		int month = now.get(Calendar.MONTH);
		if (month < 9) {
			sb.append('0');
		}
		sb.append(1 + month).append('-');
		int day = now.get(Calendar.DAY_OF_MONTH);
		if (day < 10) {
			sb.append('0');
		}
		sb.append(day);
		return sb.toString();
	}

	private boolean updateRepositoryConfig(final LocalRepositoryManager manager,
			String repositoryID) throws IOException,
			MalformedURLException, OpenRDFException {
		File repositoryConfig = SystemProperties.getRepositoryConfigFile();
		String configString = readContent(repositoryConfig.toURI().toURL());
		RepositoryConfig config = getRepositoryConfig(configString);
		if (repositoryID.equals(config.getID())) {
			return updateRepositoryConfig(manager, config);
		} else {
			return false;
		}
	}

	private String readContent(URL config) throws IOException {
		InputStream in = config.openStream();
		try {
			return new Scanner(in).useDelimiter("\\Z").next();
		} finally {
			in.close();
		}
	}

	private boolean updateRepositoryConfig(LocalRepositoryManager manager,
			RepositoryConfig config) throws RepositoryException,
			RepositoryConfigException {
		config.validate();
		String id = config.getID();
		if (manager.hasRepositoryConfig(id)) {
			RepositoryConfig oldConfig = manager.getRepositoryConfig(id);
			if (equal(config, oldConfig))
				return false;
			logger.warn("Replacing repository configuration");
		} else {
			logger.info("Creating repository: {}", id);
		}
		manager.addRepositoryConfig(config);
		if (manager.getInitializedRepositoryIDs().contains(id)) {
			manager.getRepository(id).shutDown();
		}
		return true;
	}

	private RepositoryConfig getRepositoryConfig(String configString)
			throws IOException, RDFParseException, RDFHandlerException,
			GraphUtilException, RepositoryConfigException {
		Graph graph = parseTurtleGraph(configString);
		Resource node = GraphUtil.getUniqueSubject(graph, RDF.TYPE,
				RepositoryConfigSchema.REPOSITORY);
		return RepositoryConfig.create(graph, node);
	}

	private Graph parseTurtleGraph(String configString) throws IOException,
			RDFParseException, RDFHandlerException {
		Graph graph = new GraphImpl();
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		rdfParser.setRDFHandler(new StatementCollector(graph));
		String base = new File(".").getAbsoluteFile().toURI().toASCIIString();
		rdfParser.parse(new StringReader(configString), base);
		return graph;
	}

	private boolean equal(RepositoryConfig c1, RepositoryConfig c2) {
		GraphImpl g1 = new GraphImpl();
		GraphImpl g2 = new GraphImpl();
		c1.export(g1);
		c2.export(g2);
		return ModelUtil.equals(g1, g2);
	}

}
