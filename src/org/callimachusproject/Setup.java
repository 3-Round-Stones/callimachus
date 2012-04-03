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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.codec.digest.DigestUtils;
import org.callimachusproject.io.CarInputStream;
import org.callimachusproject.server.CallimachusServer;
import org.callimachusproject.server.traits.ProxyObject;
import org.callimachusproject.server.util.ChannelUtil;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Graph;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.util.ModelUtil;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.Update;
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
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.store.blob.file.FileBlobStoreProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command line tool for setting up a new repository.
 * 
 * @author James Leigh
 * 
 */
public class Setup {
	private static final String CALLIMACHUS = "/callimachus/";
	private static final String SCHEMA_GRAPH = CALLIMACHUS + "SchemaGraph";
	private static final String SERVICEABLE = CALLIMACHUS + "Serviceable";
	public static final String NAME = Version.getInstance().getVersion();
	private static final String INITIAL_GRAPH = "META-INF/templates/callimachus-initial-data.ttl";
	private static final String MAIN_ARTICLE = "META-INF/templates/main-article.docbook";

	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_DESCRIBEDBY = CALLI + "describedBy";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";
	private static final String CALLI_ADMINISTRATOR = CALLI + "administrator";
	private static final String CALLI_EDITOR = CALLI + "editor";
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
	private static final String CALLI_MEMBER = CALLI + "member";

	private static final Options options = new Options();
	static {
		options.addOption("d", "dir", true,
				"Directory used for data storage and retrieval");
		options.addOption("c", "config", true,
				"A repository config url (relative file: or http:)");
		options.getOption("config").setRequired(true);
		options.addOption("a", "car", true,
				"The callimachus.car file to be installed in the origin");
		options.getOption("car").setRequired(true);
		options.addOption("o", "origin", true,
				"The scheme, hostname and port ( http://localhost:8080 ) that resolves to this server");
		options.getOption("origin").setRequired(true);
		options.addOption("v", "virtual", true,
				"Additional scheme, hostname and port ( http://localhost:8080 ) that resolves to this server");
		options.addOption("r", "realm", true,
				"The scheme, hostname, port, and path ( http://example.com:8080/ ) that does not resolve to this server");
		options.addOption("l", "serve-all", false, "Serve all resources");
		options.addOption("u", "user", true,
				"Create the given user and prompt for a password, or append the password separated by a colon");
		options.getOption("user").setOptionalArg(true);
		options.addOption("n", "name", true,
				"If creating a new user use this full name");
		options.addOption("e", "email", true,
				"If creating a new user use this email address");
		options.addOption("s", "silent", false,
				"If the repository is already setup exit successfully");
		options.addOption("h", "help", false,
				"Print help (this message) and exit");
		options.addOption("V", "version", false,
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
		e.printStackTrace();
	}

	private final Logger logger = LoggerFactory.getLogger(Setup.class);
	private ObjectRepository repository;
	private boolean silent;
	private String serveAllAs;
	private File dir;
	private URL config;
	private URL callimachus;
	private final Set<String> origins = new HashSet<String>();
	private final Map<String, String> vhosts = new HashMap<String, String>();
	private final Map<String, String> realms = new HashMap<String, String>();
	private LocalRepositoryManager manager;
	private String origin;
	private String name;
	private String email;
	private String username;
	private String password;

	public void init(String[] args) {
		try {
			CommandLine line = new GnuParser().parse(options, args);
			if (line.hasOption('h')) {
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("[options]", options);
				System.exit(0);
				return;
			} else if (line.getArgs().length > 0) {
				System.err.println("Unrecognized option: "
						+ Arrays.toString(line.getArgs()));
				System.err.println("Arguments: " + Arrays.toString(args));
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("[options]", options);
				System.exit(0);
				return;
			} else if (line.hasOption('V')) {
				System.out.println(NAME);
				System.exit(0);
				return;
			} else {
				silent = line.hasOption('s');
				if (line.hasOption('d')) {
					dir = new File(line.getOptionValue('d')).getCanonicalFile();
				} else {
					dir = new File("").getCanonicalFile();
				}
				if (line.hasOption('c')) {
					config = resolve(line.getOptionValue('c'));
				}
				if (line.hasOption('a')) {
					callimachus = resolve(line.getOptionValue('a'));
				}
				if (line.hasOption('o')) {
					for (String o : line.getOptionValues('o')) {
						validateOrigin(o);
						origins.add(o);
					}
					String origin = line.getOptionValue('o');
					if (line.hasOption('v')) {
						for (String v : line.getOptionValues('v')) {
							validateOrigin(v);
							vhosts.put(v, origin);
						}
					}
					if (line.hasOption('r')) {
						for (String r : line.getOptionValues('r')) {
							validateRealm(r);
							realms.put(r, origin);
						}
					}
					if (line.hasOption('l')) {
						serveAllAs = origin;
					}
					if (line.hasOption('u')) {
						String u = line.getOptionValue('u');
						if (u != null && u.contains(":")) {
							username = u.substring(0, u.indexOf(':'));
							password = u.substring(u.indexOf(':') + 1);
						} else {
							Reader reader = new InputStreamReader(System.in);
							if (u == null) {
								System.out.print("Enter a username: ");
								username = new BufferedReader(reader).readLine();
							} else {
								username = u;
							}
							System.out.print("Enter password for user ");
							System.out.print(u);
							System.out.print(": ");
							password = new BufferedReader(reader).readLine();
						}
						this.origin = origin;
						this.name = line.getOptionValue('n');
						this.email = line.getOptionValue('e');
					}
				}
			}
		} catch (Exception e) {
			println(e);
			System.err.println("Arguments: " + Arrays.toString(args));
			System.exit(2);
		}
	}

	public void start() throws Exception {
		String configString = readContent(config);
		System.err.println(connect(dir, configString).toURI().toASCIIString());
		boolean changed = false;
		for (String origin : origins) {
			changed |= createOrigin(origin, callimachus, repository);
		}
		for (Map.Entry<String, String> e : vhosts.entrySet()) {
			changed |= createVirtualHost(e.getKey(), e.getValue(), repository);
		}
		for (Map.Entry<String, String> e : realms.entrySet()) {
			changed |= createRealm(e.getKey(), e.getValue(), repository);
		}
		changed |= setServeAllResourcesAs(serveAllAs, repository);
		if (password != null) {
			changed |= createAdmin(name, email, username, password, origin, repository);
		}
		if (changed || silent) {
			System.exit(0);
		} else {
			logger.warn("Repository is already setup");
			System.exit(1); // already setup
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
		repository = getObjectRepository(dir, configString);
		if (repository == null)
			throw new RepositoryConfigException(
					"Missing repository configuration");
		RepositoryConfig config = getRepositoryConfig(configString);
		manager = RepositoryProvider.getRepositoryManager(dir);
		return manager.getRepositoryDir(config.getID());
	}

	public void disconnect() {
		repository = null;
		manager.shutDown();
	}

	public void createOrigin(String origin, URL car) throws Exception {
		validateOrigin(origin);
		if (repository == null)
			throw new IllegalStateException("Not connected");
		createOrigin(origin, car, repository);
	}

	public void createVirtualHost(String virtual, String origin)
			throws RepositoryException {
		validateOrigin(virtual);
		validateOrigin(origin);
		if (repository == null)
			throw new IllegalStateException("Not connected");
		createVirtualHost(virtual, origin, repository);
	}

	public void createRealm(String realm, String origin)
			throws RepositoryException {
		validateRealm(realm);
		validateOrigin(origin);
		if (repository == null)
			throw new IllegalStateException("Not connected");
		createRealm(realm, origin, repository);
	}

	public void setResourcesAsLocalTo(String origin) throws RepositoryException {
		if (repository == null)
			throw new IllegalStateException("Not connected");
		setServeAllResourcesAs(origin, repository);
	}

	public void createAdmin(String name, String email, String username, String password, String origin)
			throws RepositoryException, UnsupportedEncodingException {
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

	private ObjectRepository getObjectRepository(File dir, String configString)
			throws OpenRDFException, MalformedURLException, IOException {
		Repository repo = getRepository(dir, configString);
		if (repo == null)
			return null;
		if (repo instanceof ObjectRepository)
			return (ObjectRepository) repo;
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		ObjectRepositoryConfig config = factory.getConfig();
		File dataDir = repo.getDataDir();
		if (dataDir == null) {
			dataDir = dir;
		}
		File wwwDir = new File(dataDir, "www");
		File blobDir = new File(dataDir, "blob");
		if (wwwDir.isDirectory() && !blobDir.isDirectory()) {
			config.setBlobStore(wwwDir.toURI().toString());
			Map<String, String> map = new HashMap<String, String>();
			map.put("provider", FileBlobStoreProvider.class.getName());
			config.setBlobStoreParameters(map);
		} else {
			config.setBlobStore(blobDir.toURI().toString());
		}
		return factory.createRepository(config, repo);
	}

	private Repository getRepository(File dir, String configString)
			throws OpenRDFException, MalformedURLException, IOException {
		RepositoryConfig config = getRepositoryConfig(configString);
		LocalRepositoryManager manager = getRepositoryManager(dir);
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

	private boolean createOrigin(String origin, URL car,
			ObjectRepository repository) throws Exception {
		boolean modified = createVirtualHost(origin, origin, repository);
		modified |= initializeOrUpgradeStore(repository, origin);
		if (car != null) {
			importCallimachus(origin, car, repository);
		}
		return modified;
	}

	private boolean initializeOrUpgradeStore(ObjectRepository repository,
			String origin) throws RepositoryException, IOException,
			OpenRDFException {
		String version = getStoreVersion(repository, origin);
		if (version == null) {
			initializeStore(origin, repository);
			return true;
		} else {
			String newVersion = upgradeStore(repository, origin, version);
			return !version.equals(newVersion);
		}
	}

	private String getStoreVersion(ObjectRepository repository, String origin)
			throws RepositoryException {
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

	private void initializeStore(String origin, ObjectRepository repository)
			throws RepositoryException {
		try {
			ClassLoader cl = CallimachusServer.class.getClassLoader();
			loadDefaultGraphs(origin, repository, cl);
			uploadMainArticle(origin, repository, cl);
		} catch (IOException e) {
			logger.debug(e.toString(), e);
		}
	}

	private void loadDefaultGraphs(String origin, Repository repository,
			ClassLoader cl) throws IOException, RepositoryException {
		RDFFormat format = RDFFormat.forFileName(INITIAL_GRAPH,
				RDFFormat.RDFXML);
		Enumeration<URL> resources = cl.getResources(INITIAL_GRAPH);
		if (!resources.hasMoreElements())
			logger.warn("Missing {}", INITIAL_GRAPH);
		while (resources.hasMoreElements()) {
			InputStream in = resources.nextElement().openStream();
			try {
				RepositoryConnection con = repository.getConnection();
				try {
					logger.info("Initializing {} Store", origin);
					con.add(in, origin + "/", format);
				} finally {
					con.close();
				}
			} catch (RDFParseException exc) {
				logger.warn(exc.toString(), exc);
			}
		}
	}

	private void uploadMainArticle(String origin, ObjectRepository repository,
			ClassLoader cl) throws RepositoryException, IOException {
		ObjectConnection con = repository.getConnection();
		try {
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
					vf.createURI(origin + "/group/users"));
			con.add(article, vf.createURI(CALLI_EDITOR),
					vf.createURI(origin + "/group/staff"));
			con.add(article, vf.createURI(CALLI_ADMINISTRATOR),
					vf.createURI(origin + "/group/admin"));
			con.add(folder, vf.createURI(CALLI_HASCOMPONENT), article);
			con.add(folder, vf.createURI(CALLI_DESCRIBEDBY),
					vf.createURI(article.toString() + "?view"));
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
		} finally {
			con.close();
		}
	}

	private String upgradeStore(ObjectRepository repository, String origin,
			String version) throws IOException, OpenRDFException {
		ClassLoader cl = getClass().getClassLoader();
		String name = "META-INF/upgrade/callimachus-" + version + ".ru";
		InputStream in = cl.getResourceAsStream(name);
		if (in == null)
			return version;
		logger.info("Upgrading store from {}", version);
		Reader reader = new InputStreamReader(in, "UTF-8");
		String ru = IOUtil.readString(reader);
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			Update u = con.prepareUpdate(SPARQL, ru, origin);
			u.execute();
			con.setAutoCommit(true);
		} finally {
			con.close();
		}
		String newVersion = getStoreVersion(repository, origin);
		if (version != null && !version.equals(newVersion)) {
			logger.info("Upgraded store from {} to {}", version, newVersion);
		}
		if (!version.equals(newVersion))
			return upgradeStore(repository, origin, newVersion);
		return newVersion;
	}

	private void importCallimachus(String origin, URL car,
			ObjectRepository repository) throws Exception {
		importSchema(car, origin, repository);
		importArchive(car, origin, repository);
	}

	private void importSchema(URL car, String origin,
			ObjectRepository repository) throws RepositoryException,
			IOException, RDFParseException {
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			RepositoryResult<Statement> stmts;
			stmts = con.getStatements(null, RDF.TYPE, vf.createURI(origin + SCHEMA_GRAPH));
			try {
				while (stmts.hasNext()) {
					Resource graph = stmts.next().getSubject();
					if (graph.stringValue().startsWith(origin + CALLIMACHUS)) {
						con.clear(graph);
					}
				}
			} finally {
				stmts.close();
			}
			CarInputStream carin = new CarInputStream(car.openStream());
			try {
				while (carin.readEntryName() != null) {
					importSchemaGraphEntry(carin, origin, con);
				}
			} finally {
				carin.close();
			}
			con.setAutoCommit(true);
		} finally {
			con.close();
		}
	}

	private void importSchemaGraphEntry(CarInputStream carin, String origin,
			ObjectConnection con) throws IOException, RDFParseException,
			RepositoryException {
		ValueFactory vf = con.getValueFactory();
		String target = origin + CALLIMACHUS + carin.readEntryName();
		InputStream in = carin.getEntryStream();
		try {
			if (carin.isSchemaEntry()) {
				String graph = target + ".owl";
				con.add(in, graph, RDFFormat.RDFXML, vf.createURI(graph));
			} else if (carin.isFileEntry()) {
				if (carin.getEntryType().startsWith("application/rdf+xml")) {
					con.add(in, target, RDFFormat.RDFXML, vf.createURI(target));
				} else if (carin.getEntryType().startsWith("text/turtle")) {
					con.add(in, target, RDFFormat.TURTLE, vf.createURI(target));
				} else {
					byte[] buf = new byte[1024];
					while (in.read(buf) >= 0)
						;
				}
			} else {
				byte[] buf = new byte[1024];
				while (in.read(buf) >= 0)
					;
			}
		} finally {
			in.close();
		}
	}

	private void importArchive(URL car, String origin,
			ObjectRepository repository) throws Exception {
		ValueFactory vf = repository.getValueFactory();
		repository.setSchemaGraphType(vf.createURI(origin + SCHEMA_GRAPH));
		repository.setCompileRepository(true);
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			URI uri = vf.createURI(origin + CALLIMACHUS);
			con.add(vf.createURI(origin + "/"), vf.createURI(CALLI_HASCOMPONENT), uri);
			con.add(uri, RDF.TYPE, vf.createURI(CALLI_FOLDER));
			con.add(uri, RDF.TYPE, vf.createURI(origin + CALLIMACHUS + "Folder"));
			con.add(uri, RDFS.LABEL, vf.createLiteral("callimachus"));

			Object folder = con.getObject(uri);
			String auth = java.net.URI.create(origin + "/").getAuthority();
			((ProxyObject) folder).setLocalAuthority(auth);
			Method UploadFolderComponents = folder.getClass().getMethod(
					"UploadFolderComponents", InputStream.class);
			InputStream in = car.openStream();
			try {
				UploadFolderComponents.invoke(folder, in);
			} finally {
				in.close();
			}
			repository.setCompileRepository(false);
			con.setAutoCommit(true);
		} finally {
			con.close();
		}
	}

	private boolean createVirtualHost(String vhost, String origin,
			ObjectRepository repository) throws RepositoryException {
		assert !vhost.endsWith("/");
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			URI subj = vf.createURI(vhost + '/');
			if (con.hasStatement(subj, RDF.TYPE, vf.createURI(CALLI_ORIGIN)))
				return false;
			logger.info("Adding origin: {} for {}", vhost, origin);
			con.add(subj, RDF.TYPE,
					vf.createURI(origin + CALLIMACHUS + "Origin"));
			con.add(subj, RDFS.LABEL, vf.createLiteral(getLabel(vhost)));
			con.add(subj, RDF.TYPE, vf.createURI(CALLI_ORIGIN));
			addRealm(subj, origin, con);
			return true;
		} finally {
			con.close();
		}
	}

	private boolean createRealm(String realm, String origin,
			ObjectRepository repository) throws RepositoryException {
		assert realm.endsWith("/");
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			URI subj = vf.createURI(realm);
			if (con.hasStatement(subj, RDF.TYPE, vf.createURI(CALLI_REALM)))
				return false;
			logger.info("Adding realm: {} for {}", realm, origin);
			con.add(subj, RDF.TYPE, vf.createURI(origin + CALLIMACHUS + "Realm"));
			con.add(subj, RDFS.LABEL, vf.createLiteral(getLabel(realm)));
			addRealm(subj, origin, con);
			return true;
		} finally {
			con.close();
		}
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
		ValueFactory vf = con.getValueFactory();
		con.add(subj, RDF.TYPE, vf.createURI(CALLI_REALM));
		con.add(subj, RDF.TYPE, vf.createURI(CALLI_FOLDER));
		con.add(subj, vf.createURI(CALLI_READER),
				vf.createURI(origin + "/group/users"));
		con.add(subj, vf.createURI(CALLI_EDITOR),
				vf.createURI(origin + "/group/staff"));
		con.add(subj, vf.createURI(CALLI_ADMINISTRATOR),
				vf.createURI(origin + "/group/admin"));
		con.add(subj, vf.createURI(CALLI_UNAUTHORIZED),
				vf.createURI(origin + CALLIMACHUS + "pages/unauthorized.xhtml"));
		con.add(subj, vf.createURI(CALLI_FORBIDDEN),
				vf.createURI(origin + CALLIMACHUS + "pages/forbidden.xhtml"));
		con.add(subj, vf.createURI(CALLI_AUTHENTICATION),
				vf.createURI(origin + "/accounts"));
		con.add(subj, vf.createURI(CALLI_MENU),
				vf.createURI(origin + "/main+menu"));
		con.add(subj,
				vf.createURI(CALLI_FAVICON),
				vf.createURI(origin
						+ CALLIMACHUS + "images/callimachus-icon.ico"));
		con.add(subj, vf.createURI(CALLI_THEME),
				vf.createURI(origin + CALLIMACHUS + "theme/default"));
	}

	private boolean setServeAllResourcesAs(String origin, ObjectRepository repository) throws RepositoryException {
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			boolean modified = false;
			URI ableUri = origin == null ? null : vf.createURI(origin + SERVICEABLE);
			RepositoryResult<Statement> stmts = con.getStatements(null, OWL.EQUIVALENTCLASS, RDFS.RESOURCE);
			try {
				while (stmts.hasNext()) {
					Statement st = stmts.next();
					String serve = st.getSubject().stringValue();
					if (serve.endsWith(SERVICEABLE) && !serve.equals(ableUri)) {
						logger.info("All resources are no longer served as {}", st.getSubject());
						con.remove(st, st.getContext());
						String schemaGraph = serve.substring(0, serve.lastIndexOf(SERVICEABLE)) + SCHEMA_GRAPH;
						con.remove(st.getContext(), RDF.TYPE, vf.createURI(schemaGraph));
						modified = true;
					}
				}
			} finally {
				stmts.close();
			}
			if (ableUri != null && !con.hasStatement(ableUri, OWL.EQUIVALENTCLASS, RDFS.RESOURCE)) {
				logger.info("All resources are now served as {}", ableUri);
				con.add(ableUri, OWL.EQUIVALENTCLASS, RDFS.RESOURCE, ableUri);
				con.add(ableUri, RDF.TYPE, vf.createURI(origin + SCHEMA_GRAPH));
				return true;
			}
			return modified;
		} finally {
			con.close();
		}
	}

	private boolean createAdmin(String name, String email, String username, String password,
			String origin, ObjectRepository repository)
			throws UnsupportedEncodingException, RepositoryException {
		if (username == null || !username.toLowerCase().equals(username))
			throw new IllegalArgumentException("Username must be in lowercase");
		if (URLEncoder.encode(username, "UTF-8") != username)
			throw new IllegalArgumentException("Invalid username: '" + username
					+ "'");
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			ObjectFactory of = con.getObjectFactory();
			boolean modified = false;
			for (Statement st1 : con.getStatements(vf.createURI(origin + "/"),
					vf.createURI(CALLI_AUTHENTICATION), null).asList()) {
				Resource accounts = (Resource) st1.getObject();
				for (Statement st2 : con.getStatements(accounts,
						vf.createURI(CALLI_AUTHNAME), null).asList()) {
					String authName = st2.getObject().stringValue();
					String decoded = username + ":" + authName + ":" + password;
					byte[] encoded = DigestUtils.md5(decoded);
					Literal lit = of.createLiteral(encoded);
					for (Statement st3 : con.getStatements(accounts,
							vf.createURI(CALLI_AUTHNAMESPACE), null).asList()) {
						Resource user = (Resource) st3.getObject();
						URI subj = vf.createURI(user.stringValue() + username);
						modified |= changeAdminPassword(origin, user, subj,
								name, email, username, lit, con);
					}
				}
			}
			return modified;
		} finally {
			con.close();
		}
	}

	private boolean changeAdminPassword(String origin, Resource folder, URI subj,
			String name, String email, String username, Literal encoded, ObjectConnection con)
			throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		if (con.hasStatement(subj, vf.createURI(CALLI_ENCODED), encoded))
			return false;
		if (con.hasStatement(subj, RDF.TYPE, vf.createURI(CALLI_USER))) {
			logger.info("Changing password of {}", username);
			con.remove(subj, vf.createURI(CALLI_ENCODED), null);
			con.add(subj, vf.createURI(CALLI_ENCODED), encoded);
		} else {
			logger.info("Creating user {}", username);
			URI staff = vf.createURI(origin + "/group/staff");
			URI admin = vf.createURI(origin + "/group/admin");
			con.add(subj, RDF.TYPE, vf.createURI(origin + CALLIMACHUS + "User"));
			con.add(subj, RDF.TYPE, vf.createURI(CALLI_PARTY));
			con.add(subj, RDF.TYPE, vf.createURI(CALLI_USER));
			con.add(subj, vf.createURI(CALLI_NAME), vf.createLiteral(username));
			con.add(subj, vf.createURI(CALLI_ALGORITHM),
					vf.createLiteral("MD5"));
			con.add(subj, vf.createURI(CALLI_ENCODED), encoded);
			if (name == null || name.length() == 0) {
				con.add(subj, RDFS.LABEL, vf.createLiteral(username));
			} else {
				con.add(subj, RDFS.LABEL, vf.createLiteral(name));
			}
			con.add(subj, vf.createURI(CALLI_READER), staff);
			con.add(subj, vf.createURI(CALLI_ADMINISTRATOR), admin);
			con.add(folder, vf.createURI(CALLI_HASCOMPONENT), subj);
			con.add(admin, vf.createURI(CALLI_MEMBER), subj);
			if (email != null && email.length() > 2) {
				con.add(subj, vf.createURI(CALLI_EMAIL), vf.createLiteral(email));
			}
		}
		return true;
	}

}
