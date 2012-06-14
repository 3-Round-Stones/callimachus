package org.callimachusproject.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;

import junit.framework.TestCase;

public class KeywordSearchTest extends TestCase {

	private static TemporaryServer temporaryServer = TemporaryServerFactory.getInstance().createServer();

	public KeywordSearchTest(String name) throws Exception {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
		temporaryServer.resume();
		Authenticator.setDefault(new Authenticator() {
		     protected PasswordAuthentication getPasswordAuthentication() {
		       return new PasswordAuthentication(temporaryServer.getUsername(), temporaryServer.getPassword()); 
		     }
		 });
	}

	public void tearDown() throws Exception {
		super.tearDown();
		temporaryServer.pause();
	}

	private String getSearchURL() throws MalformedURLException, IOException,
			ProtocolException {
		URL url = new java.net.URL(temporaryServer.getOrigin() + "/");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("OPTIONS");
		String header = connection.getHeaderField("LINK");
		int rel = header.indexOf("rel=\"search\"");
		int end = header.lastIndexOf(">", rel);
		int start = header.lastIndexOf("<", rel);
		String searchURL = header.substring(start + 1, end);
		return searchURL;
	}
	
	private String getTemplateURL(String searchTerm) throws Exception {
		URL url = new java.net.URL(getSearchURL());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("ACCEPT", "application/opensearchdescription+xml");
		InputStream stream = connection.getInputStream();
		String text = new java.util.Scanner(stream).useDelimiter("\\A").next();
		int pos = text.indexOf("template");
		int start = text.indexOf("\"", pos);
		int stop = text.indexOf("\"", start + 1);
		String templateURL = text.substring(start + 1, stop);
		
		int before = templateURL.indexOf("{");
		int after = templateURL.indexOf("}");
		String firstPart = templateURL.substring(0, before);
		String secondPart = templateURL.substring(after+1);
		String fullURL = firstPart + searchTerm + secondPart;
		return fullURL;
	}
	
	private String getContents(String searchTerm) throws Exception {
		URL url = new java.net.URL(getTemplateURL(searchTerm));
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("ACCEPT", "application/atom+xml");
		InputStream stream = connection.getInputStream();
		String contents = new java.util.Scanner(stream).useDelimiter("\\A").next();
		return contents;
	}
	
	public void testPositiveKeywordSearch() throws Exception {
		String testContents = getContents("test");
		assertTrue(testContents.contains("<entry>"));
		
	}
	
	public void testNegativeKeywordSearch() throws Exception {
		String nothingContents = getContents("nothing");
		assertFalse(nothingContents.contains("<entry>"));
	}
	
}
