package org.callimachusproject.setup;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;
import javax.naming.NamingException;

import org.callimachusproject.management.ConfigTemplate;
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
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockingSetupTool {
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
	private static final String REPOSITORY_TYPES = "META-INF/templates/repository-types.properties";

	private final Logger logger = LoggerFactory.getLogger(BlockingSetupTool.class);
	private final LocalRepositoryManager manager;
	private final File repositoryConfig;
	private final CallimachusConf conf;

	public BlockingSetupTool(File baseDir, File repositoryConfig,
			CallimachusConf conf) throws OpenRDFException {
		this.manager = RepositoryProvider.getRepositoryManager(baseDir);
		this.repositoryConfig = repositoryConfig;
		this.conf = conf;
	}

	public String toString() {
		return manager.getBaseDir().toString();
	}

	public LocalRepositoryManager getRepositoryManager() {
		return manager;
	}

	public File getRepositoryConfigFile() {
		return repositoryConfig;
	}

	public String getProperty(String key) throws IOException {
		return conf.getProperty(key);
	}

	public void setProperty(String key, String value) throws IOException {
		conf.setProperty(key, value);
	}

	public String[] getAvailableRepositoryTypes() throws IOException {
		List<String> list = new ArrayList<String>();
		ClassLoader cl = this.getClass().getClassLoader();
		Enumeration<URL> types = cl.getResources(REPOSITORY_TYPES);
		while (types.hasMoreElements()) {
			Properties properties = new Properties();
			InputStream in = types.nextElement().openStream();
			try {
				properties.load(in);
			} finally {
				in.close();
			}
			Enumeration<?> names = properties.propertyNames();
			while (names.hasMoreElements()) {
				list.add((String) names.nextElement());
			}
		}
		return list.toArray(new String[list.size()]);
	}

	public Map<String,String> getRepositoryProperties() throws IOException {
		if (!repositoryConfig.isFile())
			return Collections.emptyMap();
		ClassLoader cl = this.getClass().getClassLoader();
		Enumeration<URL> types = cl.getResources(REPOSITORY_TYPES);
		while (types.hasMoreElements()) {
			Properties properties = new Properties();
			InputStream in = types.nextElement().openStream();
			try {
				properties.load(in);
			} finally {
				in.close();
			}
			Enumeration<?> names = properties.propertyNames();
			while (names.hasMoreElements()) {
				String type = (String) names.nextElement();
				String path = properties.getProperty(type);
				Enumeration<URL> configs = cl.getResources(path);
				while (configs.hasMoreElements()) {
					URL url = configs.nextElement();
					ConfigTemplate temp = new ConfigTemplate(url);
					Map<String, String> parameters = temp.getParameters(repositoryConfig
							.toURI().toURL());
					if (parameters == null)
						continue;
					Map<String,String> result = new LinkedHashMap<String, String>();
					result.put("type", type);
					result.putAll(parameters);
					return result;
				}
			}
		}
		return Collections.emptyMap();
	}

	public synchronized void setRepositoryProperties(Map<String,String> parameters)
			throws IOException, OpenRDFException {
		Map<String, String> map = getRepositoryProperties();
		map = new LinkedHashMap<String, String>(map);
		map.putAll(parameters);
		String type = map.get("type");
		boolean modified = false;
		ClassLoader cl = this.getClass().getClassLoader();
		Enumeration<URL> types = cl.getResources(REPOSITORY_TYPES);
		while (types.hasMoreElements()) {
			Properties properties = new Properties();
			InputStream in = types.nextElement().openStream();
			try {
				properties.load(in);
			} finally {
				in.close();
			}
			if (properties.containsKey(type)) {
				String path = properties.getProperty(type);
				Enumeration<URL> configs = cl.getResources(path);
				while (configs.hasMoreElements()) {
					URL url = configs.nextElement();
					ConfigTemplate temp = new ConfigTemplate(url);
					String config = temp.render(map);
					if (config != null) {
						modified = true;
						logger.info("Replacing {}", repositoryConfig);
						repositoryConfig.getParentFile().mkdirs();
						FileWriter writer = new FileWriter(repositoryConfig);
						try {
							writer.write(config);
						} finally {
							writer.close();
						}
					}
				}
			}
		}
		if (!modified)
			throw new IllegalArgumentException("Unknown repository type: " + type);
		RepositoryConnection conn = new CallimachusSetup(manager, getRepositoryConfig()).openConnection();
		try {
			// just check that we can access the repository
			conn.hasStatement(null, null, null, false);
		} finally {
			conn.close();
		}
	}

	public String getWebappOrigins() throws IOException {
		return join(Arrays.asList(conf.getCallimachusWebappOrigins()));
	}

	public synchronized void setWebappOrigins(String origins)
			throws IOException, OpenRDFException, NoSuchMethodException,
			InvocationTargetException {
		List<String> previous = Arrays.asList(getWebappOrigins().split("\\s+"));
		Collection<String> now = new LinkedHashSet<String>(Arrays.asList(origins.split("\\s+")));
		List<String> removing = new ArrayList<String>(previous);
		removing.removeAll(now);
		List<String> adding = new ArrayList<String>(now);
		adding.removeAll(previous);
		// validate state
		if (now.size() > 1) {
			for (SetupOrigin o : getOrigins()) {
				if (!o.isResolvable() && !removing.contains(o.getWebappOrigin()))
					throw new IllegalStateException("Multiple resolvable origins cannot be used if unresolvable realms exist");
			}
		}
		synchronized (conf) {
			conf.setProperty("PRIMARY_ORIGIN", join(now));
			String val = conf.getProperty("ORIGIN");
			if (val != null) {
				List<String> list = Arrays.asList(val.split("\\s+"));
				List<String> allOrigins = new ArrayList<String>(list);
				allOrigins.removeAll(removing);
				allOrigins.addAll(adding);
				allOrigins.remove("");
				conf.setProperty("ORIGIN", join(allOrigins));
			} else {
				conf.setProperty("ORIGIN", join(now));
			}
		}
		SetupOrigin[] allOrigins = getOrigins();
		loop: for (String origin : adding) {
			for (SetupOrigin o : allOrigins) {
				if (o.getWebappOrigin().equals(origin) && !o.isPlaceHolder())
					continue loop; // already exists
			}
			// if origin is undefined in RDF store, create it
			createWebappOrigin(origin);
		}
	}

	public SetupOrigin[] getOrigins() throws IOException, OpenRDFException {
		List<SetupOrigin> list = new ArrayList<SetupOrigin>();
		CallimachusSetup setup = new CallimachusSetup(manager, getRepositoryConfig());
		RepositoryConnection con = setup.openConnection();
		try {
			for (String webappOrigin : getWebappOrigins().split("\\s+")) {
				if (webappOrigin.length() > 0) {
					String root = webappOrigin + "/";
					TupleQueryResult results = con.prepareTupleQuery(
							QueryLanguage.SPARQL, SELECT_ROOT,
							root).evaluate();
					try {
						if (!results.hasNext()) {
							list.add(new SetupOrigin(root, true, webappOrigin));
						}
						while (results.hasNext()) {
							SetupOrigin o = createSetupOrigin(webappOrigin,
									results.next());
							if (o != null) {
								list.add(o);
							}
						}
					} finally {
						results.close();
					}
				}
			}
		} finally {
			con.close();
		}
		return list.toArray(new SetupOrigin[list.size()]);
	}

	public synchronized void addResolvableOrigin(String origin, String webappOrigin)
			throws IOException, OpenRDFException {
		for (SetupOrigin o : getOrigins()) {
			if (!o.isResolvable())
				throw new IllegalStateException("Multiple resolvable origins cannot be used if unresolvable realms exist");
		}
		CallimachusSetup setup = new CallimachusSetup(manager, getRepositoryConfig());
		setup.createOrigin(origin, webappOrigin);
		String val = conf.getProperty("ORIGIN");
		if (val == null) {
			val = "";
		}
		List<String> list = Arrays.asList(val.trim().split("\\s+"));
		List<String> allOrigins = new ArrayList<String>(list);
		allOrigins.add(origin);
		conf.setProperty("ORIGIN", join(allOrigins));
	}

	public synchronized void addRootRealm(String realm, String webappOrigin)
			throws OpenRDFException, IOException {
		String root = webappOrigin + "/";
		if (!getWebappOrigins().equals(webappOrigin))
			throw new IllegalStateException("Unresolvable realms can only be used with a single primary origin");
		for (SetupOrigin origin : getOrigins()) {
			if (origin.isResolvable() && !origin.getRoot().equals(root))
				throw new IllegalStateException("Unresolvable realms can only be used with a single resolvable origin");
		}
		CallimachusSetup setup = new CallimachusSetup(manager, getRepositoryConfig());
		setup.createRealm(realm, webappOrigin);
	}

	public synchronized String[] getDigestEmailAddresses(String webappOrigin)
			throws OpenRDFException, IOException {
		CallimachusSetup setup = new CallimachusSetup(manager, getRepositoryConfig());
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
		CallimachusSetup setup = new CallimachusSetup(manager, getRepositoryConfig());
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
		CallimachusSetup setup = new CallimachusSetup(manager, getRepositoryConfig());
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
				forb, unauth, split);
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
		CallimachusSetup setup = new CallimachusSetup(manager, getRepositoryConfig());
		setup.prepareWebappOrigin(origin);
		boolean created = setup.createWebappOrigin(origin);
		setup.finalizeWebappOrigin(origin);
		if (created) {
			logger.info("Callimachus installed at {}", origin);
		} else {
			logger.info("Callimachus already appears to be installed at {}", origin);
		}
	}

	private String getRepositoryConfig() throws IOException {
		if (!repositoryConfig.isFile())
			return null;
		Scanner file = new Scanner(repositoryConfig).useDelimiter("\\A");
		try {
			return file.next();
		} finally {
			file.close();
		}
	}

	private String join(Collection<String> strings) {
		if (strings.isEmpty())
			return null;
		StringBuilder sb = new StringBuilder();
		for (String string : strings) {
			sb.append(string).append(' ');
		}
		return sb.toString().trim();
	}
}
