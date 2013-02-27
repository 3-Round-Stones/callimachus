package org.callimachusproject.management;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.mail.MessagingException;

import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIUtils;
import org.callimachusproject.Version;
import org.callimachusproject.auth.AuthorizationService;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.io.FileUtil;
import org.callimachusproject.logging.LoggingProperties;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.server.WebServer;
import org.callimachusproject.setup.SetupOrigin;
import org.callimachusproject.setup.SetupTool;
import org.callimachusproject.util.CallimachusConf;
import org.callimachusproject.util.ConfigTemplate;
import org.callimachusproject.util.MailProperties;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.SystemRepository;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.store.blob.BlobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalliServer implements CalliServerMXBean {
	private static final String CHANGES_PATH = "../changes/";
	private static final String ERROR_XPL_PATH = "pipelines/error.xpl";
	private static final String SCHEMA_GRAPH = "types/SchemaGraph";
	private static final String IDENTITY_PATH = "/diverted;";
	private static final String ORIGIN = "http://callimachusproject.org/rdf/2009/framework#Origin";
	private static final String REPOSITORY_TYPES = "META-INF/templates/repository-types.properties";
	private static final ThreadFactory THREADFACTORY = new ThreadFactory() {
		public Thread newThread(Runnable r) {
			String name = CalliServer.class.getSimpleName() + "-"
					+ Integer.toHexString(r.hashCode());
			Thread t = new Thread(r, name);
			t.setDaemon(true);
			return t;
		}
	};

	public static interface ServerListener {
		void repositoryInitialized(String repositoryID, CalliRepository repository);

		void repositoryShutDown(String repositoryID);

		void webServiceStarted(WebServer server);

		void webServiceStopping(WebServer server);
	}

	private final Logger logger = LoggerFactory.getLogger(CalliServer.class);
	private final ExecutorService executor = Executors
			.newSingleThreadScheduledExecutor(THREADFACTORY);
	private final CallimachusConf conf;
	private final ServerListener listener;
	private final File serverCacheDir;
	private volatile int starting;
	private volatile boolean running;
	private volatile boolean stopping;
	private int processing;
	private Exception exception;
	WebServer server;
	private final LocalRepositoryManager manager;
	private final Map<String, CalliRepository> repositories = new LinkedHashMap<String, CalliRepository>();

	public CalliServer(CallimachusConf conf, LocalRepositoryManager manager,
			ServerListener listener) throws OpenRDFException, IOException {
		this.conf = conf;
		this.listener = listener;
		this.manager = manager;
		String tmpDirStr = System.getProperty("java.io.tmpdir");
		if (tmpDirStr == null) {
			tmpDirStr = "tmp";
		}
		File tmpDir = new File(tmpDirStr);
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
		File cacheDir = new File(tmpDir, "cache");
		File in = new File(cacheDir, "client");
		FileUtil.deleteOnExit(cacheDir);
		HTTPObjectClient.setCacheDirectory(in);
		serverCacheDir = new File(cacheDir, "server");
	}

	public String toString() {
		return manager.getBaseDir().toString();
	}

	public boolean isRunning() {
		return running;
	}

	public synchronized void init() throws OpenRDFException, IOException {
		if (isWebServiceRunning()) {
			stopWebServiceNow();
		}
		try {
			if (isThereAnOriginSetup()) {
				server = createServer();
			} else {
				logger.warn("No Web origin is setup on this server");
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
		} catch (OpenRDFException e) {
			logger.error(e.toString(), e);
		} catch (GeneralSecurityException e) {
			logger.error(e.toString(), e);
		} finally {
			if (server == null) {
				shutDownRepositories();
			}
		}
	}

	public synchronized void start() throws IOException, OpenRDFException {
		running = true;
		notifyAll();
		if (server != null) {
			try {
				logger.info("Callimachus is binding to {}", toString());
				for (SetupOrigin origin : getOrigins()) {
					HttpHost host = URIUtils.extractHost(java.net.URI.create(origin.getRoot()));
					HTTPObjectClient.getInstance().setProxy(host, server);
				}
				for (CalliRepository repository : repositories.values()) {
					repository.setCompileRepository(true);
				}
				server.start();
				System.gc();
				Thread.yield();
				long uptime = ManagementFactory.getRuntimeMXBean().getUptime();
				logger.info("Callimachus started after {} seconds", uptime / 1000.0);
				if (listener != null) {
					listener.webServiceStarted(server);
				}
			} catch (IOException e) {
				logger.error(e.toString(), e);
			} catch (OpenRDFException e) {
				logger.error(e.toString(), e);
			}
		}
	}

	public synchronized void stop() throws IOException, OpenRDFException {
		running = false;
		if (isWebServiceRunning()) {
			stopWebServiceNow();
		}
		notifyAll();
	}

	public synchronized void destroy() throws IOException, OpenRDFException {
		stop();
		shutDownRepositories();
		manager.shutDown();
		notifyAll();
	}

	@Override
	public String getServerName() throws IOException {
		String name = conf.getServerName();
		if (name == null || name.length() == 0)
			return Version.getInstance().getVersion();
		return name;
	}

	@Override
	public void setServerName(String name) throws IOException {
		if (name == null || name.length() == 0 || name.equals(Version.getInstance().getVersion())) {
			conf.setServerName(null);
		} else {
			conf.setServerName(name);
		}
		if (server != null) {
			server.setName(getServerName());
		}
	}

	public String getPorts() throws IOException {
		int[] ports = getPortArray();
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<ports.length; i++) {
			sb.append(ports[i]);
			if (i <ports.length - 1) {
				sb.append(' ');
			}
		}
		return sb.toString();
	}

	public void setPorts(String portStr) throws IOException {
		int[] ports = new int[0];
		if (portStr != null && portStr.trim().length() > 0) {
			String[] values = portStr.trim().split("\\s+");
			ports = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				ports[i] = Integer.parseInt(values[i]);
			}
		}
		conf.setPorts(ports);
		if (server != null) {
			server.listen(getPortArray(), getSSLPortArray());
		}
	}

	public String getSSLPorts() throws IOException {
		int[] ports = getSSLPortArray();
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<ports.length; i++) {
			sb.append(ports[i]);
			if (i <ports.length - 1) {
				sb.append(' ');
			}
		}
		return sb.toString();
	}

	public void setSSLPorts(String portStr) throws IOException {
		int[] ports = new int[0];
		if (portStr != null && portStr.trim().length() > 0) {
			String[] values = portStr.trim().split("\\s+");
			ports = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				ports[i] = Integer.parseInt(values[i]);
			}
		}
		conf.setSslPorts(ports);
		if (server != null) {
			server.listen(getPortArray(), getSSLPortArray());
		}
	}

	public boolean isStartingInProgress() {
		return starting > 0;
	}

	public boolean isStoppingInProgress() {
		return stopping;
	}

	public boolean isWebServiceRunning() {
		return server != null && server.isRunning();
	}

	public synchronized void startWebService() throws Exception {
		if (isWebServiceRunning())
			return;
		final int start = ++starting;
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				startWebServiceNow(start);
				return null;
			}
		});
	}

	public void restartWebService() throws Exception {
		final int start = ++starting;
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				stopWebServiceNow();
				startWebServiceNow(start);
				return null;
			}
		});
	}

	public void stopWebService() throws Exception {
		if (stopping || !isWebServiceRunning())
			return;
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				stopWebServiceNow();
				return null;
			}
		});
	}

	@Override
	public Map<String, String> getMailProperties() throws IOException {
		return MailProperties.getInstance().getMailProperties();
	}

	@Override
	public void setMailProperties(Map<String, String> lines)
			throws IOException, MessagingException {
		MailProperties.getInstance().setMailProperties(lines);
	}

	@Override
	public Map<String, String> getLoggingProperties() throws IOException {
		return LoggingProperties.getInstance().getLoggingProperties();
	}

	@Override
	public void setLoggingProperties(Map<String, String> lines)
			throws IOException, MessagingException {
		LoggingProperties.getInstance().setLoggingProperties(lines);
	}

	@Override
	public String[] getRepositoryIDs() throws OpenRDFException {
		Set<String> set = manager.getRepositoryIDs();
		set = new LinkedHashSet<String>(set);
		set.remove(SystemRepository.ID);
		return set.toArray(new String[0]);
	}

	public Map<String,String> getRepositoryProperties() throws IOException, OpenRDFException {
		Map<String, String> map = getAllRepositoryProperties();
		map = new LinkedHashMap<String, String>(map);
		Iterator<String> iter = map.keySet().iterator();
		while (iter.hasNext()) {
			if (iter.next().contains("password")) {
				iter.remove();
			}
		}
		return map;
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

	public synchronized void setRepositoryProperties(Map<String,String> parameters)
			throws IOException, OpenRDFException {
		Map<String, Map<String, String>> params;
		Map<String, String> combined;
		combined = getAllRepositoryProperties();
		combined = new LinkedHashMap<String, String>(combined);
		combined.putAll(parameters);
		params = groupBeforePeriod(combined);
		Set<String> removed = new LinkedHashSet<String>(params.keySet());
		removed.removeAll(groupBeforePeriod(parameters).keySet());
		for (String repositoryID : removed) {
			if (manager.removeRepository(repositoryID)) {
				logger.warn("Removed repository {}", repositoryID);
			}
		}
		combined = getAllRepositoryProperties();
		combined = new LinkedHashMap<String, String>(combined);
		combined.putAll(parameters);
		params = groupBeforePeriod(combined);
		for (String repositoryID : params.keySet()) {
			Map<String, String> pmap = params.get(repositoryID);
			String type = pmap.get(null);
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
				if (!properties.containsKey(type))
					continue;
				String path = properties.getProperty(type);
				Enumeration<URL> configs = cl.getResources(path);
				while (configs.hasMoreElements()) {
					URL url = configs.nextElement();
					ConfigTemplate temp = new ConfigTemplate(url);
					RepositoryConfig config = temp.render(pmap);
					if (config == null)
						throw new RepositoryConfigException("Missing parameters for " + repositoryID);
					if (manager.hasRepositoryConfig(repositoryID)) {
						RepositoryConfig oldConfig = manager.getRepositoryConfig(repositoryID);
						if (temp.getParameters(config).equals(temp.getParameters(oldConfig)))
							continue;
						config.validate();
						logger.info("Replacing repository configuration {}", repositoryID);
						manager.addRepositoryConfig(config);
					} else {
						config.validate();
						logger.info("Creating repository {}", repositoryID);
						manager.addRepositoryConfig(config);
					}
					if (manager.getInitializedRepositoryIDs().contains(repositoryID)) {
						manager.getRepository(repositoryID).shutDown();
					}
					try {
						Repository repo = manager.getRepository(repositoryID);
						RepositoryConnection conn = repo.getConnection();
						try {
							// just check that we can access the repository
							conn.hasStatement(null, null, null, false);
						} finally {
							conn.close();
						}
					} catch (OpenRDFException e) {
						logger.error(e.toString(), e);
						throw new RepositoryConfigException(e.toString());
					}
				}
			}
		}
	}

	public synchronized void checkForErrors() throws Exception {
		try {
			if (exception != null)
				throw exception;
		} finally {
			exception = null;
		}
	}

	public synchronized boolean isSetupInProgress() {
		return processing > 0;
	}

	public String[] getWebappOrigins() throws IOException {
		return conf.getWebappOrigins();
	}

	@Override
	public void setupWebappOrigin(final String webappOrigin,
			final String repositoryID) throws Exception {
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				CalliRepository repository = getInitializedRepository(repositoryID);
				Repository repo = manager.getRepository(repositoryID);
				File dataDir = manager.getRepositoryDir(repositoryID);
				if (repository == null) {
					repository = new CalliRepository(repo, dataDir);
					String changes = repository.getCallimachusUrl(webappOrigin, CHANGES_PATH);
					if (changes != null) {
						repository.setChangeFolder(changes);
					}
				} else {
					BlobStore blobs = repository.getBlobStore();
					String changes = repository.getChangeFolder();
					repository = new CalliRepository(repo, dataDir);
					if (changes != null) {
						repository.setChangeFolder(changes);
					}
					if (blobs != null) {
						repository.setBlobStore(blobs);
					}
				}
				SetupTool tool = new SetupTool(repositoryID, repository, conf);
				tool.setupWebappOrigin(webappOrigin);
				refreshRepository(repositoryID);
				if (isWebServiceRunning()) {
					server.addOrigin(webappOrigin, getRepository(repositoryID));
					server.setIdentityPrefix(getIdentityPrefixes());
					HttpHost host = URIUtils.extractHost(java.net.URI.create(webappOrigin + "/"));
					HTTPObjectClient.getInstance().setProxy(host, server);
				}
				return null;
			}
		});
	}

	@Override
	public void ignoreWebappOrigin(String webappOrigin) throws Exception {
		List<String> list = new ArrayList<String>(Arrays.asList(conf.getWebappOrigins()));
		list.remove(webappOrigin);
		for (SetupOrigin origin : getOrigins()) {
			if (webappOrigin.equals(origin.getWebappOrigin())) {
				list.remove(origin.getRoot().replaceAll("/$", ""));
			}
		}
		conf.setWebappOrigins(list.toArray(new String[list.size()]));
	}

	public SetupOrigin[] getOrigins() throws IOException, OpenRDFException {
		List<SetupOrigin> list = new ArrayList<SetupOrigin>();
		Set<String> webapps = new LinkedHashSet<String>(Arrays.asList(getWebappOrigins()));
		Map<String, String> map = conf.getOriginRepositoryIDs();
		for (String repositoryID : new LinkedHashSet<String>(map.values())) {
			CalliRepository repository = getRepository(repositoryID);
			SetupTool tool = new SetupTool(repositoryID, repository, conf);
			SetupOrigin[] origins = tool.getOrigins();
			for (SetupOrigin origin : origins) {
				webapps.remove(origin.getWebappOrigin());
			}
			list.addAll(Arrays.asList(origins));
		}
		for (String webappOrigin : webapps) {
			// this webapp are not yet setup in the store
			String id = map.get(webappOrigin);
			list.add(new SetupOrigin(webappOrigin, id));
		}
		return list.toArray(new SetupOrigin[list.size()]);
	}

	public void setupResolvableOrigin(final String origin, final String webappOrigin)
			throws Exception {
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				SetupTool tool = getSetupTool(webappOrigin);
				tool.setupResolvableOrigin(origin, webappOrigin);
				if (isWebServiceRunning()) {
					server.addOrigin(origin, getRepository(tool.getRepositoryID()));
					server.setIdentityPrefix(getIdentityPrefixes());
					HttpHost host = URIUtils.extractHost(java.net.URI.create(origin + "/"));
					HTTPObjectClient.getInstance().setProxy(host, server);
				}
				return null;
			}
		});
	}

	public void setupRootRealm(final String realm, final String webappOrigin)
			throws Exception {
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				getSetupTool(webappOrigin).setupRootRealm(realm, webappOrigin);
				return null;
			}
		});
	}

	public String[] getDigestEmailAddresses(String webappOrigin)
			throws OpenRDFException, IOException {
		return getSetupTool(webappOrigin).getDigestEmailAddresses(webappOrigin);
	}

	public void inviteAdminUser(final String email, final String username,
			final String label, final String comment, final String subject,
			final String body, final String webappOrigin) throws Exception {
		submit(new Callable<Void>() {
			public Void call() throws Exception {
				SetupTool tool = getSetupTool(webappOrigin);
				tool.inviteAdminUser(email, username, label, comment, subject,
						body, webappOrigin);
				String repositoryID = tool.getRepositoryID();
				CalliRepository repository = getRepository(repositoryID);
				ObjectRepository object = repository.getDelegate();
				AuthorizationService.getInstance().get(object).resetCache();
				return null;
			}
		});
	}

	public boolean changeDigestUserPassword(String email, String password,
			String webappOrigin) throws OpenRDFException, IOException {
		return getSetupTool(webappOrigin).changeDigestUserPassword(email,
				password, webappOrigin);
	}

	synchronized void saveError(Exception exc) {
		exception = exc;
	}

	synchronized void begin() {
		processing++;
	}

	synchronized void end() {
		processing--;
		notifyAll();
	}

	protected void submit(final Callable<Void> task)
			throws Exception {
		checkForErrors();
		executor.submit(new Runnable() {
			public void run() {
				begin();
				try {
					task.call();
				} catch (Exception exc) {
					saveError(exc);
				} finally {
					end();
				}
			}
		});
		Thread.yield();
	}

	SetupTool getSetupTool(String webappOrigin) throws OpenRDFException, IOException {
		String repositoryID = conf.getOriginRepositoryIDs().get(webappOrigin);
		CalliRepository repository = getRepository(repositoryID);
		return new SetupTool(repositoryID, repository, conf);
	}

	private Map<String, String> getAllRepositoryProperties()
			throws IOException, OpenRDFException {
		Map<String, String> map = new LinkedHashMap<String, String>();
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
					for (String id : manager.getRepositoryIDs()) {
						if (id.equals(SystemRepository.ID))
							continue;
						RepositoryConfig cfg = manager.getRepositoryConfig(id);
						Map<String, String> params = temp.getParameters(cfg);
						if (params == null || id.indexOf('.') >= 0)
							continue;
						for (Map.Entry<String, String> e : params.entrySet()) {
							map.put(id + '.' + e.getKey(), e.getValue());
						}
						map.put(id, type);
					}
				}
			}
		}
		return map;
	}

	private Map<String, Map<String, String>> groupBeforePeriod(
			Map<String, String> parameters) {
		Map<String, Map<String, String>> params = new LinkedHashMap<String, Map<String,String>>();
		for (Map.Entry<String, String> e : parameters.entrySet()) {
			String id, key;
			int idx = e.getKey().indexOf('.');
			if (idx < 0) {
				id = e.getKey();
				key = null;
			} else {
				id = e.getKey().substring(0, idx);
				key = e.getKey().substring(idx + 1);
			}
			String value = e.getValue();
			Map<String, String> map = params.get(id);
			if (map == null) {
				params.put(id, map = new LinkedHashMap<String, String>());
			}
			map.put(key, value);
		}
		return params;
	}

	synchronized void startWebServiceNow(int start) {
		if (start != starting)
			return;
		try {
			if (isWebServiceRunning()) {
				stopWebServiceNow();
			}
			try {
				if (getPortArray().length == 0 && getSSLPortArray().length == 0)
					throw new IllegalStateException("No TCP port defined for server");
				if (!isThereAnOriginSetup())
					throw new IllegalStateException("Repository origin is not setup");
				if (server == null) {
					server = createServer();
				}
			} finally {
				if (server == null) {
					shutDownRepositories();
				}
			}
			server.start();
			if (listener != null) {
				listener.webServiceStarted(server);
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
		} catch (OpenRDFException e) {
			logger.error(e.toString(), e);
		} catch (GeneralSecurityException e) {
			logger.error(e.toString(), e);
		} finally {
			starting = 0;
			notifyAll();
		}
	}

	synchronized boolean stopWebServiceNow() throws OpenRDFException {
		stopping = true;
		try {
			if (server == null) {
				shutDownRepositories();
				return false;
			} else {
				if (listener != null) {
					listener.webServiceStopping(server);
				}
				server.stop();
				shutDownRepositories();
				server.destroy();
				for (SetupOrigin origin : getOrigins()) {
					HttpHost host = URIUtils.extractHost(java.net.URI.create(origin.getRoot()));
					HTTPObjectClient.getInstance().removeProxy(host, server);
				}
				return true;
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
			return false;
		} finally {
			stopping = false;
			notifyAll();
			server = null;
			shutDownRepositories();
		}
	}

	private boolean isThereAnOriginSetup() throws RepositoryException,
			RepositoryConfigException, IOException {
		Map<String, String> map = conf.getOriginRepositoryIDs();
		for (String repositoryID : new LinkedHashSet<String>(map.values())) {
			if (!manager.hasRepositoryConfig(repositoryID))
				continue;
			Repository repository = manager.getRepository(repositoryID);
			RepositoryConnection conn = repository.getConnection();
			try {
				ValueFactory vf = conn.getValueFactory();
				URI Origin = vf.createURI(ORIGIN);
				for (String origin : map.keySet()) {
					if (!repositoryID.equals(map.get(origin)))
						continue;
					URI subj = vf.createURI(origin + "/");
					if (conn.hasStatement(subj, RDF.TYPE, Origin, false))
						return true; // at least one origin is setup
				}
				return getPortArray().length > 0
						|| getSSLPortArray().length > 0;
			} finally {
				conn.close();
			}
		}
		return false;
	}

	private synchronized WebServer createServer() throws OpenRDFException,
			IOException, NoSuchAlgorithmException {
		WebServer server = new WebServer(serverCacheDir);
		for (SetupOrigin so : getOrigins()) {
			boolean first = repositories.isEmpty();
			if (so.isResolvable()) {
				String origin = so.getRoot().replaceAll("/$", "");
				server.addOrigin(origin, getRepository(so.getRepositoryID()));
				HttpHost host = URIUtils.extractHost(java.net.URI.create(so.getRoot()));
				HTTPObjectClient.getInstance().setProxy(host, server);
				if (first) {
					CalliRepository repo = getRepository(so.getRepositoryID());
					String pipe = repo.getCallimachusUrl(so.getWebappOrigin(), ERROR_XPL_PATH);
					if (pipe == null)
						throw new IllegalArgumentException(
								"Callimachus webapp not setup on: " + so.getWebappOrigin());
					server.setErrorPipe(pipe);
				}
			}
		}
		server.setName(getServerName());
		server.setIdentityPrefix(getIdentityPrefixes());
		server.listen(getPortArray(), getSSLPortArray());
		return server;
	}

	CalliRepository getRepository(String repositoryID)
			throws IOException, OpenRDFException {
		Map<String, String> map = conf.getOriginRepositoryIDs();
		List<String> origins = new ArrayList<String>(map.size());
		for (Map.Entry<String, String> e : map.entrySet()) {
			if (repositoryID.equals(e.getValue())) {
				origins.add(e.getKey());
			}
		}
		CalliRepository repository = repositories.get(repositoryID);
		if (repository != null && repository.isInitialized())
			return repository;
		if (origins.isEmpty())
			return null;
		Repository repo = manager.getRepository(repositoryID);
		File dataDir = manager.getRepositoryDir(repositoryID);
		repository = new CalliRepository(repo, dataDir);
		String changes = repository.getCallimachusUrl(origins.get(0), CHANGES_PATH);
		if (changes != null) {
			repository.setChangeFolder(changes);
		}
		for (String origin : origins) {
			String schema = repository.getCallimachusUrl(origin, SCHEMA_GRAPH);
			if (schema != null) {
				repository.addSchemaGraphType(schema);
			}
		}
		repository.setCompileRepository(true);
		if (listener != null && repositories.containsKey(repositoryID)) {
			listener.repositoryShutDown(repositoryID);
		}
		repositories.put(repositoryID, repository);
		if (listener != null) {
			listener.repositoryInitialized(repositoryID, repository);
		}
		return repository;
	}

	synchronized CalliRepository getInitializedRepository(String repositoryID) {
		CalliRepository repository = repositories.get(repositoryID);
		if (repository == null || !repository.isInitialized())
			return null;
		return repository;
	}

	synchronized void refreshRepository(String repositoryID) {
		repositories.remove(repositoryID);
	}

	private synchronized void shutDownRepositories() throws OpenRDFException {
		for (Map.Entry<String, CalliRepository> e : repositories.entrySet()) {
			e.getValue().shutDown();
			if (listener != null) {
				listener.repositoryShutDown(e.getKey());
			}
		}
		repositories.clear();
		if (!manager.getInitializedRepositoryIDs().isEmpty()) {
			manager.refresh();
		}
	}

	private int[] getPortArray() throws IOException {
		return conf.getPorts();
	}

	private int[] getSSLPortArray() throws IOException {
		return conf.getSslPorts();
	}

	private String[] getIdentityPrefixes() throws IOException, OpenRDFException {
		Map<String, String> map = new LinkedHashMap<String, String>();
		for (SetupOrigin origin : getOrigins()) {
			String key = origin.getRepositoryID();
			if (origin.isResolvable() && map.containsKey(key)) {
				map.put(key, null);
			} else if (origin.isResolvable()) {
				map.put(key, origin.getWebappOrigin());
			}
		}
		List<String> identities = new ArrayList<String>(map.size());
		for (String origin : map.values()) {
			if (origin != null) {
				identities.add(origin + IDENTITY_PATH);
			}
		}
		return identities.toArray(new String[identities.size()]);
	}

}
