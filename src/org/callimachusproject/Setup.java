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
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpHost;
import org.callimachusproject.cli.Command;
import org.callimachusproject.cli.CommandSet;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.client.UnavailableHttpClient;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.io.CarInputStream;
import org.callimachusproject.server.CallimachusRepository;
import org.callimachusproject.setup.UpdateProvider;
import org.callimachusproject.setup.Updater;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Graph;
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
	private static final String GROUP_ADMIN = "/auth/groups/admin";
	private static final String GROUP_STAFF = "/auth/groups/staff";
	private static final String GROUP_PUBLIC = "/auth/groups/public";
	public static final String NAME = Version.getInstance().getVersion();
	private static final String CHANGES_PATH = "../changes/";
	private static final String REALM_TYPE = "types/Realm";
	private static final String ORIGIN_TYPE = "types/Origin";
	private static final String USER_TYPE = "types/User";
	private static final String FOLDER_TYPE = "types/Folder";

	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";
	private static final String CALLI_ADMINISTRATOR = CALLI + "administrator";
	private static final String CALLI_SUBSCRIBER = CALLI + "subscriber";
	private static final String CALLI_READER = CALLI + "reader";
	private static final String CALLI_FOLDER = CALLI + "Folder";
	private static final String CALLI_AUTHENTICATION = CALLI + "authentication";
	private static final String CALLI_ENCODED = CALLI + "encoded";
	private static final String CALLI_AUTHNAME = CALLI + "authName";
	private static final String CALLI_AUTHNAMESPACE = CALLI + "authNamespace";
	private static final String CALLI_USER = CALLI + "User";
	private static final String CALLI_PARTY = CALLI + "Party";
	private static final String CALLI_NAME = CALLI + "name";
	private static final String CALLI_EMAIL = CALLI + "email";
	private static final String CALLI_ALGORITHM = CALLI + "algorithm";
	private static final String CALLI_PASSWORD = CALLI + "passwordDigest";
	private static final String CALLI_MEMBER = CALLI + "member";

	private static final CommandSet commands = new CommandSet(NAME);
	static {
		commands.option("d", "dir").arg("directory").desc("Directory used for data storage and retrieval");
		commands.require("c", "config").arg("file").desc(
				"A repository config url (relative file: or http:)");
		commands.option("f", "car").arg("file").desc(
				"Target and CAR URL pairs, separated by equals, that should be installed for each primary origin");
		commands.option("w", "webapp").arg("file").desc(
				"Callimachus webapp CAR URL, the relative URL of the callimachus webapp CAR, that should be installed for each origin");
		commands.require("o", "origin").arg("http").desc(
				"The scheme, hostname and port ( http://localhost:8080 ) that resolves to this server");
		commands.option("v", "virtual").arg("http").desc(
				"Additional scheme, hostname and port ( http://localhost:8080 ) that resolves to this server");
		commands.option("r", "realm").arg("http").desc(
				"The scheme, hostname, port, and path ( http://example.com:8080/ ) that does not resolve to this server");
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
	private final ServiceLoader<UpdateProvider> updateProviders = ServiceLoader
			.load(UpdateProvider.class, getClass().getClassLoader());
	private final Map<String,TermFactory> webapps = new HashMap<String, TermFactory>();
	private CallimachusRepository repository;
	private ValueFactory vf;
	private boolean silent;
	private File dir;
	private URL config;
	private URL webappCar;
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
					if (line.has("webapp")) {
						webappCar = resolve(line.get("webapp"));
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
								password = console.readPassword("Enter a new password for %s: ", username);
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
			changed |= createOrigin(origin);
		}
		for (Map.Entry<String, String> e : vhosts.entrySet()) {
			changed |= createVirtualHost(e.getKey(), e.getValue());
		}
		for (Map.Entry<String, String> e : realms.entrySet()) {
			changed |= createRealm(e.getKey(), e.getValue());
		}
		if (webappCar != null) {
			for (String origin : origins) {
				changed |= importCallimachusWebapp(webappCar, origin);
			}
		}
		for (Map.Entry<String, URL> e : cars.entrySet()) {
			String origin = origins.iterator().next();
			for (String o : origins) {
				if (e.getKey().startsWith(o + "/")) {
					origin = o;
				}
			}
			changed |= importCar(e.getValue(), e.getKey(), origin);
		}
		if (email != null && email.length() > 0) {
			for (String origin : origins) {
				changed |= createAdmin(name, email, username, password, origin);
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
		vf = repository.getValueFactory();
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

	public boolean createOrigin(String origin) throws Exception {
		validateOrigin(origin);
		if (repository == null)
			throw new IllegalStateException("Not connected");
		boolean barren = webappIfPresent(origin) == null;
		if (barren) {
			// (new) origin does not (yet) have a Callimachus webapp folder
			synchronized (webapps) {
				webapps.put(origin, TermFactory.newInstance(createWebappUrl(origin)));
			}
		}
		repository.setChangeFolder(webapp(origin, CHANGES_PATH).stringValue());
		boolean modified = createVirtualHost(origin, origin);
		if (!barren) {
			String version = getStoreVersion(origin);
			String newVersion = upgradeStore(origin);
			modified |= !newVersion.equals(version);
		}
		Iterator<UpdateProvider> iter = updateProviders.iterator();
		while (iter.hasNext()) {
			Updater updater = iter.next().updateCallimachusWebapp(origin);
			if (updater != null) {
				String webapp1 = webapp(origin, "").stringValue();
				modified |= updater.update(webapp1, repository);
				updateWebappContext(origin);
			}
		}
		return modified;
	}

	public boolean importCallimachusWebapp(URL car, String origin) throws Exception {
		String folder = repository.getCallimachusUrl(origin, "");
		if (folder == null)
			throw new IllegalArgumentException("Origin not setup: " + origin);
		return importCar(car, folder, origin);
	}

	public boolean importCar(URL car, String folder, String origin)
			throws Exception {
		validateOrigin(origin);
		if (repository == null)
			throw new IllegalStateException("Not connected");
		if (car == null)
			throw new IllegalArgumentException("No CAR provided");
		createFolder(folder, origin);
		URI[] schemaGraphs = importSchema(car, folder, origin);
		importArchive(schemaGraphs, car, folder, origin);
		return true;
	}

	public boolean createVirtualHost(String virtual, String origin)
			throws OpenRDFException, IOException {
		validateOrigin(virtual);
		validateOrigin(origin);
		if (repository == null)
			throw new IllegalStateException("Not connected");
		boolean modified = false;
		Iterator<UpdateProvider> iter = updateProviders.iterator();
		while (iter.hasNext()) {
			Updater updater = iter.next().updateOrigin(virtual);
			if (updater != null) {
				String webapp = webapp(origin, "").stringValue();
				modified |= updater.update(webapp, repository);
			}
		}
		return modified | createRealm(virtual + "/", origin);
	}

	public boolean createRealm(String realm, String origin)
			throws OpenRDFException, IOException {
		validateRealm(realm);
		validateOrigin(origin);
		if (repository == null)
			throw new IllegalStateException("Not connected");
		boolean modified = false;
		Iterator<UpdateProvider> iter = updateProviders.iterator();
		while (iter.hasNext()) {
			Updater updater = iter.next().updateRealm(realm);
			if (updater != null) {
				String webapp = webapp(origin, "").stringValue();
				modified |= updater.update(webapp, repository);
			}
		}
		return modified;
	}

	public boolean createAdmin(String name, String email, String username,
			char[] password, String origin) throws OpenRDFException,
			IOException {
		if (repository == null)
			throw new IllegalStateException("Not connected");
		validateName(username);
		validateEmail(email);
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			boolean modified = false;
			for (String[] row : getAuthNamesAndNamespaces(origin, con)) {
				String authName = row[0];
				String user = row[1];
				String[] encoded = encodePassword(username, email, authName,
						password);
				URI subj = vf.createURI(user + username);
				modified |= changeAdminPassword(origin, vf.createURI(user),
						subj, name, email, username, encoded, con);
			}
			con.setAutoCommit(true);
			return modified;
		} finally {
			con.close();
		}
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

	private String createWebappUrl(String origin) throws IOException {
		Iterator<UpdateProvider> iter = updateProviders.iterator();
		while (iter.hasNext()) {
			String webapp = iter.next().getDefaultCallimachusWebappLocation(origin);
			if (webapp != null)
				return webapp;
		}
		throw new AssertionError("Cannot determine Callimachus webapp folder");
	}

	private String getStoreVersion(String origin) throws OpenRDFException {
		ObjectConnection con = repository.getConnection();
		try {
			RepositoryResult<Statement> stmts;
			stmts = con.getStatements(webapp(origin, "../ontology"),
					OWL.VERSIONINFO, null);
			try {
				if (stmts.hasNext()) {
					String value = stmts.next().getObject().stringValue();
					return value;
				}
			} finally {
				stmts.close();
			}
			stmts = con.getStatements(webapp(origin, "/callimachus"),
					OWL.VERSIONINFO, null);
			try {
				if (stmts.hasNext()) {
					String value = stmts.next().getObject().stringValue();
					return value;
				}
			} finally {
				stmts.close();
			}
		} finally {
			con.close();
		}
		return null;
	}

	private String upgradeStore(String origin)
			throws IOException, OpenRDFException {
		String version = getStoreVersion(origin);
		Iterator<UpdateProvider> iter = updateProviders.iterator();
		while (iter.hasNext()) {
			Updater updater = iter.next().updateFrom(origin, version);
			if (updater != null) {
				String webapp = webapp(origin, "").stringValue();
				updater.update(webapp, repository);
				updateWebappContext(origin);
			}
		}
		String newVersion = getStoreVersion(origin);
		if (version != null && !version.equals(newVersion)) {
			logger.info("Upgraded store from {} to {}", version, newVersion);
			return upgradeStore(origin);
		}
		return newVersion;
	}

	private void updateWebappContext(String origin) throws OpenRDFException {
		synchronized (webapps) {
			webapps.clear();
		}
		repository.setChangeFolder(webapp(origin, CHANGES_PATH).stringValue());
	}

	private URI[] importSchema(URL car, String folder, String origin) throws RepositoryException,
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
				URI graph = con.getVersionBundle();
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
			String folderUri, String origin)
			throws Exception {
		HttpHost host = getAuthorityAddress(origin);
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		UnavailableHttpClient service = new UnavailableHttpClient();
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

	private boolean createFolder(String folder, String origin) throws OpenRDFException {
		boolean modified = false;
		int idx = folder.lastIndexOf('/', folder.length() - 2);
		String parent = folder.substring(0, idx + 1);
		if (parent.endsWith("://")) {
			parent = null;
		} else {
			modified = createFolder(parent, origin);
		}
		ValueFactory vf = repository.getValueFactory();
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			URI uri = vf.createURI(folder);
			if (con.hasStatement(uri, RDF.TYPE, webapp(origin, ORIGIN_TYPE)))
				return modified;
			if (con.hasStatement(uri, RDF.TYPE, webapp(origin, REALM_TYPE)))
				return modified;
			if (con.hasStatement(uri, RDF.TYPE, webapp(origin, FOLDER_TYPE)))
				return modified;
			if (parent == null)
				throw new IllegalStateException("Can only import a CAR within a previously defined origin or realm");
			if (con.hasStatement(vf.createURI(parent), vf.createURI(CALLI_HASCOMPONENT), uri))
				return modified;
			con.add(vf.createURI(parent), vf.createURI(CALLI_HASCOMPONENT), uri);
			String label = folder.substring(parent.length()).replace("/", "").replace('-', ' ');
			con.add(uri, RDF.TYPE, vf.createURI(CALLI_FOLDER));
			con.add(uri, RDF.TYPE, webapp(origin, FOLDER_TYPE));
			con.add(uri, RDFS.LABEL, vf.createLiteral(label));
			add(con, uri, CALLI_READER, origin + GROUP_PUBLIC);
			add(con, uri, CALLI_ADMINISTRATOR, origin + GROUP_ADMIN);
			con.setAutoCommit(true);
			return true;
		} finally {
			con.close();
		}
	}

	private HttpHost getAuthorityAddress(String origin) {
		HttpHost host;
		String scheme = "http";
		if (origin.startsWith("https:")) {
			scheme = "https";
		}
		if (origin.indexOf(':') != origin.lastIndexOf(':')) {
			int slash = origin.lastIndexOf('/');
			int colon = origin.lastIndexOf(':');
			int port = Integer.parseInt(origin.substring(colon + 1));
			host = new HttpHost(origin.substring(slash + 1, colon), port,
					scheme);
		} else if (origin.startsWith("https:")) {
			int slash = origin.lastIndexOf('/');
			host = new HttpHost(origin.substring(slash + 1), 443, scheme);
		} else {
			int slash = origin.lastIndexOf('/');
			host = new HttpHost(origin.substring(slash + 1), 80, scheme);
		}
		return host;
	}

	private void add(ObjectConnection con, URI subj, String pred,
			String resource) throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		con.add(subj, vf.createURI(pred), vf.createURI(resource));
	}

	private List<String[]> getAuthNamesAndNamespaces(String origin,
			ObjectConnection con) throws RepositoryException {
		List<String[]> list = new ArrayList<String[]>();
		ValueFactory vf = con.getValueFactory();
		for (Statement st1 : con.getStatements(vf.createURI(origin + "/"),
				vf.createURI(CALLI_AUTHENTICATION), null).asList()) {
			Resource accounts = (Resource) st1.getObject();
			for (Statement st2 : con.getStatements(accounts,
					vf.createURI(CALLI_AUTHNAME), null).asList()) {
				String authName = st2.getObject().stringValue();
				for (Statement st3 : con.getStatements(accounts,
						vf.createURI(CALLI_AUTHNAMESPACE), null).asList()) {
					String ns = st3.getObject().stringValue();
					list.add(new String[] { authName, ns });
				}
			}
		}
		return list;
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
			String[] encoded, ObjectConnection con) throws OpenRDFException,
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
			URI staff = vf.createURI(origin + GROUP_STAFF);
			URI admin = vf.createURI(origin + GROUP_ADMIN);
			con.add(subj, RDF.TYPE, webapp(origin, USER_TYPE));
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

	private URI webapp(String origin, String path) throws OpenRDFException {
		synchronized (webapps) {
			if (webapps.containsKey(origin))
				return vf.createURI(webapps.get(origin).resolve(path));
			String webapp = repository.getCallimachusUrl(origin, "");
			if (webapp == null) {
				webapp = webappIfPresent(origin);
				if (webapp == null)
					throw new IllegalStateException("Origin has not yet been created: " + origin);
			}
			TermFactory tf = TermFactory.newInstance(webapp);
			webapps.put(origin, tf);
			return vf.createURI(tf.resolve(path));
		}
	}

	private String webappIfPresent(String origin) throws OpenRDFException {
		// check >=1.0 webapp context
		String webapp = repository.getCallimachusUrl(origin, "");
		if (webapp == null) {
			// check <1.0 webapp context
			String root = origin + "/";
			RepositoryConnection con = repository.getConnection();
			try {
				ValueFactory vf = con.getValueFactory();
				RepositoryResult<Statement> stmts;
				stmts = con
						.getStatements(vf.createURI(root), RDF.TYPE, null, false);
				try {
					while (stmts.hasNext()) {
						String type = stmts.next().getObject().stringValue();
						if (type.startsWith(root) && type.endsWith("/Origin")) {
							int end = type.length() - "/Origin".length();
							return type.substring(0, end + 1);
						}
					}
				} finally {
					stmts.close();
				}
			} finally {
				con.close();
			}
			return null;
		}
		return webapp;
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
