package org.callimachusproject.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import junit.framework.TestCase;

public class FolderContentsTest extends TestCase {
	
	private TemporaryServer temporaryServer;

	public FolderContentsTest(String name) throws Exception {
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
		String contents = getRelContents();
		
		URL contentsURL = new java.net.URL(contents);
		HttpURLConnection contentsConnection = (HttpURLConnection) contentsURL.openConnection();
		contentsConnection.setRequestMethod("GET");
		contentsConnection.setRequestProperty("ACCEPT", "application/atom+xml");
		InputStream stream = contentsConnection.getInputStream();
		String text = new java.util.Scanner(stream).useDelimiter("\\A").next();
		assertTrue(text.contains("<app:collection"));
	}

	private String getRelContents() throws MalformedURLException, IOException,
			ProtocolException {
		URL url = new java.net.URL(temporaryServer.getOrigin() + "/");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("OPTIONS");
		String header = connection.getHeaderField("LINK");
		int rel = header.indexOf("rel=\"contents\"");
		int end = header.lastIndexOf(">", rel);
		int start = header.lastIndexOf("<", rel);
		String contents = header.substring(start + 1, end);
		return contents;
	}

}
