package org.callimachusproject.test;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.callimachusproject.server.CallimachusRepository;
import org.junit.After;
import org.junit.Before;
import org.openrdf.OpenRDFException;

public abstract class TemporaryServerTestCase extends TestCase {
    private static final TemporaryServerFactory factory = TemporaryServerFactory.getInstance();
    private static final Map<Class<?>, TemporaryServer> servers = new HashMap<Class<?>, TemporaryServer>();
	private final TemporaryServer server;

	public TemporaryServerTestCase() {
		this(null);
	}

	public TemporaryServerTestCase(String name) {
		super(name);
		synchronized (servers) {
			if (servers.containsKey(getClass())) {
				server = servers.get(getClass());
			} else {
				servers.put(getClass(), server = factory.createServer());
			}
		}
	}

	@Before
	public void setUp() throws Exception {
		super.setUp();
		server.resume();
		Authenticator.setDefault(new Authenticator() {
		     protected PasswordAuthentication getPasswordAuthentication() {
		       return new PasswordAuthentication(server.getUsername(), server.getPassword()); 
		     }
		 });
	}

	@After
	public void tearDown() throws Exception {
		super.tearDown();
		server.pause();
		Authenticator.setDefault(null);
	}

	public WebResource getHomeFolder() throws IOException, OpenRDFException {
		return getCallimachusUrl("/");
	}

	public WebResource getCallimachusUrl(String path) throws IOException,
			OpenRDFException {
		String origin = server.getOrigin();
		String url = getRepository().getCallimachusUrl(origin, path);
		return new WebResource(url);
	}

	public String getUsername() {
		return server.getUsername();
	}

	public char[] getPassword() {
		return server.getPassword();
	}

	public CallimachusRepository getRepository() {
		return server.getRepository();
	}

}
