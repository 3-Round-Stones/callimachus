package org.callimachusproject.test;

import info.aduna.io.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIUtils;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.repository.CalliRepository;
import org.callimachusproject.server.WebServer;
import org.callimachusproject.setup.CallimachusSetup;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.util.GraphUtilException;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;

public class TemporaryServerFactory {
	private static int MIN_PORT = 49152;
	private static int MAX_PORT = 65535;
	private static final String CHANGES_PATH = "../changes/";
	private static final String SCHEMA_GRAPH = "types/SchemaGraph";
	private static final File WEBAPP_CAR = findCallimachusWebappCar();
	private static final int PORT = findPort(WEBAPP_CAR.getAbsolutePath().hashCode());
	private static final TemporaryServerFactory instance = new TemporaryServerFactory("http://localhost:" + PORT, PORT, "test@example.com", "test".toCharArray());

	public static TemporaryServerFactory getInstance() {
		return instance;
	}

	private static File findCallimachusWebappCar() {
		File dist = new File("dist");
		if (dist.list() != null) {
			for (String file : dist.list()) {
				if (file.startsWith("callimachus-webapp")
						&& file.endsWith(".car"))
					return new File(dist, file);
			}
		}
		throw new AssertionError("Could not find callimachus-webapp.car in "
				+ dist.getAbsolutePath());
	}

	private static int findPort(int seed) {
		int range = (MAX_PORT - MIN_PORT) / 2;
		return (seed % range) + range + MIN_PORT;
	}

	private final String origin;
	private final int port;
	private final String email;
	private final char[] password;
	private final Map<Integer, TemporaryServer> running = new LinkedHashMap<Integer, TemporaryServer>();

	public TemporaryServerFactory(String origin, int port, String email,
			char[] password) {
		assert email.indexOf('@') > 0;
		this.origin = origin;
		this.port = port;
		this.email = email;
		this.password = password;
	}

	public TemporaryServer createServer() {
		return new TemporaryServer() {
			private SoftReference<TemporaryServer> ref;

			private synchronized TemporaryServer getDelegate() {
				TemporaryServer delegate = null;
				if (ref != null) {
					delegate = ref.get();
				}
				if (delegate == null) {
					delegate = createTemporaryServer();
					ref = new SoftReference<TemporaryServer>(delegate);
				}
				return delegate;
			}

			public void start() throws InterruptedException, Exception {
				getDelegate().start();
			}

			public void pause() throws Exception {
				getDelegate().pause();
			}

			public void resume() throws Exception {
				getDelegate().resume();
			}

			public void stop() throws Exception {
				getDelegate().stop();
			}

			public void destroy() throws Exception {
				getDelegate().destroy();
			}

			public String getOrigin() {
				return getDelegate().getOrigin();
			}

			public String getUsername() {
				return getDelegate().getUsername();
			}

			public char[] getPassword() {
				return getDelegate().getPassword();
			}

			@Override
			public CalliRepository getRepository() {
				return getDelegate().getRepository();
			}
		};
	}

	private synchronized TemporaryServer createTemporaryServer() {
		try {
			final File dir = createCallimachus(origin);
			final LocalRepositoryManager manager = RepositoryProvider.getRepositoryManager(dir);
			final File dataDir = manager.getRepositoryDir("callimachus");
			return new TemporaryServer(){
				private final WebServer server = new WebServer(new File(dataDir, "cache/server"));
				private CalliRepository repository;
				private boolean stopped;

				public synchronized void start() throws InterruptedException, Exception {
					Repository repo = manager.getRepository("callimachus");
					repository = new CalliRepository(repo, dataDir);
					String url = repository.getCallimachusUrl(origin, CHANGES_PATH);
					String schema = repository.getCallimachusUrl(origin, SCHEMA_GRAPH);
					repository.addSchemaGraphType(schema);
					repository.setChangeFolder(url);
					repository.setCompileRepository(true);
					server.addOrigin(origin, repository);
					server.listen(new int[]{port}, new int[0]);
					server.start();
					HttpHost host = URIUtils.extractHost(java.net.URI.create(origin + "/"));
					HTTPObjectClient.getInstance().setProxy(host, server);
					Thread.sleep(100);
				}

				public synchronized void pause() throws Exception {
				}

				public synchronized void resume() throws Exception {
					synchronized (running) {
						TemporaryServer other = running.get(port);
						if (stopped || !this.equals(other)) {
							if (other != null) {
								other.stop();
							}
							start();
							running.put(port, this);
						}
					}
				}

				public synchronized void stop() throws Exception {
					if (!stopped) {
						server.stop();
						server.destroy();
						stopped = true;
						Thread.sleep(100);
					}
				}

				public synchronized void destroy() throws Exception {
					stop();
					manager.shutDown();
					FileUtil.deltree(dir);
				}

				@Override
				protected void finalize() throws Exception {
					destroy();
				}

				public String getOrigin() {
					return origin;
				}

				public String getUsername() {
					return email;
				}

				public char[] getPassword() {
					return password;
				}

				public CalliRepository getRepository() {
					return this.repository;
				}

				public String toString() {
					return server.toString();
				}
			};
		} catch (Exception e) {
			throw new AssertionError(e);
		}
	}

	private File createCallimachus(String origin) throws Exception {
		String name = TemporaryServer.class.getSimpleName() + Integer.toHexString(WEBAPP_CAR.getAbsolutePath().hashCode());
		File dir = createDirectory(name);
		if (!dir.isDirectory() || WEBAPP_CAR.lastModified() > dir.lastModified()) {
			if (dir.isDirectory()) {
				FileUtil.deleteDir(dir);
			}
			dir.delete();
			String configStr = new Scanner(new File("etc", "callimachus-repository.ttl")).useDelimiter("\\A").next();
			System.setProperty("org.callimachusproject.config.webapp", WEBAPP_CAR.getAbsolutePath());
			RepositoryConfig config = getRepositoryConfig(configStr);
			LocalRepositoryManager manager = RepositoryProvider.getRepositoryManager(dir);
			Repository repo = getRepository(manager, config);
			if (repo == null)
				throw new RepositoryConfigException(
						"Missing repository configuration");
			File dataDir = manager.getRepositoryDir(config.getID());
			CalliRepository repository = new CalliRepository(repo, dataDir);
			CallimachusSetup setup = new CallimachusSetup(repository);
			setup.prepareWebappOrigin(origin);
			setup.createWebappOrigin(origin);
			setup.finalizeWebappOrigin(origin);
			String username = email.substring(0, email.indexOf('@'));
			setup.createAdmin(email, username, username, null, origin);
			setup.changeUserPassword(email, username, password, origin);
			repository.shutDown();
			manager.shutDown();
		}
		File temp = FileUtil.createTempDir(name);
		copyDir(dir, temp);
		return temp;
	}

	private Repository getRepository(LocalRepositoryManager manager, RepositoryConfig config)
			throws OpenRDFException, MalformedURLException, IOException {
		if (config == null || manager == null)
			return null;
		String id = config.getID();
		if (manager.hasRepositoryConfig(id))
			return manager.getRepository(id);
		config.validate();
		manager.addRepositoryConfig(config);
		if (manager.getInitializedRepositoryIDs().contains(id)) {
			manager.getRepository(id).shutDown();
		}
		return manager.getRepository(id);
	}

	private RepositoryConfig getRepositoryConfig(String configString)
			throws IOException, RDFParseException, RDFHandlerException,
			GraphUtilException, RepositoryConfigException {
		Graph graph = parseTurtleGraph(configString);
		Resource node = GraphUtil.getUniqueSubject(graph, RDF.TYPE,
				RepositoryConfigSchema.REPOSITORY);
		return RepositoryConfig.create(graph, node);
	}

	private Graph parseTurtleGraph(String configString) throws IOException,
			RDFParseException, RDFHandlerException {
		Graph graph = new GraphImpl();
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		rdfParser.setRDFHandler(new StatementCollector(graph));
		String base = new File(".").getAbsoluteFile().toURI().toASCIIString();
		rdfParser.parse(new StringReader(configString), base);
		return graph;
	}

	private void copyDir(File src, File dest) throws IOException {
		if (src.isDirectory()) {
			dest.mkdir();
			for (String file : src.list()) {
				copyDir(new File(src, file), new File(dest, file));
			}
		} else {
			FileUtil.copyFile(src, dest);
		}
	}

	private File createDirectory(String name) throws IOException {
		String tmpDirStr = System.getProperty("java.io.tmpdir");
		if (tmpDirStr != null) {
			File tmpDir = new File(tmpDirStr);
			if (!tmpDir.exists()) {
				tmpDir.mkdirs();
			}
		}
		File tmp = File.createTempFile(name, "");
		tmp.delete();
		return new File(tmp.getParentFile(), name);
	}
}
