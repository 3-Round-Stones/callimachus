package org.callimachusproject.api;

import java.net.HttpURLConnection;
import java.net.URL;

import junit.framework.TestCase;

public class OriginOptionsTest extends TestCase {
	
	private TemporaryServer temporaryServer;

	public OriginOptionsTest(String name) throws Exception {
		super(name);
		temporaryServer = TemporaryServer.newInstance();
	}

	@Override
	public void finalize() throws Exception {
		temporaryServer.destroy();
	}

	public void setUp() throws Exception {
		super.setUp();
		temporaryServer.start();
	}

	public void tearDown() throws Exception {
		super.tearDown();
		temporaryServer.stop();
	}
	
	public void test() throws Exception {
		URL url = new java.net.URL(temporaryServer.getOrigin() + "/");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("OPTIONS");
		String header = connection.getHeaderField("LINK");
		assertTrue(header.contains("rel=\"contents\""));
	}

}
