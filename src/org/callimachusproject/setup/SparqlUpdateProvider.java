package org.callimachusproject.setup;

import static org.openrdf.query.QueryLanguage.SPARQL;
import info.aduna.io.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.server.CallimachusRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparqlUpdateProvider implements UpdateProvider {
	private static final Pattern DEFAULT_WEBAPP = Pattern.compile("(?:^|\n)\\s*#\\s*@webapp\\s*<([^>]*)>\\s*(?:\n|$)");
	private static final String INITIAL_RU = "META-INF/upgrade/callimachus-initial.ru";
	private final Logger logger = LoggerFactory
			.getLogger(SparqlUpdateProvider.class);

	public String getDefaultCallimachusWebappLocation(String origin) throws IOException {
		Enumeration<URL> resources = getClass().getClassLoader().getResources(INITIAL_RU);
		if (!resources.hasMoreElements())
			logger.warn("Missing {}", INITIAL_RU);
		String root = origin + "/";
		TermFactory tf = TermFactory.newInstance(root);
		while (resources.hasMoreElements()) {
			InputStream in = resources.nextElement().openStream();
			Reader reader = new InputStreamReader(in, "UTF-8");
			String ru = IOUtil.readString(reader);
			Matcher m = DEFAULT_WEBAPP.matcher(ru);
			while (m.find()) {
				try {
					String resolved = tf.resolve(m.group(1));
					if (resolved.startsWith(root))
						return resolved;
				} catch (IllegalArgumentException e) {
					logger.warn(e.toString(), e);
				}
			}
		}
		return null;
	}

	public Updater initialize(final String origin) throws IOException {
		final ClassLoader cl = getClass().getClassLoader();
		Enumeration<URL> resources = cl.getResources(INITIAL_RU);
		if (!resources.hasMoreElements())
			return null;
		return new Updater() {
			public boolean update(String webapp, CallimachusRepository repository)
					throws IOException, OpenRDFException {
				Enumeration<URL> resources = cl.getResources(INITIAL_RU);
				while (resources.hasMoreElements()) {
					InputStream in = resources.nextElement().openStream();
					logger.info("Initializing {} Store", origin);
					Reader reader = new InputStreamReader(in, "UTF-8");
					String ru = IOUtil.readString(reader);
					ObjectConnection con = repository.getConnection();
					try {
						con.setAutoCommit(false);
						con.prepareUpdate(SPARQL, ru, webapp).execute();
						con.setAutoCommit(true);
					} finally {
						con.close();
					}
				}
				return true;
			}
		};
	}

	public Updater updateFrom(final String origin, final String version) throws IOException {
		final ClassLoader cl = getClass().getClassLoader();
		final String name = "META-INF/upgrade/callimachus-" + version + ".ru";
		Enumeration<URL> resources = cl.getResources(name);
		if (!resources.hasMoreElements())
			return null;
		return new Updater() {
			public boolean update(String webapp, CallimachusRepository repository)
					throws IOException, OpenRDFException {
				Enumeration<URL> resources = cl.getResources(name);
				while (resources.hasMoreElements()) {
					InputStream in = resources.nextElement().openStream();
					logger.info("Upgrading store from {}", version);
					Reader reader = new InputStreamReader(in, "UTF-8");
					String ru = IOUtil.readString(reader);
					ObjectConnection con = repository.getConnection();
					try {
						con.setAutoCommit(false);
						con.prepareUpdate(SPARQL, ru, webapp).execute();
						con.setAutoCommit(true);
					} finally {
						con.close();
					}
				}
				return true;
			}
		};
	}

	public Updater update(String origin) throws IOException {
		return null;
	}

}
