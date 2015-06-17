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

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.callimachusproject.cli.Command;
import org.callimachusproject.cli.CommandSet;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.setup.CallimachusSetup;
import org.callimachusproject.setup.SetupTool;
import org.callimachusproject.util.BackupTool;
import org.callimachusproject.util.CallimachusConf;
import org.callimachusproject.util.DomainNameSystemResolver;
import org.callimachusproject.util.SystemProperties;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.concurrent.ManagedExecutors;
import org.openrdf.http.object.management.ObjectRepositoryManager;
import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryImplConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line tool for setting up a new repository.
 * 
 * @author James Leigh
 * 
 */
public class Setup {
	private static final String STYLES_CSS = "styles/callimachus.less?less";
	private static final String SCRIPTS_JS = "../scripts.js";
	private static final String ADMIN_GROUP = "/auth/groups/admin";

	public static final String NAME = Version.getInstance().getVersion();

	private static final CommandSet commands = new CommandSet(NAME);
	static {
		commands.require("c", "conf")
				.arg("file")
				.desc("The local etc/callimachus.conf file to read settings from");
		commands.option("d", "dataDir").arg("directory")
				.desc("Base directory used for local storage");
		commands.option("k", "backups").arg("directory")
				.desc("Backup directory");
		commands.option("K", "no-backup").desc("Disable automatic backup");
		commands.option("G", "no-upgrade")
				.desc("Disables upgrading stored data (new users and repository config may still be modified)");
		commands.option("u", "user").optional("name")
				.desc("Create the given user");
		commands.option("g", "group").arg("group path")
				.desc("Add the new user to this group (in addition to the admin group)");
		commands.option("e", "email").optional("addr")
				.desc("If creating a new user use this email address");
		commands.option("l", "launch").optional("command")
				.desc("When the setup is complete launch this command and (if appropriate) open a Web browser to register");
		commands.option("L", "no-launch").desc("Don't launch any commands and don't open any browsers");
		commands.option("h", "help").desc("Print help (this message) and exit");
		commands.option("v", "version").desc(
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

	final Logger logger = LoggerFactory.getLogger(Setup.class);
	private final ExecutorService executor = ManagedExecutors
			.getInstance().newFixedThreadPool(
					Runtime.getRuntime().availableProcessors(),
					Setup.class.getSimpleName());
	private final Set<String> groups = new HashSet<String>();
	private File confFile;
	private File basedir;
	private BackupTool backup;
	private boolean upgrade;
	private String email;
	private String defaultEmail;
	private String username;
	private char[] password;
	private String launch;

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
			} else 
				if (line.isParseError()) {
				line.printParseError();
				System.exit(2);
				return;
			} else {
				if (line.has("dataDir")) {
					basedir = new File(line.get("dataDir"));
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
				upgrade = !line.has("no-upgrade");
				if (line.has("user") || line.has("email")) {
					this.email = line.get("email");
					String u = line.get("user");
					if (u != null) {
						if (u.contains(":")) {
							username = u.substring(0, u.indexOf(':'));
							password = u.substring(u.indexOf(':') + 1).toCharArray();
						} else {
							username = u;
						}
						CallimachusSetup.validateName(username);
					}
					if (email == null || email.length() < 1) {
						email = null;
						String hostname = DomainNameSystemResolver
								.getInstance().getCanonicalLocalHostName();
						String user = System.getProperty("user.name");
						if ("root".equals(user) && System.getenv("SUDO_USER") != null) {
							user = System.getenv("SUDO_USER");
						}
						defaultEmail = user + "@"
								+ hostname;
						CallimachusSetup.validateEmail(defaultEmail);
					}
					if (line.has("group")) {
						groups.addAll(Arrays.asList(line.getAll("group")));
					}
				}
				if (line.has("launch") && !line.has("no-launch")) {
					launch = line.get("launch");
					if (launch == null) {
						launch = "";
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
		Set<String> webapps = new LinkedHashSet<String>();
		final List<String> links = new ArrayList<String>();
		final ObjectRepositoryManager manager = new ObjectRepositoryManager(basedir);
		try {
			Map<String, String> idByOrigin = conf.getOriginRepositoryIDs();
			Set<String> repositoryIDs = new LinkedHashSet<String>(idByOrigin.values());
			List<Future<Collection<String>>> tasks = new ArrayList<Future<Collection<String>>>();
			for (final String id : repositoryIDs) {
				tasks.add(executor.submit(new Callable<Collection<String>>() {
					public Collection<String> call() throws Exception {
						return setupRepository(id, manager, conf, links);
					}
				}));
			}
			for (Future<Collection<String>> task : tasks) {
				webapps.addAll(task.get());
			}
			conf.setAppVersion(Version.getInstance().getVersionCode());
		} finally {
			manager.shutDown();
		}
		if (!links.isEmpty()) {
			System.err.println("Use this URL to assign a password");
			System.err.println();
			System.err.flush();
			for (String url : links) {
				System.out.println(url);
			}
			System.out.flush();
			System.err.println();
		}
		String cmd = launch;
		if (cmd != null) {
			if (cmd.length() > 0) {
				System.gc();
				launch(cmd);
				Thread.sleep(2000);
				waitUntilServing(webapps);
			}
			openWebBrowser(links);
		}
		System.exit(0);
	}

	public void stop() throws Exception {
		// do nothing
	}

	public void destroy() throws Exception {
		executor.shutdownNow();
	}

	Collection<String> setupRepository(String repositoryID,
			ObjectRepositoryManager manager, CallimachusConf conf,
			Collection<String> links) throws IOException,
			MalformedURLException, OpenRDFException, NoSuchAlgorithmException,
			URISyntaxException {
		Set<String> webappOrigins = getWebappsInRepository(repositoryID, conf);
		if (manager.isRepositoryPresent(repositoryID)) {
			URL dataURL = manager.getRepositoryLocation(repositoryID);
			if ("file".equalsIgnoreCase(dataURL.getProtocol())) {
				File dataDir = new File(dataURL.toURI());
				if (backup != null && dataDir != null && dataDir.isDirectory()) {
					backup.backup(repositoryID, conf.getAppVersion(), dataDir);
				}
			}
		} else {
			updateRepositoryConfig(manager, repositoryID);
		}
		String[] prefixes = new String[webappOrigins.size()];
		int i=0;
		for (String origin : webappOrigins) {
			prefixes[i++] = origin + "/";
		}
		manager.setRepositoryPrefixes(repositoryID, prefixes);
		Repository repo = manager.getRepository(repositoryID);
		if (repo == null)
			throw new RepositoryConfigException(
					"Missing repository configuration for " + repositoryID);
		Map<String, String> webapps = new LinkedHashMap<String, String>();
		CalliRepository repository = new CalliRepository(repositoryID, manager);
		try {
			CallimachusSetup setup = new CallimachusSetup(repository);
			if (upgrade) {
				upgradeRepository(setup, webappOrigins, links);
			}
			for (String origin : webappOrigins) {
				try {
					webapps.put(origin, setup.getWebappURL(origin));
				} catch (IllegalStateException e) {
					logger.warn(e.getMessage());
				}
			}
			if (email != null || defaultEmail != null) {
				for (String origin : webapps.keySet()) {
					if (email != null || !setup.isRegisteredAdmin(origin)) {
						String e = email == null ? defaultEmail : email;
						setup.inviteUser(e, origin);
						setup.addInvitedUserToGroup(e, ADMIN_GROUP, origin);
						for (String group : groups) {
							setup.addInvitedUserToGroup(e, group, origin);
						}
						if (password != null && password.length > 0
								&& username != null && username.length() > 0) {
							setup.registerDigestUser(e, username, password,
									origin);
						} else {
							Set<String> reg = setup.getUserRegistrationLinks(e,
									origin);
							synchronized (links) {
								links.addAll(reg);
							}
						}
					}
				}
			}
		} finally {
			repository.shutDown();
		}
		return webapps.values();
	}

	private void upgradeRepository(CallimachusSetup setup,
			Set<String> webappOrigins, Collection<String> links)
			throws OpenRDFException, IOException, NoSuchAlgorithmException {
		for (String origin : webappOrigins) {
			setup.prepareWebappOrigin(origin);
		}
		for (String origin : webappOrigins) {
			setup.createWebappOrigin(origin);
		}
		for (String origin : webappOrigins) {
			setup.updateWebapp(origin);
		}
		for (String origin : webappOrigins) {
			setup.finalizeWebappOrigin(origin);
		}
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
		return map.keySet();
	}

	private boolean updateRepositoryConfig(final ObjectRepositoryManager manager,
			String repositoryID) throws IOException,
			MalformedURLException, OpenRDFException {
		File repositoryConfig = SystemProperties.getRepositoryConfigFile();
		String configString = readContent(repositoryConfig.toURI().toURL());
		String systemId = repositoryConfig.toURI().toASCIIString();
		RepositoryConfig config = SetupTool.getRepositoryConfig(manager, configString, systemId);
		if (repositoryID.equals(config.getID())) {
			return updateRepositoryConfig(manager, config);
		} else {
			RepositoryImplConfig impl = config.getRepositoryImplConfig();
			config = new RepositoryConfig(repositoryID, config.getTitle(), impl);
			return updateRepositoryConfig(manager, config);
		}
	}

	private boolean updateRepositoryConfig(ObjectRepositoryManager manager,
			RepositoryConfig config) throws OpenRDFException {
		config.validate();
		String id = config.getID();
		logger.info("Creating repository: {}", id);
		manager.addRepository(config);
		return true;
	}

	private void launch(String cmd) throws IOException {
		final Process exec = Runtime.getRuntime().exec(cmd);
		new Thread() {
			@Override
			public void run() {
				try {
					InputStream in = exec.getInputStream();
					try {
						InputStreamReader isr = new InputStreamReader(in);
						BufferedReader br = new BufferedReader(isr);
						String line = null;
						while ((line = br.readLine()) != null) {
							System.out.println(line);
						}
					} finally {
						in.close();
					}
				} catch (IOException ioe) {
					logger.error(ioe.getMessage(), ioe);
				}
			}
		}.start();
		new Thread() {
			@Override
			public void run() {
				try {
					InputStream stderr = exec.getErrorStream();
					try {
						exec.getOutputStream().close();
						InputStreamReader isr = new InputStreamReader(stderr);
						BufferedReader br = new BufferedReader(isr);
						String line = null;
						while ((line = br.readLine()) != null) {
							System.err.println(line);
						}
					} finally {
						stderr.close();
					}
				} catch (IOException ioe) {
					logger.error(ioe.getMessage(), ioe);
				}
			}
		}.start();
	}

	private void waitUntilServing(Set<String> webapps)
			throws InterruptedException {
		for (int i = 0; i < 120; i++) {
			try {
				for (String webapp : webapps) {
					readContent(URI.create(webapp).resolve(SCRIPTS_JS).toURL());
					readContent(URI.create(webapp).resolve(STYLES_CSS).toURL());
				}
				break;
			} catch (ConnectException e) {
				Thread.sleep(2000);
				continue;
			} catch (IOException e) {
				break;
			}
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

	private void openWebBrowser(final List<String> links) throws IOException {
		if (links.isEmpty()) {
			logger.debug("No outstanding invites for this email address");
		} else if ("root".equals(System.getProperty("user.name"))) {
			logger.debug("Not going to open a Web browser as root");
		} else if (Desktop.isDesktopSupported()) {
		    Desktop desktop = Desktop.getDesktop();
		    if (desktop.isSupported(Desktop.Action.BROWSE)) {
		    	for (String url : links) {
		    		desktop.browse(URI.create(url));
		    	}
		    } else {
		    	logger.debug("Browser not suported");
		    }
		} else {
			logger.debug("Desktop not supported");
		}
	}

}
