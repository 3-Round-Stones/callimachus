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

public class BLOBResourceTest extends TestCase {
	
	private TemporaryServer temporaryServer = TemporaryServer.getInstance();

	public void setUp() throws Exception {
		super.setUp();
		temporaryServer.start();
	}

	public void tearDown() throws Exception {
		super.tearDown();
		temporaryServer.stop();
	}
	
	private String getCollection() throws Exception {
		String contents = getRelContents();
		
		URL contentsURL = new java.net.URL(contents);
		HttpURLConnection contentsConnection = (HttpURLConnection) contentsURL.openConnection();
		contentsConnection.setRequestMethod("GET");
		contentsConnection.setRequestProperty("ACCEPT", "application/atom+xml");
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
		String header = connection.getHeaderField("LINK");
		int rel = header.indexOf("rel=\"contents\"");
		int end = header.lastIndexOf(">", rel);
		int start = header.lastIndexOf("<", rel);
		String contents = header.substring(start + 1, end);
		return contents;
	}
	
	private String getLocation() throws Exception {
		Authenticator.setDefault(new Authenticator() {
		     protected PasswordAuthentication getPasswordAuthentication() {
		       return new PasswordAuthentication("admin", "admin".toCharArray()); 
		     }
		 });
		
		URL url = new java.net.URL(getCollection());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Slug", "myLogo.html");
		connection.setRequestProperty("Content-Type", "text/html");
		connection.setDoOutput(true);
		OutputStream output = connection.getOutputStream();
		output.write("<html><head><title>Output Page</title></head><body></body></html>".getBytes());
		output.close();
		String header = connection.getHeaderField("Location");
		return header;
	}
	
	private String getEditMedia() throws Exception {
		URL url = new java.net.URL(getLocation());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("OPTIONS");
		String header = connection.getHeaderField("LINK");
		int rel = header.indexOf("rel=\"edit-media\"");
		int end = header.lastIndexOf(">", rel);
		int start = header.lastIndexOf("<", rel);
		String contents = header.substring(start + 1, end);
		return contents;
	}
	
	public void testCreate() throws MalformedURLException, Exception {
		Authenticator.setDefault(new Authenticator() {
		     protected PasswordAuthentication getPasswordAuthentication() {
		       return new PasswordAuthentication("admin", "admin".toCharArray()); 
		     }
		 });
		
		URL url = new java.net.URL(getCollection());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Slug", "myLogo.html");
		connection.setRequestProperty("Content-Type", "text/html");
		connection.setDoOutput(true);
		OutputStream output = connection.getOutputStream();
		output.write("<html><head><title>Output Page</title></head><body></body></html>".getBytes());
		output.close();
		int code = connection.getResponseCode();
		assertEquals(201, code);
	}
	
	public void testRetrieve() throws Exception {
		
		URL url = new java.net.URL(getLocation());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("ACCEPT", "text/html");
		InputStream stream = connection.getInputStream();
		String text = new java.util.Scanner(stream).useDelimiter("\\A").next();
		assertEquals("<html><head><title>Output Page</title></head><body></body></html>", text);
	}
	
	public void testUpdate() throws Exception {
		Authenticator.setDefault(new Authenticator() {
		     protected PasswordAuthentication getPasswordAuthentication() {
		       return new PasswordAuthentication("admin", "admin".toCharArray()); 
		     }
		 });
		
		URL url = new java.net.URL(getLocation());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("PUT");
		connection.setRequestProperty("CONTENT-TYPE", "text/html");
		connection.setDoOutput(true);
		OutputStream output = connection.getOutputStream();
		output.write("<html><head><title>Updated Page</title></head><body></body></html>".getBytes());
		output.close();
		int code = connection.getResponseCode();
		assertEquals(204, code);
	}
	
	public void testDelete() throws Exception {
		Authenticator.setDefault(new Authenticator() {
		     protected PasswordAuthentication getPasswordAuthentication() {
		       return new PasswordAuthentication("admin", "admin".toCharArray()); 
		     }
		 });
		
		URL url = new java.net.URL(getLocation());
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("DELETE");
		int code = connection.getResponseCode();
		assertEquals(204, code);
	}
}
