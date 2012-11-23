package org.callimachusproject.types;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.callimachusproject.test.TemporaryServerTestCase;
import org.callimachusproject.test.WebResource;
import org.junit.ComparisonFailure;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.object.ObjectConnection;

public class FormInterface extends TemporaryServerTestCase {
	private static final String SKOS = "http://www.w3.org/2004/02/skos/core#";
	private final String requestSlug = "form-page.xhtml";
	private final String requestContentType = "application/xhtml+xml";
	private final String xhtml = "<?xml-stylesheet type=\"text/xsl\" href=\"/callimachus/template.xsl\"?>\n" +
			"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\"> \n" +
			"<head> <title> A Title </title> </head> \n" +
			"<body> <form id='form'>\n<select/>\n</form> </body> </html>";
	private ObjectConnection con;


	public void setUp() throws Exception {
		super.setUp();
		con = getRepository().getConnection();
	}

	public void tearDown() throws Exception {
		con.clear(con.getVersionBundle());
		con.close();
		super.tearDown();
	}

	@Test
	public void testOptions1() throws Exception {
		ValueFactory vf = con.getValueFactory();
		con.add(vf.createURI("urn:test:concept"), RDF.TYPE, vf.createURI(SKOS, "Concept"));
		con.add(vf.createURI("urn:test:concept"), vf.createURI(SKOS, "prefLabel"), vf.createLiteral("label-option-1"));
		String xml = "<select id='status' rel='skos:topConcept' xmlns:skos='http://www.w3.org/2004/02/skos/core#'><option about='?status' typeof='skos:Concept' property='skos:prefLabel' selected='selected' /></select>";
		String xhtml = this.xhtml.replace("<select/>", xml);
		WebResource uri = createPage(xhtml, requestSlug);
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
		WebResource uri = createPage(xhtml, requestSlug);
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
		WebResource uri = createPage(xhtml, requestSlug);
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
		WebResource uri = createPage(xhtml, requestSlug);
		String options = getOptions(uri);
		assertContains("<select", options);
		assertOccurance(2, "<option", options);
		assertContains("label-option-1", options);
		assertContains("label-option-3", options);
	}

	private WebResource createPage(String xhtml, String identifier) throws Exception {
		return getCollection().create(identifier, requestContentType, xhtml.getBytes());
	}
	
	private WebResource getCollection() throws Exception {
		return getRelContents().getAppCollection();
	}

	private WebResource getRelContents() throws IOException {
		return getHomeFolder().link("contents", "application/atom+xml");
	}

	private String getOptions(WebResource uri) throws Exception {
		WebResource page = uri.link("alternate", "text/html");
		String view = new String(page.get("text/html"));
		assertContains("<select", view);
		assertContains("data-options=", view);
		String url = getQuoteAfter("data-options=", view);
		assertContains(uri.toString(), url);
		return new String(page.ref(url).get("text/html"));
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
