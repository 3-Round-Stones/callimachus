package org.callimachusproject.setup;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.MessagingException;
import javax.naming.NamingException;

import org.callimachusproject.Version;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.util.CallimachusConf;
import org.callimachusproject.util.Mailer;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.Update;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.rio.RDFFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupTool {
	private static final String ADMIN_GROUP = "/auth/groups/admin";
	private static final String REALM_TYPE = "types/Realm";
	private static final String FOLDER_TYPE = "types/Folder";
	private static final String CALLI = "http://callimachusproject.org/rdf/2009/framework#";
	private static final String CALLI_FOLDER = CALLI + "Folder";
	private static final String CALLI_REALM = CALLI + "Realm";
	private static final String CALLI_HASCOMPONENT = CALLI + "hasComponent";
	private static final String CALLI_AUTHENTICATION = CALLI + "authentication";
	private static final String PREFIX = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";
	private static final String COPY_FILE_PERM = PREFIX
			+ "INSERT { $file\n"
			+ "calli:reader ?reader; calli:subscriber ?subscriber; calli:contributor ?contributor; calli:editor ?editor; calli:administrator ?administrator\n"
			+ "} WHERE { ?parent calli:hasComponent $file\n"
			+ "OPTIONAL { ?parent calli:reader ?reader }\n"
			+ "OPTIONAL { ?parent calli:subscriber ?subscriber }\n"
			+ "OPTIONAL { ?parent calli:contributor ?contributor }\n"
			+ "OPTIONAL { ?parent calli:editor ?editor }\n"
			+ "OPTIONAL { ?parent calli:administrator ?administrator }\n" + "}";
	private static final String COPY_REALM_PROPS = PREFIX
			+ "INSERT { $realm\n"
			+ "calli:authentication ?auth; calli:unauthorized ?unauth; calli:forbidden ?forbid; calli:error ?error; calli:layout ?layout; calli:allowOrigin ?allowed\n"
			+ "} WHERE { {SELECT ?origin { ?origin a calli:Realm; calli:hasComponent+ $realm} ORDER BY desc(?origin) LIMIT 1}\n"
			+ "{ ?origin calli:authentication ?auth }\n"
			+ "UNION { ?origin calli:unauthorized ?unauth }\n"
			+ "UNION { ?origin calli:forbidden ?forbid }\n"
			+ "UNION { ?origin calli:error ?error }\n"
			+ "UNION { ?origin calli:layout ?layout }\n"
			+ "UNION { ?origin calli:allowOrigin ?allowed }\n"
			+ "}";
	private static final String SELECT_REALM = PREFIX
			+ "SELECT ?realm\n"
			+ "(group_concat(if(bound(?layout),str(?layout),\"\")) AS ?layout)\n"
			+ "(group_concat(if(bound(?errorPipe),str(?errorPipe),\"\")) AS ?errorPipe)\n"
			+ "(group_concat(if(bound(?forbiddenPage),str(?forbiddenPage),\"\")) AS ?forbiddenPage)\n"
			+ "(group_concat(if(bound(?unauthorizedPage),str(?unauthorizedPage),\"\")) AS ?unauthorizedPage)\n"
			+ "(group_concat(if(bound(?authentication),str(?authentication),\"\")) AS ?authentication)\n"
			+ "WHERE {\n"
			+ "{ ?realm a </callimachus/1.0/types/Realm> }\n"
			+ "UNION { ?realm a </callimachus/1.0/types/Origin> }\n"
			+ "OPTIONAL { { ?realm calli:layout ?layout }\n"
			+ "UNION { ?realm calli:error ?errorPipe }\n"
			+ "UNION { ?realm calli:forbidden ?forbiddenPage }\n"
			+ "UNION { ?realm calli:unauthorized ?unauthorizedPage }\n"
			+ "UNION { ?realm calli:authentication ?authentication } }\n"
			+ "} GROUP BY ?realm ORDER BY ?realm";
	private static final String SELECT_EMAIL = PREFIX + "SELECT ?label ?email { </> calli:authentication [calli:authNamespace [calli:hasComponent [rdfs:label ?label; calli:email ?email]]] }";
	private static final String SELECT_USERNAME = PREFIX + "SELECT ?username { </> calli:authentication [calli:authNamespace [calli:hasComponent [calli:name ?username; calli:email $email]]] }";

	private final Logger logger = LoggerFactory.getLogger(SetupTool.class);
	private final String repositoryID;
	private final CalliRepository repository;
	private final CallimachusConf conf;

	public SetupTool(String repositoryID, CalliRepository repository,
			CallimachusConf conf) throws OpenRDFException {
		assert repository != null;
		assert repositoryID != null;
		assert conf != null;
		this.repositoryID = repositoryID;
		this.repository = repository;
		this.conf = conf;
	}

	public String toString() {
		return repository.toString();
	}

	public String getRepositoryID() {
		return repositoryID;
	}

	public CalliRepository getRepository() {
		return repository;
	}

	public SetupRealm[] getRealms() throws IOException, OpenRDFException {
		List<SetupRealm> list = new ArrayList<SetupRealm>();
		CallimachusSetup setup = new CallimachusSetup(repository);
		RepositoryConnection con = setup.openConnection();
		try {
			Map<String, String> idsByOrigin = conf.getOriginRepositoryIDs();
			for (Map.Entry<String, String> e : idsByOrigin.entrySet()) {
				if (!repositoryID.equals(e.getValue()))
					continue;
				String root = e.getKey() + "/";
				TupleQueryResult results = con.prepareTupleQuery(
						QueryLanguage.SPARQL, SELECT_REALM, root).evaluate();
				try {
					while (results.hasNext()) {
						BindingSet result = results.next();
						SetupRealm o = createSetupRealm(e.getKey(), result);
						if (o != null) {
							list.add(o);
						}
					}
				} finally {
					results.close();
				}
			}
		} finally {
			con.close();
		}
		return list.toArray(new SetupRealm[list.size()]);
	}

	public synchronized void setupWebappOrigin(String origin)
			throws IOException, OpenRDFException, NoSuchMethodException,
			InvocationTargetException {
		Map<String, String> map = conf.getOriginRepositoryIDs();
		if (map.containsKey(origin) && !repositoryID.equals(map.get(origin)))
			throw new IllegalArgumentException(
					"Origin already exists in repository: " + map.get(origin));
		map = new LinkedHashMap<String, String>(map);
		map.put(origin, repositoryID);
		conf.setOriginRepositoryIDs(map);
		for (SetupRealm o : getRealms()) {
			if (o.getWebappOrigin().equals(origin))
				return; // already exists in store
		}
		// if origin is undefined in RDF store, create it
		createWebappOrigin(origin);
	}

	public synchronized void setupRealm(String realm, String webappOrigin)
			throws IOException, OpenRDFException {
		java.net.URI uri = java.net.URI.create(realm);
		if (!realm.startsWith(uri.toASCIIString()))
			throw new IllegalArgumentException("Invalid realm: " + realm);
		if (!realm.endsWith("/"))
			throw new IllegalArgumentException("Realms must end with a slash: " + realm);
		String origin = uri.getScheme() + "://" + uri.getAuthority();
		CallimachusSetup setup = new CallimachusSetup(repository);
		setup.prepareWebappOrigin(webappOrigin);
		setup.createOrigin(origin, webappOrigin);
		setup.finalizeWebappOrigin(webappOrigin);
		createRealm(realm, webappOrigin);
	}

	public void addAuthentication(String realm, String manager) 
			throws OpenRDFException, IOException {
		ObjectConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			ValueFactory vf = con.getValueFactory();
			URI s = vf.createURI(realm);
			URI p = vf.createURI(CALLI_AUTHENTICATION);
			URI o = vf.createURI(manager);
			if (!con.hasStatement(s, p, o)) {
				con.add(s, p, o);
			}
			con.setAutoCommit(true);
		} finally {
			con.close();
		}
	}

	public void removeAuthentication(String realm, String manager) 
			throws OpenRDFException, IOException {
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			URI s = vf.createURI(realm);
			URI p = vf.createURI(CALLI_AUTHENTICATION);
			URI o = vf.createURI(manager);
			con.remove(s, p, o);
		} finally {
			con.close();
		}
	}

	public boolean createResource(String graph, String systemId, String type,
			String webappOrigin) throws OpenRDFException, IOException {
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			URI target = vf.createURI(systemId);
			URI folder = createFolder(getParentFolder(systemId), webappOrigin, con);
			URI hasComponent = vf.createURI(CALLI_HASCOMPONENT);
			con.setAutoCommit(false);
			if (con.hasStatement(folder, hasComponent, target))
				return false;
			con.add(new StringReader(graph), systemId, RDFFormat.forMIMEType(type));
			con.add(folder, hasComponent, target);
			Update perm = con.prepareUpdate(QueryLanguage.SPARQL, COPY_FILE_PERM);
			perm.setBinding("file", target);
			perm.execute();
			con.setAutoCommit(true);
			return true;
		} finally {
			con.close();
		}
	}

	public synchronized String[] getDigestEmailAddresses(String webappOrigin)
			throws OpenRDFException, IOException {
		CallimachusSetup setup = new CallimachusSetup(repository);
		RepositoryConnection con = setup.openConnection();
		try {
			List<String> list = new ArrayList<String>();
			TupleQueryResult results = con.prepareTupleQuery(
					QueryLanguage.SPARQL, SELECT_EMAIL, webappOrigin + "/")
					.evaluate();
			try {
				while (results.hasNext()) {
					BindingSet result = results.next();
					Value l = result.getValue("label");
					Value e = result.getValue("email");
					list.add(l.stringValue() + " <" + e.stringValue() + ">");
				}
			} finally {
				results.close();
			}
			return list.toArray(new String[list.size()]);
		} finally {
			con.close();
		}
	}

	public synchronized void inviteAdminUser(String email, String subject,
			String body, String webappOrigin) throws IOException,
			OpenRDFException, MessagingException, NamingException,
			GeneralSecurityException {
		CallimachusSetup setup = new CallimachusSetup(repository);
		setup.inviteUser(email, webappOrigin);
		setup.addInvitedUserToGroup(email, ADMIN_GROUP, webappOrigin);
		Set<String> links = setup.getUserRegistrationLinks(email, webappOrigin);
		for (String link : links) {
			String emailBody;
			if (body.contains("?register")) {
				emailBody = body.replaceAll("[^<\"\\s]*\\?register", link);
			} else {
				emailBody = body + "\n" + link;
			}
			new Mailer().sendMessage(subject + "\n" + emailBody, email);
		}
	}

	public synchronized boolean registerDigestUser(String email,
			String password, String webappOrigin) throws OpenRDFException,
			IOException {
		CallimachusSetup setup = new CallimachusSetup(repository);
		RepositoryConnection con = setup.openConnection();
		try {
			int b = email.indexOf('<');
			int e = email.indexOf('>');
			if (b >= 0 && e > 0) {
				email = email.substring(b + 1, e);
			}
			return registeDigestUser(email, password.toCharArray(), webappOrigin,
					setup, con);
		} finally {
			con.close();
		}
	}

	private SetupRealm createSetupRealm(String webappOrigin, BindingSet result) {
		String realm = stringValue(result.getValue("realm"));
		if (realm == null)
			return null;
		String layout = stringValue(result.getValue("layout"));
		String error = stringValue(result.getValue("errorPipe"));
		String forb = stringValue(result.getValue("forbiddenPage"));
		String unauth = stringValue(result.getValue("unauthorizedPage"));
		String auth = stringValue(result.getValue("authentication"));
		String[] split =  new String[0];
		if (auth != null && auth.length() > 0) {
			split = auth.trim().split("\\s+");
		}
		return new SetupRealm(realm, webappOrigin, layout, error, forb, unauth,
				split, repositoryID);
	}

	private String stringValue(Value value) {
		if (value == null || value.stringValue().length() == 0)
			return null;
		return value.stringValue();
	}

	private boolean registeDigestUser(String email, char[] password,
			String webappOrigin, CallimachusSetup setup,
			RepositoryConnection con) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException,
			OpenRDFException, IOException {
		boolean changed = false;
		TupleQuery qry = con.prepareTupleQuery(QueryLanguage.SPARQL,
				SELECT_USERNAME, webappOrigin + "/");
		qry.setBinding("email", con.getValueFactory().createLiteral(email));
		TupleQueryResult results = qry.evaluate();
		try {
			while (results.hasNext()) {
				String username = results.next().getValue("username")
						.stringValue();
				changed |= setup.registerDigestUser(email, username, password,
						webappOrigin);
			}
		} finally {
			results.close();
		}
		return changed;
	}

	private void createWebappOrigin(final String origin)
			throws OpenRDFException, IOException, NoSuchMethodException,
			InvocationTargetException {
		CallimachusSetup setup = new CallimachusSetup(repository);
		setup.prepareWebappOrigin(origin);
		boolean created = setup.createWebappOrigin(origin);
		setup.finalizeWebappOrigin(origin);
		if (conf.getAppVersion() == null) {
			conf.setAppVersion(Version.getInstance().getVersionCode());
		}
		if (created) {
			logger.info("Callimachus installed at {}", origin);
		} else {
			logger.info("Callimachus already appears to be installed at {}", origin);
		}
	}

	private boolean createRealm(String realm, String webappOrigin) throws OpenRDFException {
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			URI subj = vf.createURI(realm);
			if (con.hasStatement(subj, RDF.TYPE, vf.createURI(CALLI_REALM), true))
				return false;
			con.add(subj, RDF.TYPE, vf.createURI(CALLI_REALM));
			con.add(subj, RDF.TYPE, webapp(webappOrigin, REALM_TYPE));
			createFolder(realm, webappOrigin, con);
			Update props = con.prepareUpdate(QueryLanguage.SPARQL, COPY_REALM_PROPS);
			props.setBinding("realm", subj);
			props.execute();
			return true;
		} finally {
			con.close();
		}
	}

	private URI webapp(String webappOrigin, String path) throws OpenRDFException {
		return repository.getValueFactory().createURI(repository.getCallimachusUrl(webappOrigin, path));
	}

	private URI createFolder(String folder, String webappOrigin,
			ObjectConnection con) throws OpenRDFException {
		ValueFactory vf = con.getValueFactory();
		URI uri = vf.createURI(folder);
		if (con.hasStatement(uri, RDF.TYPE, vf.createURI(CALLI_FOLDER)))
			return uri;
		String parent = getParentFolder(folder);
		if (parent == null)
			throw new IllegalStateException(
					"Can only import a CAR within a previously defined origin or realm");
		URI parentUri = createFolder(parent, webappOrigin, con);
		con.add(parentUri, vf.createURI(CALLI_HASCOMPONENT), uri);
		String label = folder.substring(parent.length()).replace("/", "")
				.replace('-', ' ');
		con.add(uri, RDFS.LABEL, vf.createLiteral(label));
		con.add(uri, RDF.TYPE, vf.createURI(CALLI_FOLDER));
		if (!con.hasStatement(uri, RDF.TYPE, webapp(webappOrigin, REALM_TYPE))) {
			con.add(uri, RDF.TYPE, webapp(webappOrigin, FOLDER_TYPE));
		}
		Update perm = con.prepareUpdate(QueryLanguage.SPARQL, COPY_FILE_PERM);
		perm.setBinding("file", uri);
		perm.execute();
		return uri;
	}

	private String getParentFolder(String folder) {
		int idx = folder.lastIndexOf('/', folder.length() - 2);
		if (idx < 0)
			return null;
		String parent = folder.substring(0, idx + 1);
		if (parent.endsWith("://"))
			return null;
		return parent;
	}
}
