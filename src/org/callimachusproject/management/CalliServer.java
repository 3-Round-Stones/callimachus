package org.callimachusproject.management;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

import javax.mail.MessagingException;

import org.callimachusproject.Version;
import org.callimachusproject.client.HTTPObjectClient;
import org.callimachusproject.logging.LoggingProperties;
import org.callimachusproject.server.WebServer;
import org.callimachusproject.util.MailProperties;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.model.util.GraphUtil;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfig;
import org.openrdf.repository.config.RepositoryConfigException;
import org.openrdf.repository.config.RepositoryConfigSchema;
import org.openrdf.repository.manager.LocalRepositoryManager;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.Rio;
import org.openrdf.rio.helpers.StatementCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalliServer implements CalliServerMXBean {
	private static final String CHANGES_PATH = "../changes/";
	private static final String ERROR_XPL_PATH = "pipelines/error.xpl";
	private static final String ORIGIN = "http://callimachusproject.org/rdf/2009/framework#Origin";

	public static interface ServerListener {
		void serverStarted(WebServer server);

		void serverStopping(WebServer server);
	}

	private final Logger logger = LoggerFactory.getLogger(CalliServer.class);
	private final SetupTool tool;
	private final ServerListener listener;
	private volatile int starting;
	private volatile boolean running;
	private volatile boolean stopping;
	private WebServer server;
	private final LocalRepositoryManager manager;

	public CalliServer(SetupTool tool, ServerListener listener) throws OpenRDFException, IOException {
		this.tool = tool;
		this.listener = listener;
		this.manager = tool.getRepositoryManager();
		File dataDir = manager.getRepositoryDir(getRepositoryID());
		File cacheDir = new File(dataDir, "cache");
		File in = new File(cacheDir, "client");
		HTTPObjectClient.setCacheDirectory(in);
	}

	public String toString() {
		return tool.toString();
	}

	public boolean isRunning() {
		return running;
	}

	public synchronized void init() throws OpenRDFException, IOException {
		if (isWebServiceRunning()) {
			stopWebServiceNow();
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
			stopWebServiceNow();
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
		String name = tool.getProperty("SERVER_NAME");
		if (name == null || name.length() == 0)
			return Version.getInstance().getVersion();
		return name;
	}

	@Override
	public void setServerName(String name) throws IOException {
		if (name == null || name.length() == 0 || name.equals(Version.getInstance().getVersion())) {
			tool.setProperty("SERVER_NAME", null);
		} else {
			tool.setProperty("SERVER_NAME", name);
		}
		if (server != null) {
			server.setServerName(getServerName());
		}
	}

	public String getPorts() throws IOException {
		return tool.getProperty("PORT");
	}

	public void setPorts(String ports) throws IOException {
		if (ports == null || ports.trim().length() < 1) {
			ports = null;
		}
		tool.setProperty("PORT", ports);
	}

	public String getSSLPorts() throws IOException {
		return tool.getProperty("SSLPORT");
	}

	public void setSSLPorts(String ports) throws IOException {
		if (ports == null || ports.trim().length() < 1) {
			ports = null;
		}
		tool.setProperty("SSLPORT", ports);
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
		if (tool == null) {
			startWebServiceNow(start);
		} else {
			tool.submit(new Callable<Void>() {
				public Void call() throws Exception {
					startWebServiceNow(start);
					return null;
				}
			});
		}
	}

	public void stopWebService() throws Exception {
		if (stopping || !isWebServiceRunning())
			return;
		if (tool == null) {
			stopWebServiceNow();
		} else {
			final CountDownLatch latch = new CountDownLatch(1);
			tool.submit(new Callable<Void>() {
				public Void call() throws Exception {
					latch.countDown();
					stopWebServiceNow();
					return null;
				}
			});
			latch.await();
		}
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

	private synchronized void startWebServiceNow(int start) {
		if (start != starting)
			return;
		try {
			if (isWebServiceRunning()) {
				stopWebServiceNow();
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

	private synchronized boolean stopWebServiceNow() {
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

	private String getRepositoryID() throws OpenRDFException, IOException {
		Graph graph = new GraphImpl();
		RDFParser rdfParser = Rio.createParser(RDFFormat.TURTLE);
		rdfParser.setRDFHandler(new StatementCollector(graph));
		String base = new File(".").getAbsoluteFile().toURI().toASCIIString();
		rdfParser.parse(new FileReader(tool.getRepositoryConfigFile()), base);
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
			String value = tool.getProperty("ORIGIN");
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
		for (String origin : tool.getProperty("ORIGIN").trim().split("\\s+")) {
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
		String portStr = tool.getProperty("PORT");
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
		String sslportStr = tool.getProperty("SSLPORT");
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
