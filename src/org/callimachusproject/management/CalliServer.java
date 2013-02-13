package org.callimachusproject.management;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;

import org.callimachusproject.Version;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.io.ArrangedWriter;
import org.callimachusproject.server.WebServer;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
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
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.repository.manager.RepositoryProvider;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.turtle.TurtleWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalliServer implements CalliServerMXBean {
	private static final String CHANGES_PATH = "../changes/";
	private static final String ERROR_XPL_PATH = "pipelines/error.xpl";
	private static final String REPOSITORY_CONFIG = "callimachus-repository.ttl";
	private static final String ORIGIN = "http://callimachusproject.org/rdf/2009/framework#Origin";

	public static interface ServerListener {
		void serverStarted(WebServer server);

		void serverStopping(WebServer server);
	}

	private final Logger logger = LoggerFactory.getLogger(CalliServer.class);
	private final LocalRepositoryManager manager;
	private final CallimachusConf conf;
	private final SetupTool tool;
	private final ServerListener listener;
	private volatile int starting;
	private volatile boolean running;
	private volatile boolean stopping;
	private WebServer server;

	public CalliServer(File baseDir, SetupTool tool, ServerListener listener) throws OpenRDFException, IOException {
		this.manager = RepositoryProvider.getRepositoryManager(baseDir);
		File dataDir = manager.getRepositoryDir(getRepositoryID());
		File cacheDir = new File(dataDir, "cache");
		File in = new File(cacheDir, "client");
		HTTPObjectClient.setCacheDirectory(in);
		this.conf = CallimachusConf.getInstance();
		this.tool = tool;
		this.listener = listener;
	}

	public String toString() {
		return manager.getBaseDir().toString();
	}

	public boolean isRunning() {
		return running;
	}

	public synchronized void init() throws OpenRDFException, IOException {
		if (isWebServiceRunning()) {
			stopWebService();
		}
		try {
			String repositoryID = getRepositoryID();
			if (isServerInstalled(repositoryID)) {
				server = createServer(repositoryID);
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
				manager.refresh();
			}
		}
	}

	public synchronized void start() throws IOException, OpenRDFException {
		running = true;
		notifyAll();
		if (server != null) {
			try {
				server.start();
				if (listener != null) {
					listener.serverStarted(server);
				}
			} catch (IOException e) {
				logger.error(e.toString(), e);
			} catch (OpenRDFException e) {
				logger.error(e.toString(), e);
			}
		}
	}

	public synchronized void stop() throws IOException {
		running = false;
		if (isWebServiceRunning()) {
			stopWebService();
		}
		notifyAll();
	}

	public synchronized void destroy() {
		running = false;
		manager.shutDown();
		notifyAll();
	}

	@Override
	public String getServerName() throws IOException {
		String name = conf.getProperty("SERVER_NAME");
		if (name == null || name.length() == 0)
			return Version.getInstance().getVersion();
		return name;
	}

	@Override
	public void setServerName(String name) throws IOException {
		if (name == null || name.length() == 0 || name.equals(Version.getInstance().getVersion())) {
			conf.setProperty("SERVER_NAME", null);
		} else {
			conf.setProperty("SERVER_NAME", name);
		}
		if (server != null) {
			server.setServerName(getServerName());
		}
	}

	public String getPorts() throws IOException {
		return conf.getProperty("PORT");
	}

	public void setPorts(String ports) throws IOException {
		if (ports == null || ports.trim().length() < 1) {
			ports = null;
		}
		conf.setProperty("PORT", ports);
	}

	public String getSSLPorts() throws IOException {
		return conf.getProperty("SSLPORT");
	}

	public void setSSLPorts(String ports) throws IOException {
		if (ports == null || ports.trim().length() < 1) {
			ports = null;
		}
		conf.setProperty("SSLPORT", ports);
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
		final int start = ++starting;
		if (tool == null) {
			startServerNow(start);
		} else {
			tool.submit(new Callable<Void>() {
				public Void call() throws Exception {
					startServerNow(start);
					return null;
				}
			});
		}
	}

	public synchronized boolean stopWebService() {
		stopping = true;
		try {
			if (server == null) {
				manager.refresh();
				return false;
			} else {
				if (listener != null) {
					listener.serverStopping(server);
				}
				server.stop();
				server.destroy();
				return true;
			}
		} catch (IOException e) {
			logger.error(e.toString(), e);
			return false;
		} finally {
			stopping = false;
			notifyAll();
			server = null;
			manager.refresh();
		}
	}

	public String[] sparqlQuery(String query) throws OpenRDFException, IOException {
		String repositoryID = getRepositoryID();
		if (!manager.hasRepositoryConfig(repositoryID))
			throw new RepositoryConfigException("Repository is not initialized");
		Repository repository = manager.getRepository(repositoryID);
		RepositoryConnection conn = repository.getConnection();
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
		String repositoryID = getRepositoryID();
		if (!isServerInstalled(repositoryID))
			throw new RepositoryConfigException("Repository is not initialized");
		Repository repository = manager.getRepository(repositoryID);
		RepositoryConnection conn = repository.getConnection();
		try {
			logger.info(update);
			conn.prepareUpdate(QueryLanguage.SPARQL, update).execute();
		} finally {
			conn.close();
		}
	}

	private synchronized void startServerNow(int start) {
		if (start != starting)
			return;
		try {
			if (isWebServiceRunning()) {
				stopWebService();
			}
			try {
				String repositoryID = getRepositoryID();
				if (!manager.hasRepositoryConfig(repositoryID))
					throw new IllegalStateException("Missing repository config: " + repositoryID);
				if (getPortArray().length == 0 && getSSLPortArray().length == 0)
					throw new IllegalStateException("No TCP port defined for server");
				if (!isServerInstalled(repositoryID))
					throw new IllegalStateException("Repository origin is not setup");
				if (server == null) {
					server = createServer(repositoryID);
				}
			} finally {
				if (server == null) {
					manager.refresh();
				}
			}
			server.start();
			if (listener != null) {
				listener.serverStarted(server);
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

	private String getRepositoryID() throws OpenRDFException, IOException {
		File etc = new File(manager.getBaseDir(), "etc");
		Graph graph = new GraphImpl();
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		rdfParser.setRDFHandler(new StatementCollector(graph));
		String base = new File(".").getAbsoluteFile().toURI().toASCIIString();
		rdfParser.parse(new FileReader(new File(etc, REPOSITORY_CONFIG)), base);
		Resource node = GraphUtil.getUniqueSubject(graph, RDF.TYPE,
				RepositoryConfigSchema.REPOSITORY);
		return RepositoryConfig.create(graph, node).getID();
	}

	private boolean isServerInstalled(String repositoryID)
			throws RepositoryException, RepositoryConfigException, IOException {
		if (!manager.hasRepositoryConfig(repositoryID))
			return false;
		Repository repository = manager.getRepository(repositoryID);
		RepositoryConnection conn = repository.getConnection();
		try {
			ValueFactory vf = conn.getValueFactory();
			URI Origin = vf.createURI(ORIGIN);
			String value = conf.getProperty("ORIGIN");
			if (value == null)
				return false;
			String[] origins = value.trim().split("\\s+");
			if (origins.length < 1)
				return false;
			for (String origin : origins) {
				URI subj = vf.createURI(origin + "/");
				if (!conn.hasStatement(subj, RDF.TYPE, Origin, false))
					return false;
			}
			return getPortArray().length > 0 || getSSLPortArray().length > 0;
		} finally {
			conn.close();
		}
	}

	private synchronized WebServer createServer(String repositoryID)
			throws RepositoryConfigException, RepositoryException,
			OpenRDFException, IOException, NoSuchAlgorithmException {
		Repository repository = manager.getRepository(repositoryID);
		File dataDir = manager.getRepositoryDir(repositoryID);
		WebServer server = new WebServer(repository, dataDir);
		boolean primary = true;
		for (String origin : conf.getProperty("ORIGIN").trim().split("\\s+")) {
			if (primary) {
				server.setChangesPath(origin, CHANGES_PATH);
				server.setErrorPipe(origin, ERROR_XPL_PATH);
				primary = false;
			}
			server.addOrigin(origin);
		}
		server.setServerName(getServerName());
		server.listen(getPortArray(), getSSLPortArray());
		return server;
	}

	private int[] getPortArray() throws IOException {
		String portStr = conf.getProperty("PORT");
		int[] ports = new int[0];
		if (portStr != null && portStr.trim().length() > 0) {
			String[] values = portStr.trim().split("\\s+");
			ports = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				ports[i] = Integer.parseInt(values[i]);
			}
		}
		return ports;
	}

	private int[] getSSLPortArray() throws IOException {
		String sslportStr = conf.getProperty("SSLPORT");
		int[] sslports = new int[0];
		if (sslportStr != null && sslportStr.trim().length() > 0) {
			String[] values = sslportStr.trim().split("\\s+");
			sslports = new int[values.length];
			for (int i = 0; i < values.length; i++) {
				sslports[i] = Integer.parseInt(values[i]);
			}
		}
		return sslports;
	}
}
