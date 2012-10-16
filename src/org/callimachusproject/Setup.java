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

import static org.openrdf.query.QueryLanguage.SPARQL;
import info.aduna.io.IOUtil;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.callimachusproject.cli.Command;
import org.callimachusproject.cli.CommandSet;
import org.callimachusproject.io.CarInputStream;
import org.callimachusproject.server.CallimachusRepository;
import org.callimachusproject.server.CallimachusServer;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.callimachusproject.server.client.HTTPServiceUnavailable;
import org.callimachusproject.server.util.ChannelUtil;
import org.callimachusproject.util.DomainNameSystemResolver;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.util.ModelUtil;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.algebra.evaluation.util.ValueComparator;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.object.ObjectConnection;
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
	private static final String CALLIMACHUS = "/callimachus/";
	private static final String SYSTEM_GROUP = "/group/system";
	private static final String ACTIVITY_PATH = "/activity/";
	private static final String ACTIVITY_TYPE = CALLIMACHUS + "Activity";
	private static final String FOLDER_TYPE = CALLIMACHUS + "Folder";
	private static final String GRAPH_DOCUMENT = CALLIMACHUS + "GraphDocument";
	private static final String SERVE_ALL = "/everything-else-public.ttl";
	private static final String SERVE_ALL_TTL = "META-INF/templates/callimachus-all-serviceable.ttl";
	private static final String INITIAL_RU = "META-INF/upgrade/callimachus-initial.ru";
	private static final String MAIN_ARTICLE = "META-INF/templates/main-article.docbook";

	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_DESCRIBEDBY = CALLI + "describedby";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";
	private static final String CALLI_ADMINISTRATOR = CALLI + "administrator";
	private static final String CALLI_EDITOR = CALLI + "editor";
	private static final String CALLI_CONTRIBUTOR = CALLI + "contributor";
	private static final String CALLI_SUBSCRIBER = CALLI + "subscriber";
	private static final String CALLI_READER = CALLI + "reader";
	private static final String CALLI_ORIGIN = CALLI + "Origin";
	private static final String CALLI_REALM = CALLI + "Realm";
	private static final String CALLI_FOLDER = CALLI + "Folder";
	private static final String CALLI_UNAUTHORIZED = CALLI + "unauthorized";
	private static final String CALLI_FORBIDDEN = CALLI + "forbidden";
	private static final String CALLI_AUTHENTICATION = CALLI + "authentication";
	private static final String CALLI_MENU = CALLI + "menu";
	private static final String CALLI_FAVICON = CALLI + "favicon";
	private static final String CALLI_THEME = CALLI + "theme";
	private static final String CALLI_ENCODED = CALLI + "encoded";
	private static final String CALLI_AUTHNAME = CALLI + "authName";
	private static final String CALLI_AUTHNAMESPACE = CALLI + "authNamespace";
	private static final String CALLI_USER = CALLI + "User";
	private static final String CALLI_PARTY = CALLI + "Party";
	private static final String CALLI_NAME = CALLI + "name";
	private static final String CALLI_EMAIL = CALLI + "email";
	private static final String CALLI_ALGORITHM = CALLI + "algorithm";
	private static final String CALLI_SECRET = CALLI + "secret";
	private static final String CALLI_PASSWORD = CALLI + "passwordDigest";
	private static final String CALLI_MEMBER = CALLI + "member";
	private static final String CALLI_ANONYMOUSFROM = CALLI + "anonymousFrom";

	private static final CommandSet commands = new CommandSet(NAME);
	static {
		commands.option("d", "dir").arg("directory").desc("Directory used for data storage and retrieval");
		commands.require("c", "config").arg("file").desc(
				"A repository config url (relative file: or http:)");
		commands.require("f", "car").arg("file").desc(
				"Target and CAR URL pairs, separated by equals, that should be installed for each primary origin");
		commands.require("o", "origin").arg("http").desc(
				"The scheme, hostname and port ( http://localhost:8080 ) that resolves to this server");
		commands.option("v", "virtual").arg("http").desc(
				"Additional scheme, hostname and port ( http://localhost:8080 ) that resolves to this server");
		commands.option("r", "realm").arg("http").desc(
				"The scheme, hostname, port, and path ( http://example.com:8080/ ) that does not resolve to this server");
		commands.option("l", "serve-all").desc("Serve all other resources publicly");
		commands.option("u", "user").optional("name").desc(
				"Create the given user and prompt for a password, or append the password separated by a colon");
		commands.option("n", "name").arg("name").desc(
				"If creating a new user use this full name");
		commands.option("e", "email").arg("addr").desc(
				"If creating a new user use this email address");
		commands.option("s", "silent").desc(
				"If the repository is already setup exit successfully");
		commands.option("h", "help").desc(
				"Print help (this message) and exit");
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
	private final DomainNameSystemResolver dnsResolver = DomainNameSystemResolver.getInstance();
	private CallimachusRepository repository;
	private boolean silent;
	private String serveAllAs;
	private File dir;
	private URL config;
	private final Map<String, URL> cars = new HashMap<String, URL>();
	private final Set<String> origins = new HashSet<String>();
	private final Map<String, String> vhosts = new HashMap<String, String>();
	private final Map<String, String> realms = new HashMap<String, String>();
	private LocalRepositoryManager manager;
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
				if (line.has("dir")) {
					dir = new File(line.get("dir")).getCanonicalFile();
				} else {
					dir = new File("").getCanonicalFile();
				}
				if (line.has("config")) {
					config = resolve(line.get("config"));
				}
				if (line.has("origin")) {
					for (String o : line.getAll("origin")) {
						validateOrigin(o);
						origins.add(o);
					}
					if (line.has("car")) {
						for (String pair : line.getAll("car")) {
							int idx = pair.indexOf('=');
							String path = pair.substring(0, idx);
							URL url = resolve(pair.substring(idx + 1));
							for (String origin : origins) {
								java.net.URI uri = java.net.URI.create(origin + "/");
								cars.put(uri.resolve(path).toASCIIString(), url);
							}
						}
					}
					String origin = line.get("origin");
					if (line.has("virtual")) {
						for (String v : line.getAll("virtual")) {
							validateOrigin(v);
							vhosts.put(v, origin);
						}
					}
					if (line.has("realm")) {
						for (String r : line.getAll("realm")) {
							validateRealm(r);
							realms.put(r, origin);
						}
					}
					if (line.has("serve-all")
							&& line.getAll("origin").length == 1
							&& !line.has("virtual") && !line.has("realm")) {
						serveAllAs = origin;
					}
					if (line.has("user") || line.has("email")) {
						this.name = line.get("name");
						this.email = line.get("email");
						String u = line.get("user");
						if (u != null && u.contains(":")) {
							username = u.substring(0, u.indexOf(':'));
							password = u.substring(u.indexOf(':') + 1).toCharArray();
							validateName(username);
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
							validateName(username);
						}
						if (email == null || email.length() < 1) {
							if (console == null) {
								Reader reader = new InputStreamReader(System.in);
								email = new BufferedReader(reader).readLine();
							} else {
								email = console.readLine("Enter an email: ");
							}
							validateEmail(email);
						}
						if (password == null) {
							if (console == null) {
								Reader reader = new InputStreamReader(System.in);
								password = new BufferedReader(reader).readLine().toCharArray();
							} else  {
								password = console.readPassword("Enter a new password for %s: ", email);
							}
						}
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
		String configString = readContent(config);
		System.err.println(connect(dir, configString).toURI().toASCIIString());
		boolean changed = false;
		for (String origin : origins) {
			changed |= createOrigin(origin, repository);
		}
		for (Map.Entry<String, String> e : vhosts.entrySet()) {
			changed |= createVirtualHost(e.getKey(), e.getValue(), repository);
		}
		for (Map.Entry<String, String> e : realms.entrySet()) {
			changed |= createRealm(e.getKey(), e.getValue(), repository);
		}
		for (Map.Entry<String, URL> e : cars.entrySet()) {
			String origin = origins.iterator().next();
			for (String o : origins) {
				if (e.getKey().startsWith(o + "/")) {
					origin = o;
				}
			}
			changed |= importCar(e.getValue(), e.getKey(), origin, repository);
		}
		changed |= setServeAllResourcesAs(serveAllAs, repository);
		if (email != null && email.length() > 0) {
			for (String origin : origins) {
				changed |= createAdmin(name, email, username, password, origin, repository);
			}
			if (password != null) {
				Arrays.fill(password, '*');
				System.gc();
			}
		}
		if (changed || silent) {
			System.exit(0);
		} else {
			logger.warn("Repository is already setup");
			System.exit(166); // already setup
		}
	}

	public void stop() throws Exception {
		disconnect();
	}

	public void destroy() throws Exception {
		// do nothing
	}

	public File connect(File dir, String configString) throws OpenRDFException,
			MalformedURLException, IOException {
		if (repository != null)
			throw new IllegalStateException("Must call disconnect before connect can be called again");
		repository = getCallimachusRepository(dir, configString);
		if (repository == null)
			throw new RepositoryConfigException(
					"Missing repository configuration");
		RepositoryConfig config = getRepositoryConfig(configString);
		manager = RepositoryProvider.getRepositoryManager(dir);
		return manager.getRepositoryDir(config.getID());
	}

	public void disconnect() {
		repository = null;
		if (manager != null) {
			manager.shutDown();
			manager = null;
		}
	}

	public void createOrigin(String origin) throws Exception {
		validateOrigin(origin);
		if (repository == null)
			throw new IllegalStateException("Not connected");
		createOrigin(origin, repository);
	}

	public void importCar(URL car, String folder, String origin)
			throws Exception {
		validateOrigin(origin);
		if (repository == null)
			throw new IllegalStateException("Not connected");
		if (car == null)
			throw new IllegalArgumentException("No CAR provided");
		importCar(car, folder, origin, repository);
	}

	public void createVirtualHost(String virtual, String origin)
			throws RepositoryException, IOException {
		validateOrigin(virtual);
		validateOrigin(origin);
		if (repository == null)
			throw new IllegalStateException("Not connected");
		createVirtualHost(virtual, origin, repository);
	}

	public void createRealm(String realm, String origin)
			throws RepositoryException, IOException {
		validateRealm(realm);
		validateOrigin(origin);
		if (repository == null)
			throw new IllegalStateException("Not connected");
		createRealm(realm, origin, repository);
	}

	public void setServeAllResourcesAs(String origin)
			throws OpenRDFException, IOException {
		if (repository == null)
			throw new IllegalStateException("Not connected");
		setServeAllResourcesAs(origin, repository);
	}

	public void createAdmin(String name, String email, String username,
			char[] password, String origin) throws RepositoryException,
			IOException {
		if (repository == null)
			throw new IllegalStateException("Not connected");
		createAdmin(name, email, username, password, origin, repository);
	}

	private void validateOrigin(String origin) {
		if (origin == null)
			throw new IllegalArgumentException("Missing origin");
		if (origin.endsWith("/"))
			throw new IllegalArgumentException("Origins must not include a path");
		java.net.URI uri = java.net.URI.create(origin + "/");
		if (uri.isOpaque())
			throw new IllegalArgumentException("Origins must not be opaque");
		if (!uri.isAbsolute())
			throw new IllegalArgumentException("Origins must be absolute");
		if (!"/".equals(uri.getPath()))
			throw new IllegalArgumentException("Origins must not include a path");
		if (uri.getQuery() != null)
			throw new IllegalArgumentException("Origins must not include a query part");
		if (uri.getFragment() != null)
			throw new IllegalArgumentException("Origins must not include a fragment part");
		if (uri.getUserInfo() != null)
			throw new IllegalArgumentException("Origins must not include any user info");
	}

	private void validateRealm(String realm) {
		if (realm == null)
			throw new IllegalArgumentException("Missing origin");
		if (!realm.endsWith("/"))
			throw new IllegalArgumentException("Realms must end with '/'");
		java.net.URI uri = java.net.URI.create(realm);
		if (uri.isOpaque())
			throw new IllegalArgumentException("Realms must not be opaque");
		if (!uri.isAbsolute())
			throw new IllegalArgumentException("Realms must be absolute");
		if (uri.getQuery() != null)
			throw new IllegalArgumentException("Realms must not include a query part");
		if (uri.getFragment() != null)
			throw new IllegalArgumentException("Realms must not include a fragment part");
		if (uri.getUserInfo() != null)
			throw new IllegalArgumentException("Realms must not include any user info");
	}

	private URL resolve(String file) throws MalformedURLException {
		try {
			return new File(".").toURI().resolve(file).toURL();
		} catch (IllegalArgumentException e) {
			return new File(file).toURI().toURL();
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

	private CallimachusRepository getCallimachusRepository(File baseDir,
			String configString) throws OpenRDFException,
			MalformedURLException, IOException {
		RepositoryConfig config = getRepositoryConfig(configString);
		Repository repo = getRepository(baseDir, config);
		if (repo == null)
			return null;
		File dataDir = repo.getDataDir();
		if (dataDir == null) {
			LocalRepositoryManager manager = getRepositoryManager(baseDir);
			dataDir = manager.getRepositoryDir(config.getID());
		}
		return new CallimachusRepository(repo, dataDir);
	}

	private Repository getRepository(File baseDir, RepositoryConfig config)
			throws OpenRDFException, MalformedURLException, IOException {
		LocalRepositoryManager manager = getRepositoryManager(baseDir);
		if (config == null || manager == null)
			return null;
		String id = config.getID();
		if (manager.hasRepositoryConfig(id)) {
			RepositoryConfig oldConfig = manager.getRepositoryConfig(id);
			if (equal(config, oldConfig))
				return manager.getRepository(id);
			logger.warn("Replacing repository configuration");
		}
		config.validate();
		logger.info("Creating repository: {}", id);
		manager.addRepositoryConfig(config);
		return manager.getRepository(id);
	}

	private RepositoryConfig getRepositoryConfig(String configString)
			throws IOException, RDFParseException, RDFHandlerException,
			GraphUtilException, RepositoryConfigException {
		Graph graph = parseTurtleGraph(configString);
		Resource node = GraphUtil.getUniqueSubject(graph, RDF.TYPE,
				RepositoryConfigSchema.REPOSITORY);
		RepositoryConfig config = RepositoryConfig.create(graph, node);
		return config;
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

	private LocalRepositoryManager getRepositoryManager(File dir)
			throws RepositoryConfigException, RepositoryException {
		return RepositoryProvider.getRepositoryManager(dir);
	}

	private boolean equal(RepositoryConfig c1, RepositoryConfig c2) {
		GraphImpl g1 = new GraphImpl();
		GraphImpl g2 = new GraphImpl();
		c1.export(g1);
		c2.export(g2);
		return ModelUtil.equals(g1, g2);
	}

	private boolean createOrigin(String o, CallimachusRepository repository)
			throws Exception {
		repository.setActivityFolderAndType(o + ACTIVITY_PATH, o
				+ ACTIVITY_TYPE, o + FOLDER_TYPE);
		boolean modified = createVirtualHost(o, o, repository);
		modified |= initializeOrUpgradeStore(repository, o);
		modified |= addSystemGroupMembers(o + SYSTEM_GROUP, repository);
		return modified;
	}

	private boolean initializeOrUpgradeStore(CallimachusRepository repository,
			String origin) throws RepositoryException, IOException,
			OpenRDFException {
		String version = getStoreVersion(repository, origin);
		if (version == null) {
			initializeStore(origin, repository);
		}
		String newVersion = upgradeStore(repository, origin);
		return version == null || !newVersion.equals(version);
	}

	private String getStoreVersion(CallimachusRepository repository,
			String origin) throws RepositoryException {
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			RepositoryResult<Statement> stmts;
			URI s = vf.createURI(origin + "/callimachus");
			stmts = con.getStatements(s, OWL.VERSIONINFO, null);
			try {
				if (!stmts.hasNext())
					return null;
				return stmts.next().getObject().stringValue();
			} finally {
				stmts.close();
			}
		} finally {
			con.close();
		}
	}

	private void initializeStore(String origin, CallimachusRepository repository)
			throws OpenRDFException {
		try {
			ClassLoader cl = CallimachusServer.class.getClassLoader();
			loadDefaultGraphs(origin, repository, cl);
			uploadMainArticle(origin, repository, cl);
		} catch (IOException e) {
			logger.debug(e.toString(), e);
		}
	}

	private void loadDefaultGraphs(String origin, Repository repository,
			ClassLoader cl) throws IOException, OpenRDFException {
		Enumeration<URL> resources = cl.getResources(INITIAL_RU);
		if (!resources.hasMoreElements())
			logger.warn("Missing {}", INITIAL_RU);
		while (resources.hasMoreElements()) {
			InputStream in = resources.nextElement().openStream();
			try {
				logger.info("Initializing {} Store", origin);
				Reader reader = new InputStreamReader(in, "UTF-8");
				String ru = IOUtil.readString(reader);
				RepositoryConnection con = repository.getConnection();
				try {
					con.setAutoCommit(false);
					con.prepareUpdate(SPARQL, ru, origin + "/").execute();
					con.setAutoCommit(true);
				} finally {
					con.close();
				}
			} catch (MalformedQueryException exc) {
				logger.warn(exc.toString(), exc);
			}
		}
	}

	private void uploadMainArticle(String origin,
			CallimachusRepository repository, ClassLoader cl)
			throws RepositoryException, IOException {
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			URI folder = vf.createURI(origin + "/");
			URI article = vf.createURI(origin + "/main-article.docbook");
			logger.info("Uploading main article: {}", article);
			con.add(article, RDF.TYPE,
					vf.createURI(origin + CALLIMACHUS + "Article"));
			con.add(article, RDF.TYPE,
					vf.createURI("http://xmlns.com/foaf/0.1/Document"));
			con.add(article, RDFS.LABEL, vf.createLiteral("main article"));
			con.add(article, vf.createURI(CALLI_READER),
					vf.createURI(origin + "/group/public"));
			con.add(article, vf.createURI(CALLI_SUBSCRIBER),
					vf.createURI(origin + "/group/users"));
			con.add(article, vf.createURI(CALLI_EDITOR),
					vf.createURI(origin + "/group/staff"));
			con.add(article, vf.createURI(CALLI_ADMINISTRATOR),
					vf.createURI(origin + "/group/admin"));
			con.add(folder, vf.createURI(CALLI_HASCOMPONENT), article);
			con.add(folder, vf.createURI(CALLI_DESCRIBEDBY),
					vf.createLiteral("main-article.docbook?view"));
			InputStream in = cl.getResourceAsStream(MAIN_ARTICLE);
			try {
				OutputStream out = con.getBlobObject(article)
						.openOutputStream();
				try {
					ChannelUtil.transfer(in, out);
				} finally {
					out.close();
				}
			} finally {
				in.close();
			}
			con.setAutoCommit(true);
		} finally {
			con.close();
		}
	}

	private String upgradeStore(CallimachusRepository repository, String origin)
			throws IOException, OpenRDFException {
		String version = getStoreVersion(repository, origin);
		ClassLoader cl = getClass().getClassLoader();
		String name = "META-INF/upgrade/callimachus-" + version + ".ru";
		Enumeration<URL> resources = cl.getResources(name);
		while (resources.hasMoreElements()) {
			InputStream in = resources.nextElement().openStream();
			logger.info("Upgrading store from {}", version);
			Reader reader = new InputStreamReader(in, "UTF-8");
			String ru = IOUtil.readString(reader);
			ObjectConnection con = repository.getConnection();
			try {
				con.setAutoCommit(false);
				con.prepareUpdate(SPARQL, ru, origin).execute();
				con.setAutoCommit(true);
			} finally {
				con.close();
			}
		}
		String newVersion = getStoreVersion(repository, origin);
		if (version != null && !version.equals(newVersion)) {
			logger.info("Upgraded store from {} to {}", version, newVersion);
			return upgradeStore(repository, origin);
		}
		return newVersion;
	}

	private boolean addSystemGroupMembers(String group,
			CallimachusRepository repository) throws SocketException, OpenRDFException {
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			URI subj = vf.createURI(group);
			URI pred = vf.createURI(CALLI_ANONYMOUSFROM);
			boolean modified = false;
			for (String host : dnsResolver.reverseAllLocalHosts()) {
				Literal obj = vf.createLiteral(host);
				if (!con.hasStatement(subj, pred, obj)) {
					con.add(subj, pred, obj);
					modified = true;
				}
			}
			con.setAutoCommit(true);
			return modified;
		} finally {
			con.close();
		}
	}

	private boolean importCar(URL car, String folder, String origin,
			CallimachusRepository repository) throws Exception {
		createFolder(folder, origin, repository);
		URI[] schemaGraphs = importSchema(car, folder, origin, repository);
		importArchive(schemaGraphs, car, folder, origin, repository);
		return true;
	}

	private URI[] importSchema(URL car, String folder, String origin,
			CallimachusRepository repository) throws RepositoryException,
			IOException, RDFParseException {
		Collection<URI> schemaGraphs = new LinkedHashSet<URI>();
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			CarInputStream carin = new CarInputStream(car.openStream());
			try {
				String name;
				while ((name = carin.readEntryName()) != null) {
					try {
						URI graph = importSchemaGraphEntry(carin, folder, con);
						if (graph != null) {
							schemaGraphs.add(graph);
						}
					} catch (RDFParseException e) {
						String msg = e.getMessage() + " in " + name;
						RDFParseException pe = new RDFParseException(msg, e.getLineNumber(), e.getColumnNumber());
						pe.initCause(e);
						throw pe;
					}
				}
			} finally {
				carin.close();
			}
			con.setAutoCommit(true);
		} finally {
			con.close();
		}
		return schemaGraphs.toArray(new URI[schemaGraphs.size()]);
	}

	private URI importSchemaGraphEntry(CarInputStream carin, String folder,
			ObjectConnection con) throws IOException, RDFParseException,
			RepositoryException {
		ValueFactory vf = con.getValueFactory();
		String target = folder + carin.readEntryName();
		InputStream in = carin.getEntryStream();
		try {
			if (carin.isSchemaEntry()) {
				URI graph = con.getActivityURI();
				con.add(in, target, RDFFormat.RDFXML, graph);
				return graph;
			} else if (carin.isFileEntry()) {
				URI graph = vf.createURI(target);
				if (carin.getEntryType().startsWith("application/rdf+xml")) {
					con.clear(graph);
					con.add(in, target, RDFFormat.RDFXML, graph);
					return graph;
				} else if (carin.getEntryType().startsWith("text/turtle")) {
					con.clear(graph);
					con.add(in, target, RDFFormat.TURTLE, graph);
					return graph;
				} else {
					byte[] buf = new byte[1024];
					while (in.read(buf) >= 0)
						;
					return null;
				}
			} else {
				byte[] buf = new byte[1024];
				while (in.read(buf) >= 0)
					;
				return null;
			}
		} finally {
			in.close();
		}
	}

	private void importArchive(URI[] schemaGraphs, URL car,
			String folderUri, String origin, CallimachusRepository repository)
			throws Exception {
		InetSocketAddress host = getAuthorityAddress(origin);
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HTTPServiceUnavailable service = new HTTPServiceUnavailable();
		client.setProxy(host, service);
		for (URI schemaGraph : schemaGraphs) {
			repository.addSchemaGraph(schemaGraph);
		}
		repository.setCompileRepository(true);
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			if (schemaGraphs.length > 0) {
				con.clear(schemaGraphs);
			}
			Object folder = con.getObject(folderUri);
			Method UploadFolderComponents = findUploadFolderComponents(folder);
			InputStream in = car.openStream();
			try {
				logger.info("Importing {} into {}", car, folderUri);
				int argc = UploadFolderComponents.getParameterTypes().length;
				Object[] args = new Object[argc];
				args[0] = in;
				UploadFolderComponents.invoke(folder, args);
			} catch (InvocationTargetException e) {
				try {
					throw e.getCause();
				} catch (Exception cause) {
					throw cause;
				} catch (Error cause) {
					throw cause;
				} catch (Throwable cause) {
					throw e;
				}
			} finally {
				in.close();
			}
			repository.setCompileRepository(false);
			con.setAutoCommit(true);
		} finally {
			con.close();
			client.removeProxy(host, service);
		}
	}

	private Method findUploadFolderComponents(Object folder)
			throws NoSuchMethodException {
		for (Method method : folder.getClass().getMethods()) {
			if ("UploadFolderComponents".equals(method.getName()))
				return method;
		}
		throw new NoSuchMethodException("UploadFolderComponents");
	}

	private boolean createFolder(String folder, String origin,
			CallimachusRepository repository) throws RepositoryException {
		boolean modified = false;
		int idx = folder.lastIndexOf('/', folder.length() - 2);
		String parent = folder.substring(0, idx + 1);
		if (parent.endsWith("://")) {
			parent = null;
		} else {
			modified = createFolder(parent, origin, repository);
		}
		ValueFactory vf = repository.getValueFactory();
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			URI uri = vf.createURI(folder);
			if (con.hasStatement(uri, RDF.TYPE, vf.createURI(origin + CALLIMACHUS + "Origin")))
				return modified;
			if (con.hasStatement(uri, RDF.TYPE, vf.createURI(origin + CALLIMACHUS + "Realm")))
				return modified;
			if (con.hasStatement(uri, RDF.TYPE, vf.createURI(origin + CALLIMACHUS + "Folder")))
				return modified;
			if (parent == null)
				throw new IllegalStateException("Can only import a CAR within a previously defined origin or realm");
			if (con.hasStatement(vf.createURI(parent), vf.createURI(CALLI_HASCOMPONENT), uri))
				return modified;
			con.add(vf.createURI(parent), vf.createURI(CALLI_HASCOMPONENT), uri);
			String label = folder.substring(parent.length()).replace("/", "").replace('-', ' ');
			con.add(uri, RDF.TYPE, vf.createURI(CALLI_FOLDER));
			con.add(uri, RDF.TYPE, vf.createURI(origin + CALLIMACHUS + "Folder"));
			con.add(uri, RDFS.LABEL, vf.createLiteral(label));
			add(con, uri, CALLI_READER, origin + "/group/public");
			add(con, uri, CALLI_ADMINISTRATOR, origin + "/group/admin");
			con.setAutoCommit(true);
			return true;
		} finally {
			con.close();
		}
	}

	private InetSocketAddress getAuthorityAddress(String origin) {
		InetSocketAddress host;
		if (origin.indexOf(':') != origin.lastIndexOf(':')) {
			int slash = origin.lastIndexOf('/');
			int colon = origin.lastIndexOf(':');
			int port = Integer.parseInt(origin.substring(colon + 1));
			host = new InetSocketAddress(origin.substring(slash + 1, colon),
					port);
		} else if (origin.startsWith("https:")) {
			int slash = origin.lastIndexOf('/');
			host = new InetSocketAddress(origin.substring(slash + 1), 443);
		} else {
			int slash = origin.lastIndexOf('/');
			host = new InetSocketAddress(origin.substring(slash + 1), 80);
		}
		return host;
	}

	private boolean createVirtualHost(String vhost, String origin,
			CallimachusRepository repository) throws RepositoryException, IOException {
		assert !vhost.endsWith("/");
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			URI subj = vf.createURI(vhost + '/');
			if (!checkSecret(subj, con))
				return false;
			if (con.hasStatement(subj, RDF.TYPE, vf.createURI(CALLI_ORIGIN))) {
				logger.info("Updating origin: {} for {}", vhost, origin);
			} else {
				logger.info("Adding origin: {} for {}", vhost, origin);
				add(con, subj, RDF.TYPE, origin + CALLIMACHUS + "Origin");
				add(con, subj, RDF.TYPE, CALLI_ORIGIN);
				con.add(subj, RDFS.LABEL, vf.createLiteral(getLabel(vhost)));
				addRealm(subj, origin, con);
			}
			con.setAutoCommit(true);
			return true;
		} finally {
			con.close();
		}
	}

	private boolean createRealm(String realm, String origin,
			CallimachusRepository repository) throws RepositoryException, IOException {
		assert realm.endsWith("/");
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			URI subj = vf.createURI(realm);
			if (!checkSecret(subj, con))
				return false;
			if (con.hasStatement(subj, RDF.TYPE, vf.createURI(CALLI_REALM))) {
				logger.info("Updating realm: {} for {}", realm, origin);
			} else {
				logger.info("Adding realm: {} for {}", realm, origin);
				con.add(subj, RDF.TYPE, vf.createURI(origin + CALLIMACHUS + "Realm"));
				con.add(subj, RDFS.LABEL, vf.createLiteral(getLabel(realm)));
				addRealm(subj, origin, con);
			}
			con.setAutoCommit(true);
			return true;
		} finally {
			con.close();
		}
	}

	private boolean checkSecret(URI subj, ObjectConnection con)
			throws RepositoryException, IOException {
		ValueFactory vf = con.getValueFactory();
		if (!con.hasStatement(subj, vf.createURI(CALLI_SECRET), null)) {
			URI secret = vf.createURI("urn:uuid:" + UUID.randomUUID());
			con.add(subj, vf.createURI(CALLI_SECRET), secret);
			byte[] bytes = new byte[1024];
			new SecureRandom().nextBytes(bytes);
			storeTextBlob(secret, Base64.encodeBase64String(bytes), con);
			return true;
		}
		return false;
	}

	private String getLabel(String origin) {
		String label = origin;
		if (label.endsWith("/")) {
			label = label.substring(0, label.length() - 1);
		}
		if (label.contains("://")) {
			label = label.substring(label.indexOf("://") + 3);
		}
		return label;
	}

	private void addRealm(URI subj, String origin, ObjectConnection con)
			throws RepositoryException {
		String c = origin + CALLIMACHUS;
		add(con, subj, RDF.TYPE, CALLI_REALM);
		add(con, subj, RDF.TYPE, CALLI_FOLDER);
		add(con, subj, CALLI_READER, origin + "/group/public");
		add(con, subj, CALLI_SUBSCRIBER, origin + "/group/everyone");
		add(con, subj, CALLI_CONTRIBUTOR, origin + "/group/users");
		add(con, subj, CALLI_EDITOR, origin + "/group/staff");
		add(con, subj, CALLI_ADMINISTRATOR, origin + "/group/admin");
		add(con, subj, CALLI_UNAUTHORIZED, c + "pages/unauthorized.xhtml?element=/1&realm=/");
		add(con, subj, CALLI_FORBIDDEN, c + "pages/forbidden.xhtml?element=/1&realm=/");
		add(con, subj, CALLI_AUTHENTICATION, origin + "/accounts");
		add(con, subj, CALLI_MENU, origin + "/main+menu");
		add(con, subj, CALLI_FAVICON, c + "images/callimachus-icon.ico");
		add(con, subj, CALLI_THEME, c + "theme/default");
	}

	private void add(ObjectConnection con, URI subj, URI pred, String resource)
			throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		con.add(subj, pred, vf.createURI(resource));
	}

	private void add(ObjectConnection con, URI subj, String pred,
			String resource) throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		con.add(subj, vf.createURI(pred), vf.createURI(resource));
	}

	private boolean setServeAllResourcesAs(String origin,
			CallimachusRepository repository) throws OpenRDFException, IOException {
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			boolean modified = false;
			URI file = origin == null ? null : vf.createURI(origin + SERVE_ALL);
			URI hasComponent = vf.createURI(CALLI_HASCOMPONENT);
			URI NamedGraph = vf.createURI("http://www.w3.org/ns/sparql-service-description#NamedGraph");
			RepositoryResult<Statement> stmts = con.getStatements(null, RDF.TYPE, NamedGraph);
			try {
				while (stmts.hasNext()) {
					Statement st = stmts.next();
					Resource serve = st.getSubject();
					if (serve.stringValue().endsWith(SERVE_ALL) && !serve.equals(file)) {
						logger.info("Other resources are no longer served publicly through {}", st.getSubject());
						con.clear(serve);
						con.remove((Resource) null, hasComponent, serve);
						modified = true;
					}
				}
			} finally {
				stmts.close();
			}
			ClassLoader cl = getClass().getClassLoader();
			InputStream in = cl.getResourceAsStream(SERVE_ALL_TTL);
			try {
				if (file != null && in != null && !con.hasStatement((Resource) null, hasComponent, file)) {
					logger.info("All other resources are now served publicly through {}", origin);
					OutputStream out = con.getBlobObject(file).openOutputStream();
					try {
						int read;
						byte[] buf = new byte[1024];
						while ((read = in.read(buf)) >= 0) {
							out.write(buf, 0, read);
						}
					} finally {
						out.close();
						in.close();
						in = con.getBlobObject(file).openInputStream();
					}
					con.add(in, file.stringValue(), RDFFormat.TURTLE, file);
					con.add(file, RDFS.LABEL, vf.createLiteral("everything else public"));
					con.add(file, RDF.TYPE, NamedGraph);
					con.add(file, RDF.TYPE, vf.createURI(origin + GRAPH_DOCUMENT));
					con.add(file, RDF.TYPE, vf.createURI("http://xmlns.com/foaf/0.1/Document"));
					con.add(file, vf.createURI(CALLI_READER), vf.createURI(origin + "/group/public"));
					con.add(file, vf.createURI(CALLI_SUBSCRIBER), vf.createURI(origin + "/group/staff"));
					con.add(file, vf.createURI(CALLI_ADMINISTRATOR), vf.createURI(origin + "/group/admin"));
					con.add(vf.createURI(origin + "/"), hasComponent, file);
					modified = true;
				}
			} finally {
				if (in != null) {
					in.close();
				}
			}
			con.setAutoCommit(true);
			return modified;
		} finally {
			con.close();
		}
	}

	private boolean createAdmin(String name, String email, String username,
			char[] password, String origin, CallimachusRepository repository)
			throws RepositoryException, IOException {
		validateName(username);
		validateEmail(email);
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			boolean modified = false;
			for (Statement st1 : con.getStatements(vf.createURI(origin + "/"),
					vf.createURI(CALLI_AUTHENTICATION), null).asList()) {
				Resource accounts = (Resource) st1.getObject();
				for (Statement st2 : con.getStatements(accounts,
						vf.createURI(CALLI_AUTHNAME), null).asList()) {
					String authName = st2.getObject().stringValue();
					String[] encoded = encodePassword(username, email,
							authName, password);
					for (Statement st3 : con.getStatements(accounts,
							vf.createURI(CALLI_AUTHNAMESPACE), null).asList()) {
						Resource user = (Resource) st3.getObject();
						URI subj = vf.createURI(user.stringValue() + username);
						modified |= changeAdminPassword(origin, user, subj,
								name, email, username, encoded, con);
					}
				}
			}
			con.setAutoCommit(true);
			return modified;
		} finally {
			con.close();
		}
	}

	private String[] encodePassword(String username, String email,
			String authName, char[] password) {
		if (password == null || password.length < 1)
			return null;
		String decodedUser = username + ":" + authName + ":" + String.valueOf(password);
		String encodedUser = DigestUtils.md5Hex(decodedUser);
		String decodedEmail = email + ":" + authName + ":" + String.valueOf(password);
		String encodedEmail = DigestUtils.md5Hex(decodedEmail);
		String[] encoded = new String[] {
				encodedUser, encodedEmail };
		return encoded;
	}

	private void validateName(String username) throws IllegalArgumentException,
			UnsupportedEncodingException {
		if (username == null || !username.toLowerCase().equals(username))
			throw new IllegalArgumentException("Username must be in lowercase");
		if (URLEncoder.encode(username, "UTF-8") != username)
			throw new IllegalArgumentException("Invalid username: '" + username
					+ "'");
	}

	private void validateEmail(String email) throws IllegalArgumentException,
			UnsupportedEncodingException {
		if (email == null || email.length() == 0)
			throw new IllegalArgumentException("email is required");
		if (!email.matches("[a-zA-Z0-9.!$%&*+/=?^_{}~-]+@[a-zA-Z0-9.-]+"))
			throw new IllegalArgumentException("Invalid email: '" + email
					+ "'");
	}

	private boolean changeAdminPassword(String origin, Resource folder,
			URI subj, String name, String email, String username,
			String[] encoded, ObjectConnection con) throws RepositoryException,
			IOException {
		ValueFactory vf = con.getValueFactory();
		URI calliEmail = vf.createURI(CALLI_EMAIL);
		if (con.hasStatement(subj, RDF.TYPE, vf.createURI(CALLI_USER))) {
			if (email != null && !con.hasStatement(subj, calliEmail, vf.createLiteral(email))) {
				logger.info("Changing email of {}", username);
				con.remove(subj, calliEmail, null);
				con.add(subj, calliEmail, vf.createLiteral(email));
			}
			if (encoded != null) {
				logger.info("Changing password of {}", username);
				setPassword(subj, encoded, con);
			}
		} else {
			logger.info("Creating user {}", username);
			URI staff = vf.createURI(origin + "/group/staff");
			URI admin = vf.createURI(origin + "/group/admin");
			con.add(subj, RDF.TYPE, vf.createURI(origin + CALLIMACHUS + "User"));
			con.add(subj, RDF.TYPE, vf.createURI(CALLI_PARTY));
			con.add(subj, RDF.TYPE, vf.createURI(CALLI_USER));
			con.add(subj, vf.createURI(CALLI_NAME), vf.createLiteral(username));
			if (encoded != null) {
				setPassword(subj, encoded, con);
			}
			if (name == null || name.length() == 0) {
				con.add(subj, RDFS.LABEL, vf.createLiteral(username));
			} else {
				con.add(subj, RDFS.LABEL, vf.createLiteral(name));
			}
			con.add(subj, vf.createURI(CALLI_SUBSCRIBER), staff);
			con.add(subj, vf.createURI(CALLI_ADMINISTRATOR), admin);
			con.add(folder, vf.createURI(CALLI_HASCOMPONENT), subj);
			con.add(admin, vf.createURI(CALLI_MEMBER), subj);
			if (email != null && email.length() > 2) {
				con.add(subj, calliEmail, vf.createLiteral(email));
			}
		}
		return true;
	}

	private void setPassword(URI subj, String[] encoded, ObjectConnection con)
			throws RepositoryException, IOException {
		ValueFactory vf = con.getValueFactory();
		con.remove(subj, vf.createURI(CALLI_ENCODED), null);
		con.remove(subj, vf.createURI(CALLI_ALGORITHM), null);
		int i = 0;
		for (URI uuid : getPasswordURI(subj, encoded.length, con)) {
			storeTextBlob(uuid, encoded[i++], con);
		}
	}

	private Collection<URI> getPasswordURI(URI subj, int count,
			ObjectConnection con) throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		List<Statement> passwords = con.getStatements(subj,
				vf.createURI(CALLI_PASSWORD), null).asList();
		if (passwords.size() == count) {
			Set<URI> list = new TreeSet<URI>(new ValueComparator());
			for (Statement st : passwords) {
				if (st.getObject() instanceof URI) {
					list.add((URI) st.getObject());
				}
			}
			if (list.size() == count)
				return list;
		}
		for (Statement st : passwords) {
			Value object = st.getObject();
			if (object instanceof URI) {
				con.getBlobObject((URI) object).delete();
			}
			con.remove(st);
		}
		Set<URI> list = new TreeSet<URI>(new ValueComparator());
		for (int i = 0; i < count; i++) {
			URI uuid = vf.createURI("urn:uuid:" + UUID.randomUUID());
			con.add(subj, vf.createURI(CALLI_PASSWORD), uuid);
			list.add(uuid);
		}
		return list;
	}

	private void storeTextBlob(URI uuid, String encoded, ObjectConnection con)
			throws RepositoryException, IOException {
		Writer writer = con.getBlobObject(uuid).openWriter();
		try {
			writer.write(encoded);
		} finally {
			writer.close();
		}
	}

}
