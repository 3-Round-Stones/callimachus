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

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.codec.digest.DigestUtils;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.algebra.evaluation.util.ValueComparator;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.object.ObjectConnection;
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

	private static final String GROUP_ADMIN = "/auth/groups/admin";
	private static final String GROUP_STAFF = "/auth/groups/staff";
	private static final String CHANGES_PATH = "../changes/";
	private static final String USER_TYPE = "types/User";

	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";
	private static final String CALLI_ADMINISTRATOR = CALLI + "administrator";
	private static final String CALLI_SUBSCRIBER = CALLI + "subscriber";
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
		String webapp = webapp(origin, "").stringValue();
		if (repository.getChangeFolder() == null) {
			String changes = webapp(origin, CHANGES_PATH).stringValue();
			repository.setChangeFolder(changes, webapp);
		}
		boolean modified = createOrigin(origin, origin);
		if (!barren) {
			String version = getStoreVersion(origin);
			String newVersion = upgradeStore(origin);
			modified |= !newVersion.equals(version);
		}
		Updater updater = updateProvider.updateCallimachusWebapp(origin);
		modified |= updater.update(webapp, repository);
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
				URI admin = webapp(webappOrigin, GROUP_ADMIN);
				if (!con.hasStatement(admin, vf.createURI(CALLI_MEMBER), subj)) {
					con.add(admin, vf.createURI(CALLI_MEMBER), subj);
					modified = true;
				}
			}
			con.setAutoCommit(true);
			return modified;
		} finally {
			con.close();
		}
	}

	public boolean addUserToGroup(String username, String groupPath,
			String webappOrigin) throws OpenRDFException, IOException {
		validateName(username);
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			boolean modified = false;
			for (String space : getDigestUserNamespaces(webappOrigin, con)) {
				URI subj = vf.createURI(space + username);
				URI group = webapp(webappOrigin, groupPath);
				if (!con.hasStatement(group, vf.createURI(CALLI_MEMBER), subj)) {
					con.add(group, vf.createURI(CALLI_MEMBER), subj);
					modified = true;
				}
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
				modified |= setPassword(subj, encoded, origin, con);
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
		Updater updater = updateProvider.finalizeCallimachusWebapp(origin);
		String webapp = webapp(origin, "").stringValue();
		return updater.update(webapp, repository);
	}

	private String encode(String string) {
		try {
			return URLEncoder.encode(string, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new AssertionError(e);
		}
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
			URI staff = webapp(origin, GROUP_STAFF);
			URI admin = webapp(origin, GROUP_ADMIN);
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
			}
			con.remove(st);
		}
		String webapp = CalliRepository.getCallimachusWebapp(origin, con);
		Set<URI> list = new TreeSet<URI>(new ValueComparator());
		for (int i = 0; i < count; i++) {
			URI uuid = SecretRealmProvider.createSecretFile(webapp, con);
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
