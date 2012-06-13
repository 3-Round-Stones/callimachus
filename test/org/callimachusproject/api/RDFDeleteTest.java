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

public class RDFDeleteTest extends TestCase {
	
	private TemporaryServer temporaryServer;
	private String connectionContentType = "application/sparql-update";
	private String requestContentType = "text/turtle";
	private String createQuery = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> \n" +
			" prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
			" prefix calli: <http://callimachusproject.org/rdf/2009/framework#> \n" +
			" INSERT DATA {  \n <test/> a </callimachus/Folder> ;  \n" +
			" rdfs:label \"test\" . }";
	private String updateQuery = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
			" DELETE { \n </test/> rdfs:label ?label . \n } \n" +
			" INSERT { \n </test/> rdfs:label \"Test Folder\" . \n } \n" +
			" WHERE { \n </test/> rdfs:label ?label . \n }";
	private String compareText = "/test/";

	public void setUp() throws Exception {
		super.setUp();
		temporaryServer = TemporaryServer.start();
		Authenticator.setDefault(new Authenticator() {
		     protected PasswordAuthentication getPasswordAuthentication() {
		       return new PasswordAuthentication(temporaryServer.getUsername(), temporaryServer.getPassword()); 
		     }
		 });
	}

	public void tearDown() throws Exception {
		super.tearDown();
		temporaryServer.stop();
	}
	
	private String getRDFContents() throws MalformedURLException, IOException,
			ProtocolException {
		URL url = new java.net.URL(temporaryServer.getOrigin() + "/");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("OPTIONS");
		String header = connection.getHeaderField("LINK");
		int rel = header.indexOf("rel=\"describedby\"");
		int start = header.lastIndexOf("<", rel);
		int end = header.lastIndexOf(">", rel);
		String contents = header.substring(start + 1, end);
		return contents;
	}
	
	private String getLocation() throws Exception {
		URL url = new java.net.URL(getRDFContents());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Content-Type", connectionContentType);
		connection.setDoOutput(true);
		OutputStream output = connection.getOutputStream();
		output.write(createQuery.getBytes());
		output.close();
		String header = connection.getHeaderField("Location");
		return header;
	}
	
	private String getDescribedBy() throws Exception {
		URL url = new java.net.URL(getLocation());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("OPTIONS");
		String header = connection.getHeaderField("LINK");
		int rel = header.indexOf("rel=\"describedby\"");
		int end = header.lastIndexOf(">", rel);
		int start = header.lastIndexOf("<", rel);
		String contents = header.substring(start + 1, end);
		return contents;
	}
	
	public void testDelete() throws Exception {
		URL url = new java.net.URL(getDescribedBy());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("DELETE");
		int code = connection.getResponseCode();
		assertEquals(204, code);
	}
}