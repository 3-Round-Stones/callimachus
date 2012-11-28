package org.callimachusproject.restapi;

import java.net.URLEncoder;

import org.callimachusproject.test.TemporaryServerTestCase;
import org.junit.Ignore;
import org.junit.Test;

public class SparqlEndpointTest extends TemporaryServerTestCase {

	private static final String QUERY = "application/sparql-query";
	private static final String UPDATE = "application/sparql-update";
	private static final String URLENCODED = "application/x-www-form-urlencoded";
	private static final String RESULTS_XML = "application/sparql-results+xml";

	@Test
	public void testGetQueryParameter() throws Exception {
		String sparql = "ASK { ?s ?p ?o }";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String results = new String(getHomeFolder().sparqlEndpoint().ref("?query=" + encoded).get(RESULTS_XML));
		assertTrue(results.contains("true"));
	}

	@Test
	public void testPostQueryForm() throws Exception {
		String sparql = "ASK { ?s ?p ?o }";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String form = "query=" + encoded;
		String results = new String(getHomeFolder().sparqlEndpoint().post(URLENCODED, form.getBytes(), RESULTS_XML));
		assertTrue(results.contains("true"));
	}

	@Ignore
	@Test
	public void ignore_testPostQueryDirectly() throws Exception {
		String sparql = "ASK { ?s ?p ?o }";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String results = new String(getHomeFolder().sparqlEndpoint().post(QUERY, encoded.getBytes(), RESULTS_XML));
		assertTrue(results.contains("true"));
	}

	@Test
	public void testPostUpdateForm() throws Exception {
		String sparql = "INSERT DATA { </> a </> }";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String form = "update=" + encoded;
		getHomeFolder().sparqlEndpoint().post(URLENCODED, form.getBytes());
	}

	@Ignore
	@Test
	public void ignore_testPostUpdateDirectly() throws Exception {
		String sparql = "INSERT DATA { </> a </> }";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		getHomeFolder().sparqlEndpoint().post(UPDATE, encoded.getBytes());
	}

}
