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

import org.callimachusproject.behaviours.CalliObjectSupport;
import org.callimachusproject.repository.auditing.AuditingRepository;
import org.callimachusproject.repository.auditing.config.AuditingRepositoryFactory;
import org.callimachusproject.sail.auditing.AuditingSail;
import org.callimachusproject.server.concepts.AnyThing;
import org.openrdf.http.object.management.WebServer;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.sail.Sail;
import org.openrdf.sail.memory.MemoryStore;

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
	protected ObjectRepository repository;
	protected ObjectRepositoryConfig config = new ObjectRepositoryConfig();
	protected WebServer server;
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
		server.addRepository(getOrigin(), repository);
		server.listen(new int[] { getPort() }, new int[0]);
		server.start();
		server.resetCache();
		client = Client.create().resource(getOrigin());
		addContentEncoding(client);
		base = client.getURI().toASCIIString();
		Thread.sleep(500);
	}

	protected WebServer createServer() throws Exception {
		File dir = new File(dataDir, "cache");
		dir.mkdirs();
		return new WebServer(dir);
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
		Thread.sleep(500);
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

	private ObjectRepository createRepository() throws Exception {
		dataDir.mkdirs();
		Sail sail = new MemoryStore(dataDir);
		sail = new AuditingSail(sail);
		Repository delegate = new SailRepository(sail);
		AuditingRepositoryFactory af = new AuditingRepositoryFactory();
		AuditingRepository auditing = af.getRepository(af.getConfig());
		auditing.setDelegate(delegate);
		auditing.setDataDir(dataDir);
		auditing.initialize();
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		return factory.createRepository(config, auditing);
	}

	private void initDataset(ObjectRepository repository) throws Exception {
		CalliObjectSupport.getCalliRepositroyFor(repository).setChangeFolder("http://example.com/changes/");
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
