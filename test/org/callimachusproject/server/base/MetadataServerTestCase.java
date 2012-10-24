package org.callimachusproject.server.base;

import info.aduna.io.FileUtil;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.ConnectException;
import java.util.Random;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.callimachusproject.server.CallimachusRepository;
import org.callimachusproject.server.HTTPObjectServer;
import org.callimachusproject.server.concepts.AnyThing;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.sail.Sail;
import org.openrdf.sail.auditing.AuditingSail;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.sail.optimistic.OptimisticRepository;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;

public abstract class MetadataServerTestCase extends TestCase {

	static {
		if (System.getProperty("ebug") != null) {
			Logger logger = Logger.getLogger("");
			ConsoleHandler ch = new ConsoleHandler() {
				public void publish(LogRecord record) {
					if (record.getLoggerName().startsWith(
							"org.openrdf.http.object"))
						super.publish(record);
				}
			};
			ch.setFormatter(new Formatter() {
				public String format(LogRecord record) {
			        StringBuffer sb = new StringBuffer();
			        String message = formatMessage(record);
			        sb.append(record.getLevel().getLocalizedName());
			        sb.append(": ");
			        sb.append(message);
			        sb.append("\r\n");
			        if (record.getThrown() != null) {
			            try {
			                StringWriter sw = new StringWriter();
			                PrintWriter pw = new PrintWriter(sw);
			                record.getThrown().printStackTrace(pw);
			                pw.close();
			                sb.append(sw.toString());
			            } catch (Exception ex) {
			            }
			        }
			        return sb.toString();
			    }
			});
			ch.setLevel(Level.ALL);
			logger.addHandler(ch);
			logger.setLevel(Level.FINE);
		}
	}
	private static int MIN_PORT = 49152;
	private static int MAX_PORT = 65535;
	private static volatile int seed = 0;
	private final Random rand = new Random();
	private int port;
	private boolean failed;
	protected CallimachusRepository repository;
	protected ObjectRepositoryConfig config = new ObjectRepositoryConfig();
	protected HTTPObjectServer server;
	protected File dataDir;
	protected WebResource client;
	protected ValueFactory vf;
	protected String base;

	@Override
	public void setUp() throws Exception {
		failed = false;
		dataDir = FileUtil.createTempDir("metadata");
		if (config.getBlobStore() == null) {
			config.setBlobStore(dataDir.toURI().toString());
		}
		config.addConcept(AnyThing.class);
		repository = createRepository();
		vf = repository.getValueFactory();
		initDataset(repository);
		server = createServer();
		server.listen(new int[] { getPort() }, new int[0]);
		server.start();
		server.resetCache();
		client = Client.create().resource(getOrigin());
		addContentEncoding(client);
		base = client.getURI().toASCIIString();
		Thread.sleep(100);
	}

	protected HTTPObjectServer createServer() throws Exception {
		return new HTTPObjectServer(repository, new File(dataDir, "cache"));
	}

	protected void addContentEncoding(WebResource client) {
		client.addFilter(new GZIPContentEncodingFilter());
	}

	@Override
	public void tearDown() throws Exception {
		server.stop();
		server.destroy();
		if (failed) {
			ObjectConnection con = repository.getConnection();
			try {
				con.export(new TriGWriter(System.out));
			} finally {
				con.close();
			}
		}
		repository.shutDown();
		FileUtil.deltree(dataDir);
		Thread.sleep(100);
		port = 0;
	}

	@Override
	protected void runTest() throws Throwable {
		try {
			try {
				super.runTest();
			} catch (UniformInterfaceException cause) {
				ClientResponse msg = cause.getResponse();
				String body = msg.getEntity(String.class);
				System.out.println(body);
				UniformInterfaceException e = new UniformInterfaceException(body, msg);
				e.initCause(cause);
				throw e;
			} catch (ClientHandlerException e) {
				if (e.getCause() instanceof ConnectException) {
					System.out.println("Could not connect to port "
							+ port);
				}
				throw e;
			}
		} catch (Throwable t) {
			failed = true;
			throw t;
		}
	}

	@Override
	public void runBare() throws Throwable {
		try {
			super.runBare();
		} catch (UniformInterfaceException cause) {
			ClientResponse msg = cause.getResponse();
			String body = msg.getEntity(String.class);
			System.out.println(body);
			UniformInterfaceException e = new UniformInterfaceException(body, msg);
			e.initCause(cause);
			throw e;
		} catch (ClientHandlerException e) {
			if (e.getCause() instanceof ConnectException) {
				System.out.println("Could not connect to port "
						+ port);
			}
			throw e;
		}
	}

	protected String getOrigin() {
		return "http://localhost:" + getPort();
	}

	protected int getPort() {
		if (port > MIN_PORT)
			return port;
		int range = (MAX_PORT - MIN_PORT) / 2;
		seed += this.getClass().getName().hashCode() + rand.nextInt();
		return port = (seed % range) + range + MIN_PORT;
	}

	private CallimachusRepository createRepository() throws Exception {
		Sail sail = new MemoryStore(dataDir);
		sail = new AuditingSail(sail);
		Repository repo = new OptimisticRepository(sail);
		repo.initialize();
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		repo = factory.createRepository(config, repo);
		return new CallimachusRepository(repo, dataDir);
	}

	private void initDataset(CallimachusRepository repository) throws RepositoryException {
		ObjectConnection con = repository.getConnection();
		try {
			ValueFactory vf = con.getValueFactory();
			URI anonymousFrom = vf.createURI("http://callimachusproject.org/rdf/2009/framework#anonymousFrom");
			con.add(vf.createURI("urn:test:anybody"), anonymousFrom, vf.createLiteral("."));
		} finally {
			con.close();
		}
	}

}
