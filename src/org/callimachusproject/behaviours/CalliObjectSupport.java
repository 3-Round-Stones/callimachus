/*
 * Copyright (c) 2009, James Leigh All rights reserved.
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
package org.callimachusproject.behaviours;

import static java.lang.Integer.toHexString;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;

import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.concepts.Activity;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.repository.auditing.ActivityFactory;
import org.callimachusproject.repository.auditing.AuditingRepositoryConnection;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.client.HttpUriClient;
import org.openrdf.http.object.management.ObjectServerMBean;
import org.openrdf.model.Model;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LinkedHashModel;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.base.RepositoryConnectionWrapper;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.util.RDFLoader;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.ContextStatementCollector;
import org.slf4j.LoggerFactory;

/**
 * Provides access to CalliRepository and revision hash tag.
 */
public abstract class CalliObjectSupport implements CalliObject {
	private static final String CHANGES_PATH = "../changes/";
	private static final Pattern URL_PATTERN = Pattern
			.compile("https?://[a-zA-Z0-9\\-\\._~%!\\$\\&'\\(\\)\\*\\+,;=:/\\[\\]@]+/(?![a-zA-Z0-9\\-\\._~%!\\$\\&'\\(\\)\\*\\+,;=:/\\?\\#\\[\\]@])");
	private final static Map<ObjectRepository, WeakReference<CalliRepository>> repositories = new WeakHashMap<ObjectRepository, WeakReference<CalliRepository>>();
	private final static WeakHashMap<ObjectConnection, Model> schemas = new WeakHashMap<ObjectConnection, Model>();

	public static void associate(CalliRepository repository,
			ObjectRepository repo) throws MalformedURLException {
		synchronized (repositories) {
			Iterator<ObjectRepository> iter = repositories.keySet().iterator();
			while (iter.hasNext()) {
				ObjectRepository key = iter.next();
				if (!key.isInitialized()) {
					iter.remove();
				}
			}
			repositories.put(repo, new WeakReference<CalliRepository>(repository));
		}
	}

	public static CalliRepository getCalliRepositroyFor(
			ObjectRepository repository) throws OpenRDFException, IOException {
		synchronized (repositories) {
			WeakReference<CalliRepository> ref = repositories.get(repository);
			if (ref != null) {
				CalliRepository result = ref.get();
				if (result != null)
					return result;
			}
			File dataDir = repository.getDataDir();
			if (dataDir == null)
				throw new IllegalArgumentException("Not a local repsitory: " + repository);
			File dir = dataDir.getParentFile().getParentFile();
			LocalRepositoryManager manager = RepositoryProvider.getRepositoryManager(dir);
			String id = RepositoryProvider.getRepositoryIdOfRepository(dataDir.toURI().toASCIIString());
			CalliRepository result = new CalliRepository(id, repository, manager);
			String desc = manager.getRepositoryInfo(id).getDescription();
			if (desc != null) {
				Matcher m = URL_PATTERN.matcher(desc);
				if (m.find()) {
					result.setChangeFolder(result.getCallimachusUrl(m.group(), CHANGES_PATH));
				}
			}
			associate(result, repository);
			return result;
		}
	}

	public static Model getSchemaModelFor(ObjectConnection con) {
		synchronized (schemas) {
			return schemas.get(con);
		}
	}

	public void resetAllCache() {
		MBeanServer mbsc = ManagementFactory.getPlatformMBeanServer();
		QueryExp instanceOf = Query.isInstanceOf(Query.value(ObjectServerMBean.class.getName()));
		for (ObjectName name : mbsc.queryNames(ObjectName.WILDCARD, instanceOf)) {
			ObjectServerMBean server = JMX.newMXBeanProxy(mbsc, name, ObjectServerMBean.class);
			server.resetCache();
		}
	}

	public CalliRepository getCalliRepository() throws OpenRDFException, IOException {
		return getCalliRepositroyFor(getObjectConnection().getRepository());
	}

	public DetachedRealm getRealm() throws OpenRDFException, IOException {
		return getCalliRepository().getRealm(this.getResource().stringValue());
	}

	public HttpUriClient getHttpClient() throws OpenRDFException, IOException {
		return getCalliRepository().getHttpClient(this.getResource().stringValue());
	}

	public void touchRevision() throws RepositoryException {
		Resource resource = getResource();
		if (resource instanceof URI) {
			String self = resource.stringValue();
			ObjectConnection con = getObjectConnection();
			ObjectFactory of = con.getObjectFactory();
			if (self.contains("#")) {
				CalliObject parent = (CalliObject) of.createObject(self.substring(0, self.indexOf('#')));
				parent.touchRevision();
			} else {
				URI bundle = con.getVersionBundle();
				if (bundle == null) {
					setProvWasGeneratedBy(null);
				} else {
					AuditingRepositoryConnection audit = findAuditing(con);
					if (audit == null) {
						setProvWasGeneratedBy(null);
					} else {
						ValueFactory vf = con.getValueFactory();
						ActivityFactory delegate = audit.getActivityFactory();
						URI activity = delegate.createActivityURI(bundle, vf);
						setProvWasGeneratedBy(of.createObject(activity, Activity.class));
					}
				}
			}
		}
	}

	public String revision() {
		try {
			Activity activity = getProvWasGeneratedBy();
			if (activity == null)
				return null;
			String uri = ((RDFObject) activity).getResource().stringValue();
			return toHexString(uri.hashCode());
		} catch (ClassCastException e) {
			LoggerFactory.getLogger(CalliObjectSupport.class).warn(e.getMessage());
			return null;
		}
	}

	@Override
	public Model getSchemaModel() {
		ObjectConnection con = getObjectConnection();
		synchronized (schemas) {
			Model model = schemas.get(con);
			if (model == null) {
				schemas.put(con, model = new LinkedHashModel());
			}
			return model;
		}
	}

	@Override
	public void setSchemaGraph(URI graph, GraphQueryResult result)
			throws QueryEvaluationException {
		Model schema = getSchemaModel();
		synchronized (schema) {
			for (Map.Entry<String, String> e : result.getNamespaces().entrySet()) {
				schema.setNamespace(e.getKey(), e.getValue());
			}
			while (result.hasNext()) {
				Statement s = result.next();
				schema.add(s.getSubject(), s.getPredicate(), s.getObject(), graph);
			}
		}
	}

	@Override
	public void setSchemaGraph(URI g, Reader reader, RDFFormat format)
			throws OpenRDFException, IOException {
		ObjectConnection con = getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		RDFLoader loader = new RDFLoader(con.getParserConfig(), vf);
		final Model schema = getSchemaModel();
		synchronized (schema) {
			RDFHandler handler = new ContextStatementCollector(schema, vf, g) {
				public void handleNamespace(String prefix, String uri)
						throws RDFHandlerException {
					schema.setNamespace(prefix, uri);
				}
			};
			loader.load(reader, g.stringValue(), format, handler);
		}
	}

	@Override
	public void setSchemaGraph(URI g, InputStream stream, RDFFormat format)
			throws OpenRDFException, IOException {
		ObjectConnection con = getObjectConnection();
		ValueFactory vf = con.getValueFactory();
		RDFLoader loader = new RDFLoader(con.getParserConfig(), vf);
		final Model schema = getSchemaModel();
		synchronized (schema) {
			RDFHandler handler = new ContextStatementCollector(schema, vf, g) {
				public void handleNamespace(String prefix, String uri)
						throws RDFHandlerException {
					schema.setNamespace(prefix, uri);
				}
			};
			loader.load(stream, g.stringValue(), format, handler);
		}
	}

	private AuditingRepositoryConnection findAuditing(
			RepositoryConnection con) throws RepositoryException {
		if (con instanceof AuditingRepositoryConnection)
			return (AuditingRepositoryConnection) con;
		if (con instanceof RepositoryConnectionWrapper)
			return findAuditing(((RepositoryConnectionWrapper) con).getDelegate());
		return null;
	}

}
