package org.callimachusproject.restapi;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.callimachusproject.test.WebResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.rdfxml.RDFXMLParser;
import org.openrdf.rio.turtle.TurtleParser;

public class DatasourceIntegrationTest extends TemporaryServerIntegrationTestCase {

	private static final String PREFIX = "PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>\n";
	private static final String QUERY = "application/sparql-query";
	private static final String UPDATE = "application/sparql-update";
	private static final String URLENCODED = "application/x-www-form-urlencoded";
	private static final String RESULTS_XML = "application/sparql-results+xml";
	private WebResource datasource;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		datasource = getHomeFolder().createRdfDatasource("data");
	}

	@Override
	@After
	public void tearDown() throws Exception {
		datasource.link("describedby").delete();
		super.tearDown();
	}

	@Test
	public void testGetQueryParameter() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { </> a </> }".getBytes());
		String sparql = "ASK { ?s ?p ?o }";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String results = new String(datasource.ref("?query=" + encoded).get(RESULTS_XML));
		assertTrue(results.contains("true"));
	}

	@Test
	public void testPostQueryForm() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { </> a </> }".getBytes());
		String sparql = "ASK { ?s ?p ?o }";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String form = "query=" + encoded;
		String results = new String(datasource.post(URLENCODED, form.getBytes(), RESULTS_XML));
		assertTrue(results.contains("true"));
	}

	@Test
	public void testPostQueryDirectly() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { </> a </> }".getBytes());
		String sparql = "ASK { ?s ?p ?o }";
		String results = new String(datasource.post(QUERY, sparql.getBytes(), RESULTS_XML));
		assertTrue(results.contains("true"));
	}

	@Test
	public void testPostUpdateForm() throws Exception {
		String sparql = "INSERT DATA { </> a </> }";
		String encoded = URLEncoder.encode(sparql, "UTF-8");
		String form = "update=" + encoded;
		datasource.post(URLENCODED, form.getBytes());
	}

	@Test
	public void testPostUpdateDirectly() throws Exception {
		String sparql = "INSERT DATA { </> a </> }";
		datasource.post(UPDATE, sparql.getBytes());
	}

	@Test
	public void testPostConstructDirectly() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { </> a </> }".getBytes());
		String rq = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";
		byte[] graph = datasource.post(QUERY, rq.getBytes(), "application/rdf+xml");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				assertEquals(st.getSubject(), st.getObject());
			}
		};
		RDFXMLParser parser = new RDFXMLParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		assertEquals(1, sc.getStatements().size());
	}

	@Test
	public void testGetDefaultGraphIndirectly() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { </> a </> }".getBytes());
		byte[] graph = datasource.ref("?default").get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				assertEquals(st.getSubject(), st.getObject());
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		assertEquals(1, sc.getStatements().size());
	}

	@Test
	public void testPutDefaultGraphIndirectly() throws Exception {
		datasource.ref("?default").put("text/turtle", (PREFIX +
				"<> a sd:Service.").getBytes());
		byte[] graph = datasource.ref("?default").get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				assertEquals(datasource.toString(), st.getSubject().stringValue());
				assertEquals("http://www.w3.org/ns/sparql-service-description#Service", st.getObject().stringValue());
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		assertEquals(1, sc.getStatements().size());
	}

	@Test
	public void testPatchDefaultGraphIndirectly() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { </> a </> }".getBytes());
		datasource.ref("?default").patch(UPDATE, (PREFIX +
				"INSERT DATA { <> a sd:Service }").getBytes());
		byte[] graph = datasource.ref("?default").get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				if (!st.getSubject().equals(st.getObject())) {
					assertEquals(datasource.toString(), st.getSubject().stringValue());
					assertEquals("http://www.w3.org/ns/sparql-service-description#Service", st.getObject().stringValue());
				}
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		assertEquals(2, sc.getStatements().size());
	}

	@Test
	public void testPostDefaultGraphIndirectly() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { </> a </> }".getBytes());
		datasource.ref("?default").post("text/turtle", (PREFIX +
				"<> a sd:Service.").getBytes());
		byte[] graph = datasource.ref("?default").get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				if (!st.getSubject().equals(st.getObject())) {
					assertEquals(datasource.toString(), st.getSubject().stringValue());
					assertEquals("http://www.w3.org/ns/sparql-service-description#Service", st.getObject().stringValue());
				}
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		assertEquals(2, sc.getStatements().size());
	}

	@Test
	public void testDeleteDefaultGraphIndirectly() throws Exception {
		datasource.ref("?default").delete();
		byte[] graph = datasource.ref("?default").get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				fail();
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		assertEquals(0, sc.getStatements().size());
	}

	@Test
	public void testGetNamedGraphIndirectly() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { GRAPH <urn:test:graph> { </> a </> } }".getBytes());
		byte[] graph = datasource.ref("?graph=urn:test:graph").get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				assertEquals(st.getSubject(), st.getObject());
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		assertEquals(1, sc.getStatements().size());
	}

	@Test
	public void testPutNamedGraphIndirectly() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { GRAPH <urn:test:graph> { </> a </> }}".getBytes());
		datasource.ref("?graph=urn:test:graph").put("text/turtle", (PREFIX +
				"<> a sd:Service.").getBytes());
		byte[] graph = datasource.ref("?graph=urn:test:graph").get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				assertEquals(datasource.toString(), st.getSubject().stringValue());
				assertEquals("http://www.w3.org/ns/sparql-service-description#Service", st.getObject().stringValue());
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		assertEquals(1, sc.getStatements().size());
	}

	@Test
	public void testPatchNamedGraphIndirectly() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { GRAPH <urn:test:graph> { </> a </> }}".getBytes());
		datasource.ref("?graph=urn:test:graph").patch(UPDATE, (PREFIX +
				"INSERT DATA { <> a sd:Service }").getBytes());
		byte[] graph = datasource.ref("?graph=urn:test:graph").get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				if (!st.getSubject().equals(st.getObject())) {
					assertEquals(datasource.toString(), st.getSubject().stringValue());
					assertEquals("http://www.w3.org/ns/sparql-service-description#Service", st.getObject().stringValue());
				}
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		assertEquals(2, sc.getStatements().size());
	}

	@Test
	public void testPostNamedGraphIndirectly() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { GRAPH <urn:test:graph> { </> a </> }}".getBytes());
		datasource.ref("?graph=urn:test:graph").post("text/turtle", (PREFIX +
				"<> a sd:Service.").getBytes());
		byte[] graph = datasource.ref("?graph=urn:test:graph").get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				if (!st.getSubject().equals(st.getObject())) {
					assertEquals(datasource.toString(), st.getSubject().stringValue());
					assertEquals("http://www.w3.org/ns/sparql-service-description#Service", st.getObject().stringValue());
				}
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		assertEquals(2, sc.getStatements().size());
	}

	@Test
	public void testDeleteNamedGraphIndirectly() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { GRAPH <urn:test:graph> { </> a </> }}".getBytes());
		datasource.ref("?graph=urn:test:graph").delete();
		byte[] graph = datasource.ref("?graph=urn:test:graph").get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				fail();
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		assertEquals(0, sc.getStatements().size());
	}

}
