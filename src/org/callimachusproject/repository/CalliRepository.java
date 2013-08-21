package org.callimachusproject.repository;

import info.aduna.net.ParsedURI;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.callimachusproject.client.HttpUriClient;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.io.ArrangedWriter;
import org.callimachusproject.repository.auditing.ActivityFactory;
import org.callimachusproject.repository.auditing.AuditingRepository;
import org.callimachusproject.repository.trace.Trace;
import org.callimachusproject.repository.trace.TracerService;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.Query;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.resultio.text.tsv.SPARQLResultsTSVWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.repository.base.RepositoryWrapper;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.object.exceptions.ObjectStoreConfigException;
import org.openrdf.rio.turtle.TurtleWriter;
import org.openrdf.store.blob.BlobStore;
import org.openrdf.store.blob.file.FileBlobStoreProvider;
import org.slf4j.LoggerFactory;

public class CalliRepository extends RepositoryWrapper implements CalliRepositoryMXBean {
	public class HttpRepositoryClient extends HttpUriClient {
		private final String source;

		private HttpRepositoryClient(String source) {
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
	private static final String FOLDER_TYPE = "types/Folder";

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

	private final org.slf4j.Logger logger = LoggerFactory.getLogger(CalliRepository.class);
	private final TracerService service = TracerService.newInstance();
	private final RealmManager realms;
	private final AuthorizationManager auth;
	private final AuditingRepository auditing;
	private final ObjectRepository object;
	private String changeFolder;

	public CalliRepository(Repository repository, File dataDir)
			throws RepositoryConfigException, RepositoryException,
			IOException {
		assert repository != null;
		object = createObjectRepository(dataDir, repository);
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
	}

	public AuthorizationManager getAuthorizationManager() {
		return auth;
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

	@Override
	public String getChangeFolder() throws OpenRDFException {
		return changeFolder;
	}

	public void setChangeFolder(String uriSpace) throws OpenRDFException {
		setChangeFolder(uriSpace, getCallimachusUrl(uriSpace, ""));
	}

	public void setChangeFolder(String uriSpace, String webapp) throws OpenRDFException {
		this.changeFolder = uriSpace;
		if (auditing != null) {
			if (webapp == null) {
				auditing.setActivityFactory(new CalliActivityFactory(object,
						uriSpace));
			} else {
				String bundle = webapp + CHANGE_TYPE;
				String folder = webapp + FOLDER_TYPE;
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

	public void addSchemaGraph(String graphURI) throws RepositoryException {
		object.addSchemaGraph(getValueFactory().createURI(graphURI));
	}

	public void addSchemaGraphType(String rdfType) throws RepositoryException {
		object.addSchemaGraphType(getValueFactory().createURI(rdfType));
	}

	public void setSchemaGraphType(String rdfType) throws RepositoryException {
		object.setSchemaGraphType(getValueFactory().createURI(rdfType));
	}

	public boolean isCompileRepository() {
		return object.isCompileRepository();
	}

	public void setCompileRepository(boolean compileRepository)
			throws ObjectStoreConfigException, RepositoryException {
		object.setCompileRepository(compileRepository);
	}

	public BlobStore getBlobStore() {
		return object.getBlobStore();
	}

	public void setBlobStore(BlobStore store) {
		object.setBlobStore(store);
	}

	@Override
	public boolean isTracingCalls() {
		for (String prefix : service.getTracingPackages()) {
			if (prefix.equals("org.openrdf.repository"))
				return true;
		}
		return false;
	}

	@Override
	public void setTracingCalls(boolean trace) {
		if (trace) {
			service.setTracingPackages("org.openrdf.repository", "org.openrdf.query", "org.openrdf.model");
		} else {
			service.setTracingPackages();
		}
	}

	@Override
	public boolean isLoggingCalls() {
		return isTracingCalls() && service.getLogger(RepositoryConnection.class).isTraceEnabled();
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
	public void resetTraceAnalysis() {
		service.resetAnalysis();
	}

	public String[] sparqlQuery(String query) throws OpenRDFException, IOException {
		RepositoryConnection conn = this.getConnection();
		try {
			Query qry = conn.prepareQuery(QueryLanguage.SPARQL, query);
			if (qry instanceof TupleQuery) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				SPARQLResultsTSVWriter writer = new SPARQLResultsTSVWriter(out);
				((TupleQuery) qry).evaluate(writer);
				return new String(out.toByteArray(), "UTF-8").split("\r?\n");
			} else if (qry instanceof BooleanQuery) {
				return new String[]{String.valueOf(((BooleanQuery) qry).evaluate())};
			} else if (qry instanceof GraphQuery) {
				StringWriter string = new StringWriter(65536);
				TurtleWriter writer = new TurtleWriter(string);
				((GraphQuery) qry).evaluate(new ArrangedWriter(writer));
				return string.toString().split("(?<=\\.)\r?\n");
			} else {
				throw new RepositoryException("Unknown query type: " + qry.getClass().getSimpleName());
			}
		} finally {
			conn.close();
		}
	}

	public void sparqlUpdate(String update) throws OpenRDFException, IOException {
		RepositoryConnection conn = this.getConnection();
		try {
			logger.info(update);
			conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();
		} finally {
			conn.close();
		}
	}

	public String getBlob(String uri) throws OpenRDFException, IOException {
		ObjectConnection conn = getConnection();
		try {
			return conn.getBlobObject(uri).getCharContent(true).toString();
		} finally {
			conn.close();
		}
	}

	public void storeBlob(String uri, String content) throws OpenRDFException, IOException {
		ObjectConnection conn = getConnection();
		try {
			logger.warn("Replacing {}", uri);
			Writer writer = conn.getBlobObject(uri).openWriter();
			try {
				writer.write(content);
			} finally {
				writer.close();
			}
		} finally {
			conn.close();
		}
	}

	public boolean addSchemaListener(Runnable action) {
		return object.addSchemaListener(action);
	}

	public boolean removeSchemaListener(Runnable action) {
		return object.removeSchemaListener(action);
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
		return con;
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
	private String getCallimachusWebapp(String url) throws OpenRDFException {
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

	private ObjectRepository createObjectRepository(File dataDir,
			Repository repository) throws RepositoryConfigException,
			RepositoryException, IOException {
		if (repository instanceof ObjectRepository)
			return (ObjectRepository) repository;
		// AdviceService used in ObjectRepository#compileSchema
		// uses java.util.ServiceLoader is a non-thread safe way
		synchronized (CalliRepository.class) {
			ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
			ObjectRepositoryConfig config = factory.getConfig();
			config.setIncludeInferred(false);
			config.setObjectDataDir(dataDir);
			File wwwDir = new File(dataDir, "www");
			File blobDir = new File(dataDir, "blob");
			if (wwwDir.isDirectory() && !blobDir.isDirectory()) {
				config.setBlobStore(wwwDir.toURI().toString());
				Map<String, String> map = new HashMap<String, String>();
				map.put("provider", FileBlobStoreProvider.class.getName());
				config.setBlobStoreParameters(map);
			} else {
				config.setBlobStore(blobDir.toURI().toString());
			}
			return factory.createRepository(config, repository);
		}
	}

	private void setLoggerLevel(String fragment, Level level) {
		boolean found = false;
		Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (name.contains(fragment)) {
				Logger logger = Logger.getLogger(name);
				logger.setLevel(level);
				setHandlerLevel(logger, level);
				found = true;
			}
		}
		if (!found)
			throw new IllegalArgumentException("No such logger: " + fragment);
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
