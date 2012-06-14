package org.callimachusproject.server.api;

import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.TestCase;

public class OriginOptionsTest extends TestCase {

	private static TemporaryServer temporaryServer = TemporaryServerFactory.getInstance().createServer();

	public OriginOptionsTest(String name) throws Exception {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
		temporaryServer.resume();
	}

	public void tearDown() throws Exception {
		super.tearDown();
		temporaryServer.pause();
	}
	
	public void test() throws Exception {
		URL url = new java.net.URL(temporaryServer.getOrigin() + "/");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("OPTIONS");
		String header = connection.getHeaderField("LINK");
		assertTrue(header.contains("rel=\"contents\""));
	}

}
