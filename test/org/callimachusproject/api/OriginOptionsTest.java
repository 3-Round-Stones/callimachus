package org.callimachusproject.api;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import junit.framework.TestCase;

public class OriginOptionsTest extends TestCase {
	
	private TemporaryServer temporaryServer = TemporaryServer.getInstance();

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
