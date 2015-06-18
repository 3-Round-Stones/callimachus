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

import static org.callimachusproject.util.PercentCodec.encode;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.codec.digest.DigestUtils;
import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.behaviours.DigestManagerSupport;
import org.callimachusproject.concepts.Activity;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.client.HttpUriClient;
import org.openrdf.model.Literal;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.algebra.evaluation.util.ValueComparator;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFFormat;
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

	private static final String DIGEST_USER_TYPE = "types/DigestUser";
	private static final String INVITED_USERS = "/auth/invited-users/";

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

	private static final String GROUP_ADMIN = "/auth/groups/admin";
	private static final String GROUP_POWER = "/auth/groups/power";
	private static final String CHANGES_PATH = "../changes/";
	private static final String INVITED_USER_TYPE = "types/InvitedUser";

	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";
	private static final String CALLI_AUTHENTICATION = CALLI + "authentication";
	private static final String CALLI_ADMINISTRATOR = CALLI + "administrator";
	private static final String CALLI_EDITOR = CALLI + "editor";
	private static final String CALLI_SUBSCRIBER = CALLI + "subscriber";
	private static final String CALLI_USER = CALLI + "User";
	private static final String CALLI_PARTY = CALLI + "Party";
	private static final String CALLI_NAME = CALLI + "name";
	private static final String CALLI_EMAIL = CALLI + "email";
	private static final String CALLI_MEMBER = CALLI + "member";
	private static final String CALLI_PASSWORD = CALLI + "passwordDigest";
	private static final String CALLI_SECRET = CALLI + "secret";
	private static final String PREFIX = "PREFIX calli:<" + CALLI + ">\n";
	private static final String ASK_ADMIN_EMAIL = PREFIX + "ASK {\n" +
			"</auth/groups/admin> calli:member ?user .\n" +
			"?user calli:email ?email\n" +
			"FILTER NOT EXISTS { ?user a <types/InvitedUser> } }";
	private static final String SELECT_DIGEST = PREFIX
			+ "SELECT REDUCED ?digest ?authName ?authNamespace\n"
			+ "WHERE { </> calli:authentication ?digest .\n" +
			"?digest a calli:DigestManager; calli:authName ?authName; calli:authNamespace ?authNamespace }";
	private static final String ENABLE_DIGEST = PREFIX
			+ "SELECT ?realm ?digest { ?realm a calli:Realm . ?digest a <types/DigestManager>\n"
			+ "FILTER strstarts(str(?digest), replace(str(?realm), \"^(.*://[^/]*/).*\", \"$1\"))\n"
			+ "FILTER NOT EXISTS { ?realm calli:authentication ?digest } }";

	private final Logger logger = LoggerFactory.getLogger(CallimachusSetup.class);
	private final UpdateProvider updateProvider = new UpdateService(getClass().getClassLoader());
	private final Map<String,TermFactory> webapps = new HashMap<String, TermFactory>();
	private final CalliRepository repository;
	private final ValueFactory vf;

	public CallimachusSetup(CalliRepository repository)
			throws OpenRDFException, IOException {
		this.repository = repository;
		vf = repository.getValueFactory();
	}

	public String getWebappURL(String origin) throws OpenRDFException {
		return webapp(origin, "").stringValue();
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
	 * @throws IOException 
	 */
	public boolean prepareWebappOrigin(String origin) throws OpenRDFException, IOException {
		validateOrigin(origin);
		Updater updater = updateProvider.prepareCallimachusWebapp(origin);
		String webapp = webappIfPresent(origin);
		if (webapp == null)
			return false;
		return updater.update(webapp, repository);
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
		if (repository.getChangeFolder() == null) {
			String changes = webapp(origin, CHANGES_PATH).stringValue();
			repository.setChangeFolder(changes, webapp(origin, "").stringValue());
		}
		boolean modified = createOrigin(origin, origin);
		if (!barren) {
			String version = getStoreVersion(origin);
			String newVersion = upgradeStore(origin);
			modified |= !newVersion.equals(version);
		}
		return modified;
	}

	/**
	 * Upgrades the Callimachus webapp.
	 * 
	 * @param origin of a Callimachus webapp
	 * @return if the RDF store was modified
	 * @throws OpenRDFException 
	 * @throws IOException 
	 * @throws Exception
	 */
	public boolean updateWebapp(String origin)
			throws IOException, OpenRDFException {
		Updater updater = updateProvider.updateCallimachusWebapp(origin);
		boolean modified = updater.update(webapp(origin, "").stringValue(), repository);
		updateWebappContext(origin);
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
		Updater updater = updateProvider.updateOrigin(origin);
		String webapp = webapp(webappOrigin, "").stringValue();
		modified |= updater.update(webapp, repository);
		if (modified) {
			logger.info("Created {}", origin);
		}
		return modified;
	}

	public boolean isRegisteredAdmin(String origin) throws OpenRDFException,
			IOException {
		ObjectConnection con = repository.getConnection();
		try {
			String base = webapp(origin, "").stringValue();
			return con.prepareBooleanQuery(QueryLanguage.SPARQL,
					ASK_ADMIN_EMAIL, base).evaluate();
		} finally {
			con.close();
		}
	}

	/**
	 * Creates (or updates email) the given user.
	 * 
	 * @param email
	 * @param origin
	 *            that contains a Callimachus webapp
	 * @return if the RDF store was modified
	 * @throws OpenRDFException
	 * @throws IOException
	 */
	public boolean inviteUser(String email, String origin)
			throws OpenRDFException, IOException {
		validateEmail(email);
		String username = email.substring(0, email.indexOf('@'));
		ObjectConnection con = repository.getConnection();
		try {
			con.begin();
			ValueFactory vf = con.getValueFactory();
			URI folder = webapp(origin, INVITED_USERS);
			URI subj = vf.createURI(folder.stringValue(), slugify(email));
			URI calliEmail = vf.createURI(CALLI_EMAIL);
			if (con.hasStatement(subj, RDF.TYPE, vf.createURI(CALLI_USER))) {
				con.commit();
				return false;
			}
			logger.info("Inviting user {}", email);
			URI power = webapp(origin, GROUP_POWER);
			URI admin = webapp(origin, GROUP_ADMIN);
			con.add(subj, RDF.TYPE, webapp(origin, INVITED_USER_TYPE));
			con.add(subj, RDF.TYPE, vf.createURI(CALLI_PARTY));
			con.add(subj, RDF.TYPE, vf.createURI(CALLI_USER));
			con.add(subj, RDFS.LABEL, vf.createLiteral(username));
			con.add(subj, vf.createURI(CALLI_SUBSCRIBER), power);
			con.add(subj, vf.createURI(CALLI_ADMINISTRATOR), admin);
			con.add(folder, vf.createURI(CALLI_HASCOMPONENT), subj);
			con.add(subj, calliEmail, vf.createLiteral(email));
			con.commit();
			return true;
		} finally {
			con.close();
		}
	}

	public boolean addInvitedUserToGroup(String email, String groupPath,
			String webappOrigin) throws OpenRDFException, IOException {
		validateEmail(email);
		ObjectConnection con = repository.getConnection();
		try {
			con.begin();
			ValueFactory vf = con.getValueFactory();
			boolean modified = false;
			URI space = webapp(webappOrigin, INVITED_USERS);
			URI subj = vf.createURI(space + slugify(email));
			URI group = webapp(webappOrigin, groupPath);
			if (!con.hasStatement(group, vf.createURI(CALLI_MEMBER), subj)) {
				con.add(group, vf.createURI(CALLI_MEMBER), subj);
				modified = true;
			}
			con.commit();
			return modified;
		} finally {
			con.close();
		}
	}

	public boolean registerDigestUser(String email, String username,
			char[] password, String origin) throws OpenRDFException,
			IOException {
		validateName(username);
		ObjectConnection con = repository.getConnection();
		try {
			con.begin();
			ValueFactory vf = con.getValueFactory();
			boolean modified = enableDigestAuth(origin, con);
			if (modified) {
				con.commit();
				repository.resetCache();
				con.begin();
			}
			URI space = webapp(origin, INVITED_USERS);
			URI invitedUser = vf.createURI(space.stringValue(), slugify(email));
			for (DigestManagerSupport digest : getDigestManagers(origin, con, repository)) {
				String users = digest.getCalliAuthNamespace().getResource().stringValue();
				URI digestUser = vf.createURI(users, username);
				digest.registerUser(invitedUser, digestUser.stringValue(), email, null);
				URI DigestUser = webapp(origin, DIGEST_USER_TYPE);
				if (!con.hasStatement(digestUser, RDF.TYPE, DigestUser)) {
					con.add(digestUser, RDF.TYPE, DigestUser);
				}
				URI auth_users = vf.createURI(users);
				URI hasComponent = vf.createURI(CALLI_HASCOMPONENT);
				if (!con.hasStatement(auth_users, hasComponent, digestUser)) {
					con.add(auth_users, hasComponent, digestUser);
				}
				URI calliName = vf.createURI(CALLI_NAME);
				Literal lit = vf.createLiteral(username);
				if (!con.hasStatement(digestUser, calliName, lit)) {
					con.remove(digestUser, calliName, null);
					con.add(digestUser, calliName, lit);
				}
				URI calliEditor = vf.createURI(CALLI_EDITOR);
				if (!con.hasStatement(digestUser, calliEditor, digestUser)) {
					con.add(digestUser, calliEditor, digestUser);
				}
				String[] encoded = encodePassword(username, email,
						digest.getCalliAuthName(), password);
				modified |= setPassword(digestUser, encoded, origin, con);
			}
			con.commit();
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
	 * @param email
	 * @param webappOrigin
	 *            that contains a Callimachus webapp
	 * 
	 * @return Set of links (if any, but often just one)
	 * @throws OpenRDFException
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 */
	public Set<String> getUserRegistrationLinks(String email, String webappOrigin) throws OpenRDFException, IOException, NoSuchAlgorithmException {
		validateEmail(email);
		Set<String> list = new LinkedHashSet<String>();
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			URI space = webapp(webappOrigin, INVITED_USERS);
			URI subj = vf.createURI(space.stringValue(), slugify(email));
			if (!con.hasStatement(subj, RDF.TYPE, webapp(webappOrigin, INVITED_USER_TYPE)))
				return Collections.emptySet();
	        Random random = java.security.SecureRandom.getInstance("SHA1PRNG");
	        String nonce = java.lang.Integer.toHexString(random.nextInt());
	        String hash = DigestUtils.md5Hex(subj.stringValue());
	        URI origin = webapp(webappOrigin, "/");
			URI calliSecret = vf.createURI(CALLI_SECRET);
			for (Statement st : con.getStatements(origin, calliSecret, null).asList()) {
	        	String realm = st.getSubject().stringValue();
	        	BlobObject secret = con.getBlobObject((URI) st.getObject());
		        Reader reader = secret.openReader(true);
		        if (reader == null) {
		        	logger.error("{} is not correctly configured", origin);
		        	continue;
		        }
		        try {
			        String text = new java.util.Scanner(reader).useDelimiter("\\A").next();
			        String token = DigestUtils.md5Hex(hash + ":" + nonce + ":" + text);
			        String queryString = "?register&token=" + token + "&nonce=" + nonce +
			            "&email=" + encode(email);
			        list.add(realm + queryString);
		        } finally {
		        	reader.close();
		        }
	        }
			return list;
		} finally {
			con.close();
		}
	}

	public boolean finalizeWebappOrigin(String origin) throws IOException, OpenRDFException {
		validateOrigin(origin);
		Updater updater = updateProvider.finalizeCallimachusWebapp(origin);
		String webapp = webapp(origin, "").stringValue();
		return updater.update(webapp, repository);
	}

	private String slugify(String email) {
		return email.replaceAll("\\s+", "+").replaceAll("[^\\w\\+\\-\\_\\.\\!\\~\\*\\'\\(\\);\\,\\&\\=\\$\\[\\]]+","_");
	}

	private String createWebappUrl(String origin) throws IOException {
		String webapp = updateProvider.getDefaultWebappLocation(origin);
		if (webapp != null)
			return webapp;
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
		Updater updater = updateProvider.updateFrom(origin, version);
		String webapp = webapp(origin, "").stringValue();
		updater.update(webapp, repository);
		updateWebappContext(origin);
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
	}

	private List<DigestManagerSupport> getDigestManagers(String origin,
			final ObjectConnection con, final CalliRepository repository) throws OpenRDFException {
		List<DigestManagerSupport> list = new ArrayList<DigestManagerSupport>();
		TupleQueryResult results = con.prepareTupleQuery(QueryLanguage.SPARQL,
				SELECT_DIGEST, webapp(origin, "").stringValue()).evaluate();
		try {
			while (results.hasNext()) {
				BindingSet result = results.next();
				final Resource digest = (Resource) result.getValue("digest");
				final String authName = result.getValue("authName")
						.stringValue();
				final Resource authNamespace = (Resource) result
						.getValue("authNamespace");
				list.add(new DigestManagerSupport() {

					public String getCalliAuthName() {
						return authName;
					}

					public Resource getResource() {
						return digest;
					}

					public ObjectConnection getObjectConnection() {
						return con;
					}

					public CalliRepository getCalliRepository() {
						return repository;
					}

					public DetachedRealm getRealm() throws OpenRDFException, IOException {
						return repository.getRealm(digest.stringValue());
					}

					public HttpUriClient getHttpClient() throws OpenRDFException {
						return repository.getHttpClient(digest.stringValue());
					}

					public RDFObject getCalliAuthNamespace() {
						return con.getObjectFactory().createObject(
								authNamespace);
					}

					public void setCalliAuthNamespace(RDFObject authNamespace) {
						throw new UnsupportedOperationException();
					}

					public void setCalliAuthName(String authName) {
						throw new UnsupportedOperationException();
					}

					public Activity getProvWasGeneratedBy() {
						throw new UnsupportedOperationException();
					}

					public void setProvWasGeneratedBy(Activity activity) {
						throw new UnsupportedOperationException();
					}

					public void touchRevision() throws RepositoryException {
						throw new UnsupportedOperationException();
					}

					public String revision() {
						throw new UnsupportedOperationException();
					}

					public void resetAllCache() {
						// no-op
					}

					public Model getSchemaModel() {
						throw new UnsupportedOperationException();
					}

					@Override
					public void removeSchemaGraph(URI graph)
							throws OpenRDFException, IOException {
						throw new UnsupportedOperationException();
					}

					public void setSchemaGraph(URI graph,
							GraphQueryResult schema) throws OpenRDFException,
							IOException {
						throw new UnsupportedOperationException();
					}

					public void setSchemaGraph(URI graph, Reader reader,
							RDFFormat format) throws OpenRDFException,
							IOException {
						throw new UnsupportedOperationException();
					}

					public void setSchemaGraph(URI graph, InputStream stream,
							RDFFormat format) throws OpenRDFException,
							IOException {
						throw new UnsupportedOperationException();
					}
				});
			}
		} finally {
			results.close();
		}
		return list;
	}

	private boolean enableDigestAuth(String origin, ObjectConnection con)
			throws OpenRDFException {
		boolean modified = false;
		ValueFactory vf = con.getValueFactory();
		String base = webapp(origin, "").stringValue();
		TupleQueryResult results = con.prepareTupleQuery(QueryLanguage.SPARQL,
				ENABLE_DIGEST, base).evaluate();
		try {
			while (results.hasNext()) {
				BindingSet result = results.next();
				Resource realm = (Resource) result.getValue("realm");
				Value digest = result.getValue("digest");
				modified = true;
				logger.info("Enabling digest authentication in {}", realm);
				con.add(realm, vf.createURI(CALLI_AUTHENTICATION), digest);
			}
		} finally {
			results.close();
		}
		return modified;
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

	private boolean setPassword(URI subj, String[] encoded, String origin,
			ObjectConnection con) throws RepositoryException, IOException {
		int i = 0;
		boolean modified = false;
		for (URI uuid : getPasswordURI(subj, encoded.length, origin, con)) {
			modified |= storeTextBlob(uuid, encoded[i++], con);
		}
		return modified;
	}

	private Collection<URI> getPasswordURI(URI subj, int count, String origin,
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
				con.remove((URI) object, null, null);
				con.remove((URI) null, null, (URI) object);
			}
		}
		String webapp = CalliRepository.getCallimachusWebapp(origin, con);
		Set<URI> list = new TreeSet<URI>(new ValueComparator());
		for (int i = 0; i < count; i++) {
			URI uuid = SecretOriginProvider.createSecretFile(webapp, con);
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
			String webapp = repository.getCallimachusWebapp(origin);
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
		String webapp = repository.getCallimachusWebapp(origin);
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
