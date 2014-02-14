/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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
import org.callimachusproject.repository.CalliRepository;
import org.openrdf.OpenRDFException;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.Update;
import org.openrdf.repository.object.ObjectConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparqlUpdateProvider extends UpdateProvider {
	private static final Pattern DEFAULT_WEBAPP = Pattern.compile("(?:^|\n)\\s*#\\s*@webapp\\s*<([^>]*)>\\s*(?:\n|$)");
	private static final String WEBAPP_RU = "META-INF/upgrade/callimachus-webapp.ru";
	private static final String ORIGIN_RU = "META-INF/upgrade/callimachus-origin.ru";
	private static final String FINALIZE_RU = "META-INF/upgrade/callimachus-finalize.ru";
	private final Logger logger = LoggerFactory
			.getLogger(SparqlUpdateProvider.class);

	@Override
	public String getDefaultWebappLocation(String origin) throws IOException {
		Enumeration<URL> resources = getClass().getClassLoader().getResources(WEBAPP_RU);
		if (!resources.hasMoreElements())
			logger.warn("Missing {}", WEBAPP_RU);
		String root = origin.toLowerCase() + "/";
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

	@Override
	public Updater updateCallimachusWebapp(final String origin) throws IOException {
		final ClassLoader cl = getClass().getClassLoader();
		Enumeration<URL> resources = cl.getResources(WEBAPP_RU);
		if (!resources.hasMoreElements())
			return null;
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				Enumeration<URL> resources = cl.getResources(WEBAPP_RU);
				while (resources.hasMoreElements()) {
					URL url = resources.nextElement();
					InputStream in = url.openStream();
					logger.info("Updating {} Store", origin);
					Reader reader = new InputStreamReader(in, "UTF-8");
					String ru = IOUtil.readString(reader);
					ObjectConnection con = repository.getConnection();
					try {
						con.begin();
						con.prepareUpdate(SPARQL, ru, webapp).execute();
						con.commit();
					} catch (MalformedQueryException e) {
						throw new MalformedQueryException(e.getMessage()
								.replaceAll("\n.*", "") + " in " + url, e);
					} finally {
						con.close();
					}
				}
				return true;
			}
		};
	}

	@Override
	public Updater updateOrigin(final String virtual)
			throws IOException {
		final ClassLoader cl = getClass().getClassLoader();
		Enumeration<URL> resources = cl.getResources(ORIGIN_RU);
		if (!resources.hasMoreElements())
			return null;
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				Enumeration<URL> resources = cl.getResources(ORIGIN_RU);
				while (resources.hasMoreElements()) {
					URL url = resources.nextElement();
					InputStream in = url.openStream();
					Reader reader = new InputStreamReader(in, "UTF-8");
					String ru = IOUtil.readString(reader);
					ObjectConnection con = repository.getConnection();
					try {
						con.begin();
						ValueFactory vf = con.getValueFactory();
						Update update = con.prepareUpdate(SPARQL, ru, webapp);
						update.setBinding("origin", vf.createURI(virtual + "/"));
						update.execute();
						con.commit();
					} catch (MalformedQueryException e) {
						throw new MalformedQueryException(e.getMessage()
								.replaceAll("\n.*", "") + " in " + url, e);
					} finally {
						con.close();
					}
				}
				return true;
			}
		};
	}

	@Override
	public Updater updateFrom(final String origin, final String version) throws IOException {
		final ClassLoader cl = getClass().getClassLoader();
		final String name = "META-INF/upgrade/callimachus-" + version + ".ru";
		Enumeration<URL> resources = cl.getResources(name);
		if (!resources.hasMoreElements())
			return null;
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				Enumeration<URL> resources = cl.getResources(name);
				while (resources.hasMoreElements()) {
					URL url = resources.nextElement();
					logger.info("Upgrading store from {}", version);
					InputStream in = url.openStream();
					Reader reader = new InputStreamReader(in, "UTF-8");
					String ru = IOUtil.readString(reader);
					ObjectConnection con = repository.getConnection();
					try {
						con.begin();
						con.prepareUpdate(SPARQL, ru, webapp).execute();
						con.commit();
					} catch (MalformedQueryException e) {
						throw new MalformedQueryException(e.getMessage()
								.replaceAll("\n.*", "") + " in " + url, e);
					} finally {
						con.close();
					}
				}
				return true;
			}
		};
	}

	@Override
	public Updater finalizeCallimachusWebapp(String origin) throws IOException {
		final ClassLoader cl = getClass().getClassLoader();
		Enumeration<URL> resources = cl.getResources(FINALIZE_RU);
		if (!resources.hasMoreElements())
			return null;
		return new Updater() {
			public boolean update(String webapp, CalliRepository repository)
					throws IOException, OpenRDFException {
				Enumeration<URL> resources = cl.getResources(FINALIZE_RU);
				while (resources.hasMoreElements()) {
					URL url = resources.nextElement();
					InputStream in = url.openStream();
					Reader reader = new InputStreamReader(in, "UTF-8");
					String ru = IOUtil.readString(reader);
					ObjectConnection con = repository.getConnection();
					try {
						con.begin();
						con.prepareUpdate(SPARQL, ru, webapp).execute();
						con.commit();
					} catch (MalformedQueryException e) {
						throw new MalformedQueryException(e.getMessage()
								.replaceAll("\n.*", "") + " in " + url, e);
					} finally {
						con.close();
					}
				}
				return true;
			}
		};
	}

}
