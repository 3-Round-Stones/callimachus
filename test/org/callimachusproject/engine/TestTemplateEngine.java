package org.callimachusproject.engine;

import java.io.Reader;
import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.http.client.HttpClient;
import org.callimachusproject.client.HttpClientFactory;

public class TestTemplateEngine extends TestCase {
	private static final String SYSTEM_ID = "http://example.com/";
	private final HttpClient client = HttpClientFactory.getInstance()
			.createHttpClient(SYSTEM_ID);

	public void setUp() throws Exception {
		super.setUp();
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}

	public void testInferTrue() throws Exception {
		Reader in = new StringReader(
				"<ul xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'>\n"
						+ "<li resource='?url'><span property='rdfs:label'/></li></ul>");
		TemplateEngine eng = TemplateEngine.newInstance(client);
		Template tem = eng.getTemplate(in, SYSTEM_ID);
		String qry = tem.getQueryString("# @infer true\n"
				+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "SELECT ?url {[a ?url]}");
		assertTrue(qry, qry.contains("# @infer true\n"));
		ParameterizedQueryParser parser = ParameterizedQueryParser
				.newInstance();
		ParameterizedQuery query = parser.parseQuery(qry, SYSTEM_ID);
		assertTrue(query.getIncludeInferred());
	}

	public void testInferFalse() throws Exception {
		Reader in = new StringReader(
				"<ul xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'>\n"
						+ "<li resource='?url'><span property='rdfs:label'/></li></ul>");
		TemplateEngine eng = TemplateEngine.newInstance(client);
		Template tem = eng.getTemplate(in, SYSTEM_ID);
		String qry = tem.getQueryString("# @infer false\n"
				+ "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "SELECT ?url {[a ?url]}");
		assertTrue(qry, qry.contains("# @infer false\n"));
		ParameterizedQueryParser parser = ParameterizedQueryParser
				.newInstance();
		ParameterizedQuery query = parser.parseQuery(qry, SYSTEM_ID);
		assertFalse(query.getIncludeInferred());
	}

	public void testInferFalseBelow() throws Exception {
		Reader in = new StringReader(
				"<ul xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'>\n"
						+ "<li resource='?url'><span property='rdfs:label'/></li></ul>");
		TemplateEngine eng = TemplateEngine.newInstance(client);
		Template tem = eng.getTemplate(in, SYSTEM_ID);
		String qry = tem.getQueryString("PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "# @infer false\n"
				+ "SELECT ?url {[a ?url]}");
		assertTrue(qry, qry.contains("# @infer false\n"));
		ParameterizedQueryParser parser = ParameterizedQueryParser
				.newInstance();
		ParameterizedQuery query = parser.parseQuery(qry, SYSTEM_ID);
		assertFalse(query.getIncludeInferred());
	}

	public void testCanonicalReferenceQuery() throws Exception {
		Reader in = new StringReader(
				"<ul xmlns:rdfs='http://www.w3.org/2000/01/rdf-schema#'>\n"
						+ "<li resource='?url'>\n" +
						"<span rel='rdfs:seeAlso' resource='http://example.com'/>\n" +
						"<span property='rdfs:label'/></li></ul>");
		TemplateEngine eng = TemplateEngine.newInstance(client);
		Template tem = eng.getTemplate(in, SYSTEM_ID);
		String qry = tem.getQueryString();
		assertTrue(qry, qry.contains("<http://example.com/>"));
	}

}
