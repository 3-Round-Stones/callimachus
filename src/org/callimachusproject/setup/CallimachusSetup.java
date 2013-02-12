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
package org.callimachusproject.setup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIUtils;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.client.UnavailableHttpClient;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.io.CarInputStream;
import org.callimachusproject.repository.CalliRepository;
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
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
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
import org.openrdf.repository.object.exceptions.RDFObjectException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.store.blob.BlobObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tool for setting up a new repository.
 * 
 * @author James Leigh
 * 
 */
public class CallimachusSetup {

	public static void validateOrigin(String origin) {
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

	public static void validateRealm(String realm) {
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

	public static void validateName(String username) throws IllegalArgumentException,
			UnsupportedEncodingException {
		if (username == null || !username.toLowerCase().equals(username))
			throw new IllegalArgumentException("Username must be in lowercase");
		if (URLEncoder.encode(username, "UTF-8") != username)
			throw new IllegalArgumentException("Invalid username: '" + username
					+ "'");
	}

	public static void validateEmail(String email) throws IllegalArgumentException,
			UnsupportedEncodingException {
		if (email == null || email.length() == 0)
			throw new IllegalArgumentException("email is required");
		if (!email.matches("[a-zA-Z0-9.!$%&*+/=?^_{}~-]+@[a-zA-Z0-9.-]+"))
			throw new IllegalArgumentException("Invalid email: '" + email
					+ "'");
	}

	private static final String SCHEMA_GRAPH = "types/SchemaGraph";
	private static final String GROUP_ADMIN = "/auth/groups/admin";
	private static final String GROUP_STAFF = "/auth/groups/staff";
	private static final String GROUP_PUBLIC = "/auth/groups/public";
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
	private static final String CALLI_AUTHNAMESPACE = CALLI + "authNamespace";
	private static final String CALLI_USER = CALLI + "User";
	private static final String CALLI_PARTY = CALLI + "Party";
	private static final String CALLI_NAME = CALLI + "name";
	private static final String CALLI_EMAIL = CALLI + "email";
	private static final String CALLI_MEMBER = CALLI + "member";
	private static final String CALLI_PASSWORD = CALLI + "passwordDigest";
	private static final String CALLI_AUTHNAME = CALLI + "authName";
	private static final String CALLI_SECRET = CALLI + "secret";

	private static final String RESOURCE_STRSTARTS = "SELECT ?resource { ?resource ?p ?o FILTER strstarts(str(?resource), str(<>)) }";
	private static final String GRAPH_STRSTARTS = "SELECT ?graph { GRAPH ?graph { ?s ?p ?o } FILTER strstarts(str(?graph), str(<>)) }";

	private final Logger logger = LoggerFactory.getLogger(CallimachusSetup.class);
	private final ServiceLoader<UpdateProvider> updateProviders = ServiceLoader
			.load(UpdateProvider.class, getClass().getClassLoader());
	private final Map<String,TermFactory> webapps = new HashMap<String, TermFactory>();
	private final CalliRepository repository;
	private final ValueFactory vf;
	private final LocalRepositoryManager manager;

	public CallimachusSetup(File baseDir, String configString)
			throws OpenRDFException, IOException {
		RepositoryConfig config = getRepositoryConfig(configString);
		manager = getRepositoryManager(baseDir);
		Repository repo = getRepository(manager, config);
		if (repo == null)
			throw new RepositoryConfigException(
					"Missing repository configuration");
		File dataDir = repo.getDataDir();
		if (dataDir == null) {
			dataDir = manager.getRepositoryDir(config.getID());
		}
		repository = new CalliRepository(repo, dataDir);
		vf = repository.getValueFactory();
	}

	public CallimachusSetup(LocalRepositoryManager manager, String configString)
			throws OpenRDFException, IOException {
		RepositoryConfig config = getRepositoryConfig(configString);
		Repository repo = getRepository(manager, config);
		if (repo == null)
			throw new RepositoryConfigException(
					"Missing repository configuration");
		File dataDir = repo.getDataDir();
		if (dataDir == null) {
			dataDir = manager.getRepositoryDir(config.getID());
		}
		repository = new CalliRepository(repo, dataDir);
		vf = repository.getValueFactory();
		this.manager = null;
	}

	public CallimachusSetup(Repository repo, File dataDir)
			throws OpenRDFException, IOException {
		repository = new CalliRepository(repo, dataDir);
		vf = repository.getValueFactory();
		this.manager = null;
	}

	/**
	 * If this object was created from a baseDir, shutdown the internal repository manager.
	 */
	public void shutDown() {
		if (manager != null) {
			manager.shutDown();
		}
	}

	public RepositoryConnection openConnection() throws RepositoryException {
		return repository.getConnection();
	}

	/**
	 * Remove the Callimachus webapp currently installed in <code>origin</code>.
	 * 
	 * @param origin
	 * @return <code>true</code> if the webapp was successfully removed
	 * @throws OpenRDFException
	 */
	public boolean clearCallimachusWebapp(String origin) throws OpenRDFException {
		if (webappIfPresent(origin) == null)
			return false;
		return deleteComponents(origin) | removeAllComponents(origin);
	}

	/**
	 * Creates an origin or updates its for a new Callimachus webapp.
	 * 
	 * @param origin will contain a Callimachus webapp
	 * @return if the RDF store was modified
	 * @throws OpenRDFException 
	 * @throws IOException 
	 * @throws Exception
	 */
	public boolean createWebappOrigin(String origin) throws OpenRDFException, IOException {
		validateOrigin(origin);
		boolean barren = webappIfPresent(origin) == null;
		if (barren) {
			// (new) origin does not (yet) have a Callimachus webapp folder
			synchronized (webapps) {
				webapps.put(origin, TermFactory.newInstance(createWebappUrl(origin)));
			}
		}
		repository.setChangeFolder(webapp(origin, CHANGES_PATH).stringValue());
		boolean modified = createOrigin(origin, origin);
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

	/**
	 * Create or updates an origin.
	 * 
	 * @param origin scheme and authority for the origin
	 * @param webappOrigin origin that will contain a Callimachus webapp
	 * @return if the RDF store was modified
	 * @throws OpenRDFException
	 * @throws IOException
	 */
	public boolean createOrigin(String origin, String webappOrigin)
			throws OpenRDFException, IOException {
		validateOrigin(origin);
		validateOrigin(webappOrigin);
		boolean modified = false;
		Iterator<UpdateProvider> iter = updateProviders.iterator();
		while (iter.hasNext()) {
			Updater updater = iter.next().updateOrigin(origin);
			if (updater != null) {
				String webapp = webapp(webappOrigin, "").stringValue();
				modified |= updater.update(webapp, repository);
			}
		}
		return modified | createRealm(origin + "/", webappOrigin);
	}

	/**
	 * Creates or updates a root realm.
	 * 
	 * @param realm absolute hierarchical URI with a path
	 * @param webappOrigin origin that will contain a Callimachus webapp
	 * @return if the RDF store was modified
	 * @throws OpenRDFException
	 * @throws IOException
	 */
	public boolean createRealm(String realm, String webappOrigin)
			throws OpenRDFException, IOException {
		validateRealm(realm);
		validateOrigin(webappOrigin);
		boolean modified = false;
		Iterator<UpdateProvider> iter = updateProviders.iterator();
		while (iter.hasNext()) {
			Updater updater = iter.next().updateRealm(realm);
			if (updater != null) {
				String webapp = webapp(webappOrigin, "").stringValue();
				modified |= updater.update(webapp, repository);
			}
		}
		if (modified) {
			logger.info("Created {}", realm);
		}
		return modified;
	}

	/**
	 * Installs the Callimachus webapp into the origin.
	 * 
	 * @param car
	 * @param webappOrigin origin to contain the Callimachus webapp
	 * @return if the RDF store was modified
	 * @throws OpenRDFException
	 * @throws InvocationTargetException 
	 * @throws NoSuchMethodException 
	 * @throws IOException 
	 */
	public boolean importCallimachusWebapp(URL car, String webappOrigin)
			throws OpenRDFException, IOException, NoSuchMethodException,
			InvocationTargetException {
		logger.info("Initializing {}", webappOrigin);
		String folder = repository.getCallimachusUrl(webappOrigin, "");
		if (folder == null)
			throw new IllegalArgumentException("Origin not setup: " + webappOrigin);
		return importCar(car, folder);
	}

	/**
	 * Installs the given CAR (or ZIP) into the given folder.
	 * 
	 * @param car
	 * @param folder absolute hierarchical URI with a path
	 * @param webappOrigin that contains a Callimachus webapp
	 * @return if the RDF store was modified
	 * @throws InvocationTargetException 
	 * @throws NoSuchMethodException 
	 * @throws OpenRDFException 
	 * @throws IOException 
	 */
	public boolean importCar(URL car, String folder)
			throws IOException, OpenRDFException, NoSuchMethodException,
			InvocationTargetException {
		if (car == null)
			throw new IllegalArgumentException("No CAR provided");
		String webappOrigin = createFolder(folder);
		URI[] schemaGraphs = importSchema(car, folder, webappOrigin);
		importArchive(schemaGraphs, car, folder, webappOrigin);
		return true;
	}

	/**
	 * Creates (or updates email) the given user.
	 * @param email
	 * @param username
	 * @param label
	 * @param comment
	 * @param webappOrigin that contains a Callimachus webapp
	 * 
	 * @return if the RDF store was modified
	 * @throws OpenRDFException
	 * @throws IOException
	 */
	public boolean createAdmin(String email, String username, String label,
			String comment, String webappOrigin) throws OpenRDFException, IOException {
		validateName(username);
		validateEmail(email);
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			boolean modified = false;
			for (String space : getDigestUserNamespaces(webappOrigin, con)) {
				URI subj = vf.createURI(space + username);
				modified |= changeAdminPassword(webappOrigin, vf.createURI(space),
						subj, email, username, label, comment, con);
			}
			con.setAutoCommit(true);
			return modified;
		} finally {
			con.close();
		}
	}

	public boolean changeUserPassword(String email, String username, char[] password, String origin) throws OpenRDFException, IOException {
		validateName(username);
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
				modified |= setPassword(subj, encoded, con);
			}
			con.setAutoCommit(true);
			if (modified) {
				logger.info("Changed password of {}", username);
			}
			return modified;
		} finally {
			con.close();
		}
	}

	/**
	 * Returns a set of registration links for the user with the given username
	 * on this webappOrigin. The empty set is returned if the user has already registered.
	 * 
	 * @param username
	 * @param email
	 * @param webappOrigin
	 *            that contains a Callimachus webapp
	 * @return Set of links (if any, but often just one)
	 * @throws OpenRDFException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 */
	public Set<String> getUserRegistrationLinks(String username, String email,
			String webappOrigin) throws OpenRDFException, IOException, NoSuchAlgorithmException {
		validateName(username);
		validateEmail(email);
		Set<String> list = new LinkedHashSet<String>();
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			for (String space : getDigestUserNamespaces(webappOrigin, con)) {
				URI subj = vf.createURI(space + username);
				if (!con.hasStatement(subj, RDF.TYPE, webapp(webappOrigin, USER_TYPE)))
					continue;
				if (con.hasStatement(subj, vf.createURI(CALLI_PASSWORD), null))
					continue;
		        Random random = java.security.SecureRandom.getInstance("SHA1PRNG");
		        String nonce = java.lang.Integer.toHexString(random.nextInt());
		        String hash = DigestUtils.md5Hex(subj.stringValue());
		        for (Statement st : con.getStatements((Resource) null, vf.createURI(CALLI_SECRET), null).asList()) {
		        	String realm = st.getSubject().stringValue();
					if (!space.startsWith(realm))
		        		continue;
		        	BlobObject secret = con.getBlobObject((URI) st.getObject());
			        Reader reader = secret.openReader(true);
			        try {
				        String text = new java.util.Scanner(reader).useDelimiter("\\A").next();
				        String token = DigestUtils.md5Hex(hash + ":" + nonce + ":" + text);
				        String queryString = "?register&token=" + token + "&nonce=" + nonce +
				            "&email=" + encode(email) + "&username=" + encode(username);
				        list.add(realm + queryString);
			        } finally {
			        	reader.close();
			        }
		        }
			}
			return list;
		} finally {
			con.close();
		}
	}

	public boolean finalizeWebappOrigin(String origin) throws IOException, OpenRDFException {
		validateOrigin(origin);
		boolean modified = false;
		Iterator<UpdateProvider> iter = updateProviders.iterator();
		while (iter.hasNext()) {
			Updater updater = iter.next().finalizeCallimachusWebapp(origin);
			if (updater != null) {
				String webapp = webapp(origin, "").stringValue();
				modified |= updater.update(webapp, repository);
			}
		}
		return modified;
	}

	private String encode(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
	}

	private Repository getRepository(LocalRepositoryManager manager, RepositoryConfig config)
			throws OpenRDFException, MalformedURLException, IOException {
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
		if (manager.getInitializedRepositoryIDs().contains(id)) {
			manager.getRepository(id).shutDown();
		}
		return manager.getRepository(id);
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

	private String upgradeStore(String origin) throws OpenRDFException, IOException {
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

	private boolean deleteComponents(String origin) {
		try {
			repository.setSchemaGraphType(webapp(origin, SCHEMA_GRAPH).stringValue());
			repository.setCompileRepository(true);
			ObjectConnection con = repository.getConnection();
			try {
				con.setAutoCommit(false);
				Object folder = con.getObject(webapp(origin, ""));
				Method DeleteComponents = findDeleteComponents(folder);
				try {
					logger.info("Removing {}", folder);
					int argc = DeleteComponents.getParameterTypes().length;
					DeleteComponents.invoke(folder, new Object[argc]);
					URI target = webapp(origin, "");
					con.remove(webapp(origin, "../"), null, target);
					con.remove(target, null, null);
					con.remove((Resource)null, null, null, target);
					con.setAutoCommit(true);
					return true;
				} catch (InvocationTargetException e) {
					try {
						throw e.getCause();
					} catch (Exception cause) {
						logger.warn(cause.toString());
					} catch (Error cause) {
						logger.warn(cause.toString());
					} catch (Throwable cause) {
						logger.warn(cause.toString());
					}
					con.rollback();
				}
			} catch (IllegalAccessException e) {
				logger.debug(e.toString());
			} catch (NoSuchMethodException e) {
				logger.debug(e.toString());
			} finally {
				con.rollback();
				repository.setCompileRepository(false);
				con.close();
			}
		} catch (RDFObjectException e) {
			logger.warn(e.toString());
		} catch (OpenRDFException e) {
			logger.warn(e.toString());
		}
		return false;
	}

	private Method findDeleteComponents(Object folder)
			throws NoSuchMethodException {
		for (Method method : folder.getClass().getMethods()) {
			if ("DeleteComponents".equals(method.getName()))
				return method;
		}
		throw new NoSuchMethodException("DeleteComponents in " + folder);
	}

	private boolean removeAllComponents(String origin) throws OpenRDFException {
		boolean modified = false;
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			String folder = webapp(origin, "").stringValue();
			TupleQueryResult results;
			results = con.prepareTupleQuery(QueryLanguage.SPARQL,
					GRAPH_STRSTARTS, folder).evaluate();
			try {
				while (results.hasNext()) {
					if (!modified) {
						modified = true;
						logger.info("Expunging {}", folder);
					}
					URI graph = (URI) results.next().getValue("graph");
					con.clear(graph);
				}
			} finally {
				results.close();
			}
			results = con.prepareTupleQuery(QueryLanguage.SPARQL,
					RESOURCE_STRSTARTS, folder).evaluate();
			try {
				while (results.hasNext()) {
					if (!modified) {
						modified = true;
						logger.info("Expunging {}", folder);
					}
					URI resource = (URI) results.next().getValue("resource");
					if (folder.equals(resource.stringValue())) {
						URI hasComponent = vf.createURI(CALLI_HASCOMPONENT);
						con.remove(resource, hasComponent, null);
					} else {
						con.remove(resource, null, null);
					}
				}
			} finally {
				results.close();
			}
			con.setAutoCommit(true);
			return modified;
		} finally {
			con.rollback();
			con.close();
		}
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

	private void importArchive(URI[] schemaGraphs, URL car, String folderUri,
			String origin) throws IOException, OpenRDFException,
			NoSuchMethodException, InvocationTargetException {
		HttpHost host = getAuthorityAddress(origin);
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		UnavailableHttpClient service = new UnavailableHttpClient();
		client.setProxy(host, service);
		for (URI schemaGraph : schemaGraphs) {
			repository.addSchemaGraph(schemaGraph.stringValue());
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
			} catch (IllegalAccessException e) {
				throw new AssertionError(e);
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

	private String createFolder(String folder) throws OpenRDFException {
		String origin = null;
		int idx = folder.lastIndexOf('/', folder.length() - 2);
		String parent = folder.substring(0, idx + 1);
		if (parent.endsWith("://")) {
			parent = null;
		} else {
			origin = createFolder(parent);
		}
		ValueFactory vf = repository.getValueFactory();
		ObjectConnection con = repository.getConnection();
		try {
			URI uri = vf.createURI(folder);
			if (origin == null || parent == null) {
				RepositoryResult<Statement> stmts = con.getStatements(uri,
						RDF.TYPE, null);
				try {
					while (stmts.hasNext()) {
						Statement st = stmts.next();
						String type = st.getObject().stringValue();
						if (type.endsWith(ORIGIN_TYPE)
								|| type.endsWith(REALM_TYPE)
								|| type.endsWith(FOLDER_TYPE)) {
							String root = TermFactory.newInstance(type)
									.resolve("/");
							return root.substring(0, root.length() - 1);
						}
					}
				} finally {
					stmts.close();
				}
				throw new IllegalStateException(
						"Can only import a CAR within a previously defined origin or realm");
			} else {
				if (con.hasStatement(uri, RDF.TYPE, webapp(origin, ORIGIN_TYPE)))
					return origin;
				if (con.hasStatement(uri, RDF.TYPE, webapp(origin, REALM_TYPE)))
					return origin;
				if (con.hasStatement(uri, RDF.TYPE, webapp(origin, FOLDER_TYPE)))
					return origin;
				if (con.hasStatement(vf.createURI(parent),
						vf.createURI(CALLI_HASCOMPONENT), uri))
					return origin;

				con.setAutoCommit(false);
				con.add(vf.createURI(parent), vf.createURI(CALLI_HASCOMPONENT),
						uri);
				String label = folder.substring(parent.length())
						.replace("/", "").replace('-', ' ');
				con.add(uri, RDF.TYPE, vf.createURI(CALLI_FOLDER));
				con.add(uri, RDF.TYPE, webapp(origin, FOLDER_TYPE));
				con.add(uri, RDFS.LABEL, vf.createLiteral(label));
				add(con, uri, CALLI_READER, origin + GROUP_PUBLIC);
				add(con, uri, CALLI_ADMINISTRATOR, origin + GROUP_ADMIN);
				con.setAutoCommit(true);
				return origin;
			}
		} finally {
			con.close();
		}
	}

	private HttpHost getAuthorityAddress(String origin) {
		return URIUtils.extractHost(java.net.URI.create(origin + "/"));
	}

	private void add(ObjectConnection con, URI subj, String pred,
			String resource) throws RepositoryException {
		ValueFactory vf = con.getValueFactory();
		con.add(subj, vf.createURI(pred), vf.createURI(resource));
	}

	private List<String> getDigestUserNamespaces(String origin,
			ObjectConnection con) throws RepositoryException {
		List<String> list = new ArrayList<String>();
		ValueFactory vf = con.getValueFactory();
		for (Statement st1 : con.getStatements(vf.createURI(origin + "/"),
				vf.createURI(CALLI_AUTHENTICATION), null).asList()) {
			Resource accounts = (Resource) st1.getObject();
			for (Statement st3 : con.getStatements(accounts,
					vf.createURI(CALLI_AUTHNAMESPACE), null).asList()) {
				list.add(st3.getObject().stringValue());
			}
		}
		return list;
	}

	private boolean changeAdminPassword(String origin, Resource folder,
			URI subj, String email, String username, String label,
			String comment, ObjectConnection con) throws OpenRDFException, IOException {
		ValueFactory vf = con.getValueFactory();
		URI calliEmail = vf.createURI(CALLI_EMAIL);
		if (con.hasStatement(subj, RDF.TYPE, vf.createURI(CALLI_USER))) {
			if (email == null || con.hasStatement(subj, calliEmail, vf.createLiteral(email)))
				return false;
			logger.info("Changing email of {}", username);
			con.remove(subj, calliEmail, null);
			con.add(subj, calliEmail, vf.createLiteral(email));
			return true;
		} else {
			logger.info("Creating user {}", username);
			URI staff = vf.createURI(origin + GROUP_STAFF);
			URI admin = vf.createURI(origin + GROUP_ADMIN);
			con.add(subj, RDF.TYPE, webapp(origin, USER_TYPE));
			con.add(subj, RDF.TYPE, vf.createURI(CALLI_PARTY));
			con.add(subj, RDF.TYPE, vf.createURI(CALLI_USER));
			con.add(subj, vf.createURI(CALLI_NAME), vf.createLiteral(username));
			if (label == null || label.length() == 0) {
				con.add(subj, RDFS.LABEL, vf.createLiteral(username));
			} else {
				con.add(subj, RDFS.LABEL, vf.createLiteral(label));
			}
			if (comment != null && comment.length() > 0) {
				con.add(subj, RDFS.COMMENT, vf.createLiteral(comment));
			}
			con.add(subj, vf.createURI(CALLI_SUBSCRIBER), staff);
			con.add(subj, vf.createURI(CALLI_ADMINISTRATOR), admin);
			con.add(folder, vf.createURI(CALLI_HASCOMPONENT), subj);
			con.add(admin, vf.createURI(CALLI_MEMBER), subj);
			if (email != null && email.length() > 2) {
				con.add(subj, calliEmail, vf.createLiteral(email));
			}
			return true;
		}
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

	private boolean setPassword(URI subj, String[] encoded, ObjectConnection con)
			throws RepositoryException, IOException {
		int i = 0;
		boolean modified = false;
		for (URI uuid : getPasswordURI(subj, encoded.length, con)) {
			modified |= storeTextBlob(uuid, encoded[i++], con);
		}
		return modified;
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

	private boolean storeTextBlob(URI uuid, String encoded, ObjectConnection con)
			throws RepositoryException, IOException {
		BlobObject blob = con.getBlobObject(uuid);
		if (encoded.equals(blob.getCharContent(false)))
			return false;
		Writer writer = blob.openWriter();
		try {
			writer.write(encoded);
			return true;
		} finally {
			writer.close();
		}
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

}
