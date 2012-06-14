package org.callimachusproject.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;

import junit.framework.TestCase;

public class NamedGraphTest extends TestCase { 
	
	private static TemporaryServer temporaryServer = TemporaryServerFactory.getInstance().createServer();
	private String requestSlug = "namedGraph.xml";
	private String requestContentType = "application/rdf+xml";
	private String outputString = "<rdf:RDF \n" +
			"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n" +
			"xmlns:dcterms=\"http://purl.org/dc/terms/\"> \n" +
		    "<rdf:Description rdf:about=\"urn:x-states:New%20York\"> \n" +
		    "<dcterms:alternative>NY</dcterms:alternative> \n" +
		    "</rdf:Description> \n" +
		    "</rdf:RDF>" ;
	private String updateOutputString = "<rdf:RDF \n" +
		    "xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" \n" +
			"xmlns:dcterms=\"http://purl.org/dc/terms/\"> \n" +
			"<rdf:Description rdf:about=\"urn:x-states:New%20York\"> \n" +
			"<dcterms:alternative>New York (UPDATED)</dcterms:alternative> \n" +
			"</rdf:Description> \n" +
			"</rdf:RDF>";
	
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
	
	private String getCollection() throws Exception {
		String contents = getRelContents();
		
		URL contentsURL = new java.net.URL(contents);
		HttpURLConnection contentsConnection = (HttpURLConnection) contentsURL.openConnection();
		contentsConnection.setRequestMethod("GET");
		contentsConnection.setRequestProperty("ACCEPT", "application/atom+xml");
		assertEquals(contentsConnection.getResponseMessage(), 200, contentsConnection.getResponseCode());
		InputStream stream = contentsConnection.getInputStream();
		String text = new java.util.Scanner(stream).useDelimiter("\\A").next();
		int app = text.indexOf("<app:collection");
		int start = text.indexOf("\"", app);
		int stop = text.indexOf("\"", start + 1);
		String result = text.substring(start + 1, stop);
		return result;
	}

	private String getRelContents() throws MalformedURLException, IOException,
			ProtocolException {
		URL url = new java.net.URL(temporaryServer.getOrigin() + "/");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("OPTIONS");
		assertEquals(connection.getResponseMessage(), 204, connection.getResponseCode());
		String header = connection.getHeaderField("LINK");
		int rel = header.indexOf("rel=\"contents\"");
		int end = header.lastIndexOf(">", rel);
		int start = header.lastIndexOf("<", rel);
		String contents = header.substring(start + 1, end);
		return contents;
	}
	
	private String getLocation() throws Exception {
		URL url = new java.net.URL(getCollection());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Slug", requestSlug);
		connection.setRequestProperty("Content-Type", requestContentType);
		connection.setDoOutput(true);
		OutputStream output = connection.getOutputStream();
		output.write(outputString.getBytes());
		output.close();
		assertEquals(connection.getResponseMessage(), 201, connection.getResponseCode());
		String header = connection.getHeaderField("Location");
		return header;
	}
	
	private String getEditMedia() throws Exception {
		URL url = new java.net.URL(getLocation());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("OPTIONS");
		assertEquals(connection.getResponseMessage(), 204, connection.getResponseCode());
		String header = connection.getHeaderField("LINK");
		int rel = header.indexOf("rel=\"edit-media\"");
		int end = header.lastIndexOf(">", rel);
		int start = header.lastIndexOf("<", rel);
		String contents = header.substring(start + 1, end);
		return contents;
	}
	
	public void testCreate() throws MalformedURLException, Exception {
		URL url = new java.net.URL(getCollection());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Slug", requestSlug);
		connection.setRequestProperty("Content-Type", requestContentType);
		connection.setDoOutput(true);
		OutputStream output = connection.getOutputStream();
		output.write(outputString.getBytes());
		output.close();
		assertEquals(connection.getResponseMessage(), 201, connection.getResponseCode());
	}
	
	public void testRetrieve() throws Exception {
		URL url = new java.net.URL(getEditMedia());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("ACCEPT", requestContentType);
		InputStream stream = connection.getInputStream();
		String text = new java.util.Scanner(stream).useDelimiter("\\A").next();
		assertTrue(connection.getResponseMessage(), text.contains("urn:x-states:New%20York"));
	}
	
	public void testUpdate() throws Exception {
		URL url = new java.net.URL(getEditMedia());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("CONTENT-TYPE", requestContentType);
		connection.setDoOutput(true);
		OutputStream output = connection.getOutputStream();
		output.write(updateOutputString.getBytes());
		output.close();
		assertEquals(connection.getResponseMessage(), 204, connection.getResponseCode());
	}

	public void testDelete() throws Exception {
		URL url = new java.net.URL(getEditMedia());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("DELETE");
		assertEquals(connection.getResponseMessage(), 204, connection.getResponseCode());
	}
}
