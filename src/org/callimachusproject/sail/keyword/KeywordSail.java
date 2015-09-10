/*
 * Copyright (c) 2011, 3 Round Stones Inc. Some rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution. 
 * - Neither the name of the openrdf.org nor the names of its contributors may
 *   be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
package org.callimachusproject.sail.keyword;

import info.aduna.iteration.CloseableIteration;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.sail.Sail;
import org.openrdf.sail.SailConnection;
import org.openrdf.sail.SailException;
import org.openrdf.sail.helpers.SailWrapper;
import org.openrdf.sail.inferencer.InferencerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Add keyword:phone property of resource's label soundex. Label properties to
 * index are read from META-INF/org.callimachusproject.sail.keyword.property if the
 * keywordProperties is null or empty. The index property and graph are
 * configurable.
 * 
 * @author James Leigh
 * 
 */
public class KeywordSail extends SailWrapper {
	private static final String SETTING_PROPERTIES = "org.callimachusproject.sail.keyword.properties";
	private static final String PHONE_URI = "http://www.openrdf.org/rdf/2011/keyword#phone";
	private final Logger logger = LoggerFactory.getLogger(KeywordSail.class);
	private boolean enabled = true;
	private URI property;
	private URI graph = null;
	private Set<URI> labels;
	private final PhoneHelper helper = PhoneHelperFactory.newInstance()
			.createPhoneHelper();

	public KeywordSail() {
		super();
	}

	public KeywordSail(Sail baseSail) {
		super(baseSail);
	}

	public String toString() {
		return getBaseSail().toString();
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public URI getPhoneProperty() {
		if (property == null)
			return ValueFactoryImpl.getInstance().createURI(PHONE_URI);
		return property;
	}

	/**
	 * RDF predicate to index resources with.
	 */
	public void setPhoneProperty(URI property) {
		this.property = property;
	}

	public URI getPhoneGraph() {
		return graph;
	}

	/**
	 * Where to index soundex phone properties.
	 */
	public void setPhoneGraph(URI graph) {
		this.graph = graph;
	}

	/**
	 * Set of RDF properties to index for keywords.
	 * 
	 * @return Set of URI or null if default properties are used
	 */
	public Set<URI> getKeywordProperties() {
		return labels;
	}

	public void setKeywordProperties(Set<URI> set) {
		this.labels = set;
	}

	@Override
	public void initialize() throws SailException {
		super.initialize();
		ValueFactory vf = getValueFactory();
		if (property == null) {
			property = vf.createURI(PHONE_URI);
		} else {
			property = vf.createURI(property.stringValue());
		}
		if (graph != null) {
			graph = vf.createURI(graph.stringValue());
		}
		if (labels == null || labels.isEmpty()) {
			labels = readSet("META-INF/org.callimachusproject.sail.keyword.property");
		} else {
			Set<URI> set = new HashSet<URI>(labels.size());
			for (URI uri : labels) {
				set.add(vf.createURI(uri.stringValue()));
			}
			labels = set;
		}
		try {
			File dir = getDataDir();
			if (dir != null) {
				Properties properties = loadSettings(dir);
				if (!isSameSettings(properties)) {
					logger.info("Reindexing keywords in {}", this);
					clear(properties);
					if (enabled) {
						reindex();
					}
				}
				saveSettings(dir);
			}
		} catch (IOException e) {
			throw new SailException(e);
		}
	}

	@Override
	public SailConnection getConnection() throws SailException {
		if (enabled)
			return getKeywordConnection();
		return super.getConnection();
	}

	protected boolean isIndexedProperty(URI property) {
		return labels.contains(property);
	}

	private KeywordConnection getKeywordConnection() throws SailException {
		return new KeywordConnection(this, super.getConnection(), helper);
	}

	private Properties loadSettings(File dir) throws FileNotFoundException,
			IOException {
		Properties properties = new Properties();
		File file = new File(dir, SETTING_PROPERTIES);
		if (file.exists()) {
			FileInputStream in = new FileInputStream(file);
			try {
				properties.load(in);
			} finally {
				in.close();
			}
		}
		return properties;
	}

	private boolean isSameSettings(Properties properties) {
		if (!Integer.toHexString(helper.hashCode()).equals(
				properties.getProperty("phone")))
			return false;
		if (!Integer.toHexString(labels.hashCode()).equals(
				properties.getProperty("label")))
			return false;
		if (!property.stringValue().equals(properties.getProperty("property")))
			return false;
		if (enabled && "false".equals(properties.getProperty("enabled")))
			return false;
		if (graph == null)
			return properties.getProperty("graph") == null;
		return graph.stringValue().equals(properties.getProperty("graph"));
	}

	private void clear(Properties properties) throws SailException {
		ValueFactory vf = getValueFactory();
		String property = properties.getProperty("property");
		if (property != null) {
			URI pred = vf.createURI(property);
			String graph = properties.getProperty("graph");
			Resource context = graph == null ? null : vf.createURI(graph);
			SailConnection con = super.getConnection();
			try {
				con.begin();
				if (con instanceof InferencerConnection) {
					InferencerConnection icon = (InferencerConnection) con;
					icon.removeInferredStatement(null, pred, null, context);
				} else {
					con.removeStatements(null, pred, null, context);
				}
				con.commit();
			} finally {
				con.close();
			}
		}
	}

	private void reindex() throws SailException {
		KeywordConnection con = getKeywordConnection();
		try {
			con.begin();
			for (URI pred : labels) {
				CloseableIteration<? extends Statement, SailException> stmts;
				stmts = con.getStatements(null, pred, null, false);
				try {
					while (stmts.hasNext()) {
						Statement st = stmts.next();
						con.index(null, st.getSubject(), st.getObject());
					}
				} finally {
					stmts.close();
				}
			}
			con.commit();
		} finally {
			con.close();
		}
	}

	private void saveSettings(File dir) throws IOException {
		Properties properties = new Properties();
		String code = Integer.toHexString(helper.hashCode());
		properties.setProperty("phone", code);
		properties.setProperty("label", Integer.toHexString(labels.hashCode()));
		properties.setProperty("property", property.stringValue());
		properties.setProperty("enabled", String.valueOf(enabled));
		if (graph == null) {
			properties.remove("graph");
		} else {
			properties.setProperty("graph", graph.stringValue());
		}
		dir.mkdirs();
		File file = new File(dir, SETTING_PROPERTIES);
		FileOutputStream out = new FileOutputStream(file);
		try {
			properties.store(out, this.toString());
		} finally {
			out.close();
		}
	}

	private Set<URI> readSet(String name) {
		ValueFactory vf = getValueFactory();
		HashSet<URI> set = new HashSet<URI>();
		Enumeration<URL> resources = getResources(name);
		while (resources.hasMoreElements()) {
			try {
				InputStream in = resources.nextElement().openStream();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(in, "UTF-8"));
				try {
					String line;
					while ((line = reader.readLine()) != null) {
						if (line.length() > 0) {
							set.add(vf.createURI(line));
						}
					}
				} finally {
					reader.close();
				}
			} catch (IOException e) {
				logger.error(e.toString(), e);
			}
		}
		return set;
	}

	private Enumeration<URL> getResources(String name) {
		ClassLoader cl = KeywordSail.class.getClassLoader();
		try {
			return cl.getResources(name);
		} catch (IOException e) {
			logger.error(e.toString(), e);
			return new Vector<URL>().elements();
		}
	}

}
