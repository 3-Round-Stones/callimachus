/*
 * Copyright (c) 2014 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.repository;

import info.aduna.net.ParsedURI;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.auth.RealmManager;
import org.callimachusproject.behaviours.CalliObjectSupport;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.repository.auditing.ActivityFactory;
import org.callimachusproject.repository.auditing.AuditingRepository;
import org.callimachusproject.repository.trace.Trace;
import org.callimachusproject.repository.trace.TracerService;
import org.openrdf.OpenRDFException;
import org.openrdf.http.object.client.HttpUriClient;
import org.openrdf.http.object.management.ObjectRepositoryManager;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.base.RepositoryWrapper;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.manager.RepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.helpers.BasicParserSettings;
import org.openrdf.store.blob.BlobStore;

public class CalliRepository extends RepositoryWrapper {
	public class HttpRepositoryClient extends HttpUriClient {
		private final String source;

		HttpRepositoryClient(String source) {
			this.source = source;
		}

		public CredentialsProvider getCredentialsProvider()
				throws OpenRDFException, IOException {
			return getRealm(source).getCredentialsProvider();
		}

		protected HttpClient getDelegate() throws IOException {
			try {
				return getRealm(source).getHttpClient();
			} catch (OpenRDFException e) {
				throw new IOException(e);
			}
		}
	}

	private static final String SLASH_ORIGIN = "/types/Origin";
	private static final String CHANGE_TYPE = "types/Change";
	private static final String FOLDER_TYPE = "types/PathSegment";

	public static String getCallimachusWebapp(String url, RepositoryConnection con)
			throws RepositoryException {
		ParsedURI parsed = new ParsedURI(url + "/");
		String root = parsed.getScheme() + "://" + parsed.getAuthority() + "/";
		ValueFactory vf = con.getValueFactory();
		RepositoryResult<Statement> stmts;
		stmts = con
				.getStatements(vf.createURI(root), RDF.TYPE, null, false);
		try {
			while (stmts.hasNext()) {
				String type = stmts.next().getObject().stringValue();
				if (type.startsWith(root) && type.endsWith(SLASH_ORIGIN)) {
					int end = type.length() - SLASH_ORIGIN.length();
					return type.substring(0, end + 1);
				}
			}
			return null;
		} finally {
			stmts.close();
		}
	}

	private final TracerService service = TracerService.newInstance();
	private final RealmManager realms;
	private final AuthorizationManager auth;
	private final AuditingRepository auditing;
	private final ObjectRepository object;
	private final RepositoryManager manager;
	private final String repositoryID;
	private String changeFolder;
	private ParserConfig parserConfig;

	public CalliRepository(String repositoryID, ObjectRepositoryManager manager)
			throws OpenRDFException, IOException {
		this(repositoryID, manager.getObjectRepository(repositoryID),
				RepositoryProvider.getRepositoryManager(manager.getLocation()
						.toExternalForm()));
	}

	public CalliRepository(String repositoryID, ObjectRepository repository, RepositoryManager manager)
			throws OpenRDFException, IOException {
		assert manager != null && repository != null;
		this.repositoryID = repositoryID;
		this.manager = manager;
		object = repository;
		auditing = findAuditingRepository(repository, object);
		RepositoryWrapper wrapper = object;
		while (wrapper.getDelegate() instanceof RepositoryWrapper) {
			wrapper = (RepositoryWrapper) wrapper.getDelegate();
		}
		trace(wrapper);
		setDelegate(object);
		CalliObjectSupport.associate(this, object);
		realms = new RealmManager(this);
		auth = new AuthorizationManager(realms, object);
		parserConfig = new ParserConfig();
		parserConfig.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
	}

	public String getRepositoryID() {
		return repositoryID;
	}

	public RepositoryManager getRepositoryManager() {
		return manager;
	}

	public AuthorizationManager getAuthorizationManager() {
		return auth;
	}

	public String getDatasourceRepositoryId(URI uri) {
		int hash = uri.stringValue().hashCode();
		String code = Integer.toHexString(Math.abs(hash));
		String local = uri.getLocalName().replaceAll("[^a-zA-Z0-9\\-.]", "_");
		String id = getRepositoryID();
		StringBuilder sb = new StringBuilder();
		if (id != null) {
			sb.append(id).append("/");
		}
		sb.append("datasources/").append(local.toLowerCase());
		sb.append("-").append(code);
		return sb.toString();
	}

	public void resetCache() {
		getAuthorizationManager().resetCache();
		realms.resetCache();
	}

	public DetachedRealm getRealm(String url) throws OpenRDFException, IOException {
		return realms.getRealm(url);
	}

	public HttpUriClient getHttpClient(final String source) {
		return new HttpRepositoryClient(source);
	}

	@Override
	public ObjectRepository getDelegate() {
		return object;
	}

	public String getChangeFolder() throws OpenRDFException {
		return changeFolder;
	}

	public void setChangeFolder(String uriSpace) throws OpenRDFException {
		setChangeFolder(uriSpace, getCallimachusWebapp(uriSpace));
	}

	public void setChangeFolder(String uriSpace, String webapp)
			throws OpenRDFException {
		this.changeFolder = uriSpace;
		if (auditing != null) {
			if (webapp == null) {
				auditing.setActivityFactory(new CalliActivityFactory(object,
						uriSpace));
			} else {
				ValueFactory vf = object.getValueFactory();
				URI bundle = vf.createURI(webapp + CHANGE_TYPE);
				URI folder = vf.createURI(webapp + FOLDER_TYPE);
				auditing.setActivityFactory(new CalliActivityFactory(object,
						uriSpace, bundle, folder));
			}
		}
	}

	public int getMaxQueryTime() {
		return object.getMaxQueryTime();
	}

	public void setMaxQueryTime(int maxQueryTime) {
		object.setMaxQueryTime(maxQueryTime);
	}

	public boolean isIncludeInferred() {
		return object.isIncludeInferred();
	}

	public void setIncludeInferred(boolean includeInferred) {
		object.setIncludeInferred(includeInferred);
	}

	public BlobStore getBlobStore() throws ObjectStoreConfigException {
		return object.getBlobStore();
	}

	public void setBlobStore(BlobStore store) {
		object.setBlobStore(store);
	}

	public boolean isTracingCalls() {
		for (String prefix : service.getTracingPackages()) {
			if (prefix.equals("org.openrdf.repository"))
				return true;
		}
		return false;
	}

	public void setTracingCalls(boolean trace) {
		if (trace) {
			service.setTracingPackages("org.openrdf.repository", "org.openrdf.query", "org.openrdf.model");
		} else {
			service.setTracingPackages();
		}
	}

	public boolean isLoggingCalls() {
		return isTracingCalls() && service.getLogger(RepositoryConnection.class).isTraceEnabled();
	}

	public void setLoggingCalls(boolean trace) {
		if (trace) {
			setTracingCalls(trace);
			setLoggerLevel("org.openrdf.repository", Level.ALL);
			setLoggerLevel("org.openrdf.query", Level.ALL);
			setLoggerLevel("org.openrdf.model", Level.ALL);
		} else {
			setLoggerLevel("org.openrdf.repository", Level.INFO);
			setLoggerLevel("org.openrdf.query", Level.INFO);
			setLoggerLevel("org.openrdf.model", Level.INFO);
		}
	}

	public String[] showActiveCalls() {
		Trace[] threads = service.getActiveCallTraces();
		String[] result = new String[threads.length];
		for (int i=0; i<threads.length; i++) {
			StringWriter sw = new StringWriter();
			PrintWriter w = new PrintWriter(sw);
		
			Trace call = threads[i];
			print(call, w);
			w.flush();
			result[i] = sw.toString();
		}
		return result;
	}

	public String[] showTraceSummary() throws IOException {
		Trace[] traces1 = service.getActiveCallTraces();
		Trace[] traces2 = service.getTracesByAverageTime();
		Trace[] traces3 = service.getTracesByTotalTime();
		Set<Trace> set = new LinkedHashSet<Trace>(traces1.length
				+ traces2.length + traces3.length);
		addEach(traces1, set);
		addEach(traces2, set);
		addEach(traces3, set);
		List<String> list = new ArrayList<String>();
		for (Trace trace : set) {
			StringWriter string = new StringWriter();
			PrintWriter writer = new PrintWriter(string);
			print(trace, writer);
			writer.println();
			list.add(string.toString());
		}
		return list.toArray(new String[list.size()]);
	}

	public void resetTraceAnalysis() {
		service.resetAnalysis();
	}

	public ObjectConnection getConnection() throws RepositoryException {
		ObjectConnection con = object.getConnection();
		if (auditing != null && con.getVersionBundle() == null) {
			URI bundle = con.getInsertContext();
			ActivityFactory activityFactory = auditing.getActivityFactory();
			if (bundle == null && activityFactory != null) {
				ValueFactory vf = getValueFactory();
				URI activityURI = activityFactory.createActivityURI(bundle, vf);
				String str = activityURI.stringValue();
				int h = str.indexOf('#');
				if (h > 0) {
					bundle = vf.createURI(str.substring(0, h));
				} else {
					bundle = activityURI;
				}
			}
			con.setVersionBundle(bundle); // use the same URI for blob version
		}
		con.setParserConfig(parserConfig);
		return con;
	}

	public RepositoryConnection openSchemaConnection()
			throws RepositoryException {
		return manager.getSystemRepository().getConnection();
	}

	/**
	 * Resolves the relative path to the callimachus webapp context installed at
	 * the origin.
	 * 
	 * @param origin
	 *            scheme and authority
	 * @param path
	 *            relative path from the Callimachus webapp context
	 * @return absolute URL of the root + webapp context + path (or null)
	 */
	public String getCallimachusUrl(String origin, String path)
			throws OpenRDFException {
		String webapp = getCallimachusWebapp(origin);
		if (webapp == null)
			return null;
		return TermFactory.newInstance(webapp).resolve(path);
	}

	/**
	 * Locates the location of the Callimachus webapp folder if present in same
	 * origin, given the root folder.
	 * 
	 * @param root
	 *            home folder, absolute URL with '/' as the path
	 * @return folder of the Callimachus webapp (or null)
	 * @throws OpenRDFException
	 */
	public String getCallimachusWebapp(String url) throws OpenRDFException {
		RepositoryConnection con = this.getConnection();
		try {
			return getCallimachusWebapp(url, con);
		} finally {
			con.close();
		}
	}

	private AuditingRepository findAuditingRepository(Repository repository,
			ObjectRepository object) throws RepositoryConfigException {
		if (repository instanceof AuditingRepository)
			return (AuditingRepository) repository;
		if (repository instanceof RepositoryWrapper)
			return findAuditingRepository(
					((RepositoryWrapper) repository).getDelegate(), object);
		return null;
	}

	private void trace(RepositoryWrapper repository) {
		Repository delegate = repository.getDelegate();
		Repository traced = service.trace(delegate, Repository.class);
		repository.setDelegate(traced);
	}

	private void setLoggerLevel(String fragment, Level level) {
		Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (name.contains(fragment)) {
				Logger logger = Logger.getLogger(name);
				logger.setLevel(level);
				setHandlerLevel(logger, level);
			}
		}
	}

	private void setHandlerLevel(Logger logger, Level level) {
		if (logger.getParent() != null) {
			setHandlerLevel(logger.getParent(), level);
		}
		Handler[] handlers = logger.getHandlers();
		if (handlers != null) {
			for (Handler handler : handlers) {
				if (handler.getLevel().intValue() > level.intValue()) {
					handler.setLevel(level);
				}
			}
		}
	}

	private void addEach(Trace[] traces, Set<Trace> set) {
		set.addAll(Arrays.asList(traces));
		for (Trace call : traces) {
			Trace parent = call;
			while ((parent = parent.getPreviousTrace()) != null) {
				set.remove(parent);
			}
		}
	}

	private static void print(Trace call, PrintWriter w) {
		if (call.getPreviousTrace() != null) {
			print(call.getPreviousTrace(), w);
		}
		for (String assign : call.getAssignments()) {
			w.println(assign);
		}
		w.println(call.toString());
	}

}
