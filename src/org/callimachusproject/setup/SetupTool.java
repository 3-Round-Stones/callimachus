package org.callimachusproject.setup;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.naming.NamingException;

import org.callimachusproject.Version;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.util.CallimachusConf;
import org.callimachusproject.util.Mailer;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetupTool {
	private static final String PREFIX = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";
	private static final String SELECT_ROOT = PREFIX
			+ "SELECT ?root\n"
			+ "(max(?resolvable) AS ?resolvable) (group_concat(?indexTarget) AS ?indexTarget)\n"
			+ "(group_concat(?layout) AS ?layout) (group_concat(?forbiddenPage) AS ?forbiddenPage)\n"
			+ "(group_concat(?unauthorizedPage) AS ?unauthorizedPage) (group_concat(?authentication) AS ?authentication)\n"
			+ "WHERE {\n"
			+ "{ ?root a </callimachus/1.0/types/Realm> FILTER NOT EXISTS { ?o calli:hasComponent ?root } BIND(false AS ?resolvable) }\n"
			+ "UNION { ?root a </callimachus/1.0/types/Origin> BIND(true AS ?resolvable) }\n"
			+ "OPTIONAL { ?root calli:describedby ?indexTarget }\n"
			+ "OPTIONAL { ?root calli:layout ?layout }\n"
			+ "OPTIONAL { ?root calli:forbidden ?forbiddenPage }\n"
			+ "OPTIONAL { ?root calli:unauthorized ?unauthorizedPage }\n"
			+ "OPTIONAL { ?root calli:authentication ?authentication }\n"
			+ "} GROUP BY ?root";
	private static final String SELECT_EMAIL = PREFIX + "SELECT ?label ?email { </> calli:authentication [calli:authNamespace [calli:hasComponent [rdfs:label ?label; calli:email ?email]]] }";
	private static final String SELECT_USERNAME = PREFIX + "SELECT ?username { </> calli:authentication [calli:authNamespace [calli:hasComponent [calli:name ?username; calli:email $email]]] }";

	private final Logger logger = LoggerFactory.getLogger(SetupTool.class);
	private final String repositoryID;
	private final CalliRepository repository;
	private final CallimachusConf conf;

	public SetupTool(String repositoryID, CalliRepository repository,
			CallimachusConf conf) throws OpenRDFException {
		this.repositoryID = repositoryID;
		this.repository = repository;
		this.conf = conf;
	}

	public String toString() {
		return repository.toString();
	}

	public CalliRepository getRepository() {
		return repository;
	}

	public String[] getWebappOrigins() throws IOException {
		return conf.getWebappOrigins();
	}

	public SetupOrigin[] getOrigins() throws IOException, OpenRDFException {
		List<SetupOrigin> list = new ArrayList<SetupOrigin>();
		CallimachusSetup setup = new CallimachusSetup(repository);
		RepositoryConnection con = setup.openConnection();
		try {
			Map<String, String> idsByOrigin = conf.getOriginRepositoryIDs();
			for (Map.Entry<String, String> e : idsByOrigin.entrySet()) {
				if (!repositoryID.equals(e.getValue()))
					continue;
				String root = e.getKey() + "/";
				TupleQueryResult results = con.prepareTupleQuery(
						QueryLanguage.SPARQL, SELECT_ROOT, root).evaluate();
				try {
					while (results.hasNext()) {
						BindingSet result = results.next();
						SetupOrigin o = createSetupOrigin(e.getKey(), result);
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
		return list.toArray(new SetupOrigin[list.size()]);
	}

	public synchronized void setupWebappOrigin(String origin)
			throws IOException, OpenRDFException, NoSuchMethodException,
			InvocationTargetException {
		List<String> previous = Arrays.asList(conf.getWebappOrigins());
		Collection<String> now = new LinkedHashSet<String>(previous);
		now.add(origin);
		// validate state
		if (now.size() > 1) {
			for (SetupOrigin o : getOrigins()) {
				if (!o.isResolvable() && o.getRepositoryID().equals(repositoryID))
					throw new IllegalStateException("Multiple resolvable origins cannot be used if unresolvable realms exist");
			}
		}
		Map<String, String> map = conf.getOriginRepositoryIDs();
		map = new LinkedHashMap<String, String>(map);
		map.put(origin, repositoryID);
		conf.setOriginRepositoryIDs(map);
		conf.setWebappOrigins(now.toArray(new String[now.size()]));
		for (SetupOrigin o : getOrigins()) {
			if (o.getWebappOrigin().equals(origin))
				return; // already exists in store
		}
		// if origin is undefined in RDF store, create it
		createWebappOrigin(origin);
	}

	public synchronized void setupResolvableOrigin(String origin, String webappOrigin)
			throws IOException, OpenRDFException {
		for (SetupOrigin o : getOrigins()) {
			if (!o.isResolvable())
				throw new IllegalStateException("Multiple resolvable origins cannot be used if unresolvable realms exist");
		}
		CallimachusSetup setup = new CallimachusSetup(repository);
		setup.prepareWebappOrigin(webappOrigin);
		setup.createOrigin(origin, webappOrigin);
		setup.finalizeWebappOrigin(webappOrigin);
	}

	public synchronized void setupRootRealm(String realm, String webappOrigin)
			throws OpenRDFException, IOException {
		String root = webappOrigin + "/";
		for (SetupOrigin origin : getOrigins()) {
			if (origin.isResolvable() && !origin.getRoot().equals(root))
				throw new IllegalStateException("Unresolvable realms can only be used with a single resolvable origin");
		}
		CallimachusSetup setup = new CallimachusSetup(repository);
		setup.prepareWebappOrigin(webappOrigin);
		setup.createRealm(realm, webappOrigin);
		setup.finalizeWebappOrigin(webappOrigin);
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

	public synchronized void inviteAdminUser(String email, String username,
			String label, String comment, String subject, String body,
			String webappOrigin) throws IOException, OpenRDFException,
			MessagingException, NamingException, GeneralSecurityException {
		CallimachusSetup setup = new CallimachusSetup(repository);
		setup.createAdmin(email, username, label, comment, webappOrigin);
		Set<String> links = setup.getUserRegistrationLinks(username, email,
				webappOrigin);
		for (String link : links) {
			String emailBody;
			Matcher m = Pattern.compile("[^<\"\\s]*\\?register").matcher(
					body);
			if (m.find()) {
				emailBody = body.substring(0, m.start()) + link
						+ body.substring(m.end(), body.length());
			} else {
				emailBody = body + "\n" + link;
			}
			new Mailer().sendMessage(subject + "\n" + emailBody,
					label + " <" + email + ">");
		}
	}

	public synchronized boolean changeDigestUserPassword(String email,
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
			return changeDigestUserPassword(email, password.toCharArray(), webappOrigin,
					setup, con);
		} finally {
			con.close();
		}
	}

	private SetupOrigin createSetupOrigin(String webappOrigin, BindingSet result) {
		String root = stringValue(result.getValue("root"));
		if (root == null)
			return null;
		boolean res = ((Literal) result.getValue("resolvable")).booleanValue();
		String indexTarget = stringValue(result.getValue("indexTarget"));
		String layout = stringValue(result.getValue("layout"));
		String forb = stringValue(result.getValue("forbiddenPage"));
		String unauth = stringValue(result.getValue("unauthorizedPage"));
		String auth = stringValue(result.getValue("authentication"));
		String[] split = auth == null ? new String[0] : auth.split("\\s+");
		return new SetupOrigin(root, res, webappOrigin, indexTarget, layout,
				forb, unauth, split, repositoryID);
	}

	private String stringValue(Value value) {
		if (value == null)
			return null;
		return value.stringValue();
	}

	private boolean changeDigestUserPassword(String email, char[] password,
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
				changed |= setup.changeUserPassword(email, username, password,
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
}
