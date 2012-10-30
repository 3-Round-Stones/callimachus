package org.callimachusproject.restapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import org.junit.ComparisonFailure;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.ObjectConnection;

public class FormInterface extends TestCase {
	private static final String SKOS = "http://www.w3.org/2004/02/skos/core#";
    private static final TemporaryServer temporaryServer = TemporaryServerFactory.getInstance().createServer();
	private final String requestSlug = "form-page.xhtml";
	private final String requestContentType = "application/xhtml+xml";
	private final String xhtml = "<?xml-stylesheet type=\"text/xsl\" href=\"/callimachus/template.xsl\"?>\n" +
			"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> \n" +
			"<head> <title> A Title </title> </head> \n" +
			"<body> <form id='form'>\n<select/>\n</form> </body> </html>";
	private ObjectConnection con;


	public void setUp() throws Exception {
		super.setUp();
		temporaryServer.resume();
		Authenticator.setDefault(new Authenticator() {
		     protected PasswordAuthentication getPasswordAuthentication() {
		       return new PasswordAuthentication(temporaryServer.getUsername(), temporaryServer.getPassword()); 
		     }
		 });
		con = temporaryServer.getRepository().getConnection();
	}

	public void tearDown() throws Exception {
		con.clear(con.getActivityURI());
		con.close();
		super.tearDown();
		temporaryServer.pause();
		Authenticator.setDefault(null);
	}

	@Test
	public void testOptions1() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createURI("urn:test:concept"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label-option-1"));
		String xml = "<select id='status' rel='skos:topConcept' xmlns:skos='http://www.w3.org/2004/02/skos/core#'><option about='?status' typeof='skos:Concept' property='skos:prefLabel' selected='selected' /></select>";
		String xhtml = this.xhtml.replace("<select/>", xml);
		String uri = createPage(xhtml, requestSlug);
		String options = getOptions(uri);
		assertContains("<select", options);
		assertOccurance(1, "<option", options);
		assertContains("label-option-1", options);
	}

	@Test
	public void testOptions2() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createURI("urn:test:concept1"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept1"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label-option-1"));
		con.add(vf.createURI("urn:test:concept2"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept2"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label-option-2"));
		String xml = "<select id='status' rel='skos:topConcept' xmlns:skos='http://www.w3.org/2004/02/skos/core#'><option about='?status' typeof='skos:Concept' property='skos:prefLabel' selected='selected' /></select>";
		String xhtml = this.xhtml.replace("<select/>", xml);
		String uri = createPage(xhtml, requestSlug);
		String options = getOptions(uri);
		assertContains("<select", options);
		assertOccurance(2, "<option", options);
		assertContains("label-option-1", options);
		assertContains("label-option-2", options);
	}

	@Test
	public void testOptions3() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createURI("urn:test:concept1"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept1"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label-option-1"));
		con.add(vf.createURI("urn:test:concept2"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept2"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label-option-2"));
		con.add(vf.createURI("urn:test:concept3"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		String xml = "<select id='status' rel='skos:topConcept' xmlns:skos='http://www.w3.org/2004/02/skos/core#'><option about='?status' typeof='skos:Concept' property='skos:prefLabel' selected='selected' /></select>";
		String xhtml = this.xhtml.replace("<select/>", xml);
		String uri = createPage(xhtml, requestSlug);
		String options = getOptions(uri);
		assertContains("<select", options);
		assertOccurance(2, "<option", options);
		assertContains("label-option-1", options);
		assertContains("label-option-2", options);
	}

	@Test
	@Ignore
	public void testOptions4() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createURI("urn:test:concept1"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept1"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label-option-1"));
		con.add(vf.createURI("urn:test:concept2"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept3"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept3"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label-option-3"));
		String xml = "<select id='status' rel='skos:topConcept' xmlns:skos='http://www.w3.org/2004/02/skos/core#'><option about='?status' typeof='skos:Concept' property='skos:prefLabel' selected='selected' /></select>";
		String xhtml = this.xhtml.replace("<select/>", xml);
		String uri = createPage(xhtml, requestSlug);
		String options = getOptions(uri);
		assertContains("<select", options);
		assertOccurance(2, "<option", options);
		assertContains("label-option-1", options);
		assertContains("label-option-3", options);
	}

	private String createPage(String xhtml, String identifier) throws Exception {
		URL url = new java.net.URL(getCollection());
		HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
		urlcon.setRequestMethod("POST");
		urlcon.setRequestProperty("Slug", identifier);
		urlcon.setRequestProperty("Content-Type", requestContentType);
		urlcon.setDoOutput(true);
		OutputStream output = urlcon.getOutputStream();
		output.write(xhtml.getBytes());
		output.close();
		assertEquals(urlcon.getResponseMessage(), 201, urlcon.getResponseCode());
		String header = urlcon.getHeaderField("Location");
		return header;
	}
	
	private String getCollection() throws Exception {
		String contents = getRelContents();
		
		URL contentsURL = new java.net.URL(contents);
		HttpURLConnection urlcon = (HttpURLConnection) contentsURL.openConnection();
		urlcon.setRequestMethod("GET");
		urlcon.setRequestProperty("ACCEPT", "application/atom+xml");
		assertEquals(urlcon.getResponseMessage(), 203, urlcon.getResponseCode());
		InputStream stream = urlcon.getInputStream();
		String text = new java.util.Scanner(stream).useDelimiter("\\A").next();
		return getQuoteAfter("<app:collection", text);
	}

	private String getRelContents() throws MalformedURLException, IOException,
			ProtocolException {
		URL url = new java.net.URL(temporaryServer.getOrigin() + "/");
		HttpURLConnection urlcon = (HttpURLConnection) url.openConnection();
		urlcon.setRequestMethod("OPTIONS");
		assertEquals(urlcon.getResponseMessage(), 204, urlcon.getResponseCode());
		String header = urlcon.getHeaderField("LINK");
		int rel = header.indexOf("rel=\"contents\"");
		int end = header.lastIndexOf(">", rel);
		int start = header.lastIndexOf("<", rel);
		String contents = header.substring(start + 1, end);
		return contents;
	}

	private String getOptions(String uri) throws Exception {
		String view = getHtml(uri + "?view");
		assertContains("<select", view);
		assertContains("data-options=", view);
		String url = getQuoteAfter("data-options=", view);
		assertContains(uri, url);
		return getHtml(url);
	}
	
	private String getHtml(String url) throws Exception {
		HttpURLConnection urlcon = (HttpURLConnection) new java.net.URL(url).openConnection();
		urlcon.setRequestMethod("GET");
		urlcon.setRequestProperty("ACCEPT", "text/html");
		assertEquals(urlcon.getResponseMessage(), 203, urlcon.getResponseCode());
		InputStream stream = urlcon.getInputStream();
		return new java.util.Scanner(stream).useDelimiter("\\A").next();
	}

	private String getQuoteAfter(String token, String text) {
		int pos = text.indexOf(token);
		int start = text.indexOf("\"", pos);
		int stop = text.indexOf("\"", start + 1);
		return text.substring(start + 1, stop).replace("&amp;", "&");
	}

	private void assertContains(String needle, String actual) {
		if (actual == null || !actual.contains(needle))
			throw new ComparisonFailure("", needle, actual);
	}

	private void assertOccurance(int count, String needle, String actual) {
		if (actual == null || count > 0 && !actual.contains(needle))
			throw new ComparisonFailure("", needle, actual);
		Matcher m = Pattern.compile(Pattern.quote(needle)).matcher(actual);
		int found = 0;
		while (m.find()) {
			found++;
		}
		if (count != found)
			throw new ComparisonFailure("Expect " + count +  " occurances, but found " + found, needle, actual);
	}
}
