/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.callimachusproject.restapi;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;

import org.callimachusproject.test.TemporaryServerIntegrationTestCase;
import org.callimachusproject.test.WebResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.rdfxml.RDFXMLParser;
import org.openrdf.rio.turtle.TurtleParser;

public class DatasourceIntegrationTest extends TemporaryServerIntegrationTestCase {

	private static final String sdService = "http://www.w3.org/ns/sparql-service-description#Service";
	private static final String PREFIX = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX sd: <http://www.w3.org/ns/sparql-service-description#>\n";
	private static final String QUERY = "application/sparql-query";
	private static final String UPDATE = "application/sparql-update";
	private static final String URLENCODED = "application/x-www-form-urlencoded";
	private static final String RESULTS_XML = "application/sparql-results+xml";
	protected WebResource datasource;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		datasource = getHomeFolder().createRdfDatasource("data/");
	}

	@Override
	@After
	public void tearDown() throws Exception {
		datasource.rel("describedby").delete();
		super.tearDown();
	}

	@Test
	public void testServiceDescription() throws Exception {
		ValueFactory vf = ValueFactoryImpl.getInstance();
		byte[] graph = datasource.get("text/turtle");
		StatementCollector sc = new StatementCollector();
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		URI s = vf.createURI(datasource.toString());
		Statement st = vf.createStatement(s, RDF.TYPE, vf.createURI(sdService));
		assertTrue(sc.getStatements().contains(st));
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
		final WebResource graph = datasource.ref("?default");
		graph.put("text/turtle", (PREFIX + "<> a sd:Service.").getBytes());
		byte[] turtle = graph.get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(graph.toString(), st.getSubject().stringValue());
				assertEquals(RDF.TYPE, st.getPredicate());
				assertEquals(sdService, st.getObject().stringValue());
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(turtle), datasource.toString());
		assertEquals(1, sc.getStatements().size());
	}

	@Test
	public void testPostDefaultGraphIndirectly() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { </> a </> }".getBytes());
		final WebResource graph = datasource.ref("?default");
		graph.post("text/turtle", (PREFIX +
				"<> a sd:Service.").getBytes());
		byte[] turtle = graph.get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				if (!st.getSubject().equals(st.getObject())) {
					assertEquals(graph.toString(), st.getSubject().stringValue());
					assertEquals(sdService, st.getObject().stringValue());
				}
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(turtle), datasource.toString());
		assertEquals(2, sc.getStatements().size());
	}

	@Test
	public void testDeleteDefaultGraphIndirectly() throws Exception {
		datasource.ref("?default").delete();
		assertEquals(404, datasource.ref("?default").headCode());
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
		final WebResource graph = datasource.ref("?graph=urn:test:graph");
		graph.put("text/turtle", (PREFIX +
				"<> a sd:Service.").getBytes());
		byte[] turtle = graph.get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				assertEquals(graph.toString(), st.getSubject().stringValue());
				assertEquals(sdService, st.getObject().stringValue());
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(turtle), datasource.toString());
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
					assertEquals(sdService, st.getObject().stringValue());
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
		final WebResource graph = datasource.ref("?graph=urn:test:graph");
		graph.post("text/turtle", (PREFIX +
				"<> a sd:Service.").getBytes());
		byte[] turtle = graph.get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				if (!st.getSubject().equals(st.getObject())) {
					assertEquals(graph.toString(), st.getSubject().stringValue());
					assertEquals(sdService, st.getObject().stringValue());
				}
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(turtle), datasource.toString());
		assertEquals(2, sc.getStatements().size());
	}

	@Test
	public void testDeleteNamedGraphIndirectly() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { GRAPH <urn:test:graph> { </> a </> }}".getBytes());
		datasource.ref("?graph=urn:test:graph").delete();
		assertEquals(404, datasource.ref("?graph=urn:test:graph").headCode());
	}

	@Test
	public void testGetDirectGraph() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { GRAPH <graph> { </> a </> } }".getBytes());
		byte[] graph = datasource.ref("graph").get("text/turtle");
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
	public void testPutDirectGraph() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { GRAPH <graph> { </> a </> }}".getBytes());
		final WebResource graph = datasource.ref("graph");
		graph.put("text/turtle", (PREFIX +
				"<> a sd:Service.").getBytes());
		byte[] turtle = graph.get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				assertEquals(graph.toString(), st.getSubject().stringValue());
				assertEquals(sdService, st.getObject().stringValue());
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(turtle), datasource.toString());
		assertEquals(1, sc.getStatements().size());
	}

	@Test
	public void testPatchDirectGraph() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { GRAPH <graph> { </> a </> }}".getBytes());
		datasource.ref("graph").patch(UPDATE, (PREFIX +
				"INSERT DATA { <> a sd:Service }").getBytes());
		byte[] graph = datasource.ref("graph").get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				if (!st.getSubject().equals(st.getObject())) {
					assertEquals(datasource.toString(), st.getSubject().stringValue());
					assertEquals(sdService, st.getObject().stringValue());
				}
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(graph), datasource.toString());
		assertEquals(2, sc.getStatements().size());
	}

	@Test
	public void testPostDirectGraph() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { GRAPH <graph> { </> a </> }}".getBytes());
		final WebResource graph = datasource.ref("graph");
		graph.post("text/turtle", (PREFIX +
				"<> a sd:Service.").getBytes());
		byte[] turtle = graph.get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				super.handleStatement(st);
				assertEquals(RDF.TYPE, st.getPredicate());
				if (!st.getSubject().equals(st.getObject())) {
					assertEquals(graph.toString(), st.getSubject().stringValue());
					assertEquals(sdService, st.getObject().stringValue());
				}
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(turtle), datasource.toString());
		assertEquals(2, sc.getStatements().size());
	}

	@Test
	public void testDeleteDirectGraph() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { GRAPH <graph> { </> a </> }}".getBytes());
		datasource.ref("graph").delete();
		assertEquals(404, datasource.ref("graph").getRedirectTarget().headCode());
	}

	@Test
	public void testGetDirectResource() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { <resource> a rdfs:Resource }".getBytes());
		final WebResource resource = datasource.ref("resource");
		byte[] turtle = resource.get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				if (st.getSubject().stringValue().equals(resource.toString())) {
					super.handleStatement(st);
					assertEquals(RDF.TYPE, st.getPredicate());
					assertEquals(RDFS.RESOURCE, st.getObject());
				}
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(turtle), datasource.toString());
		assertEquals(1, sc.getStatements().size());
	}

	@Test
	public void testGetIndirectResource() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { <resource> a rdfs:Resource }".getBytes());
		final WebResource resource = datasource.ref("resource");
		final WebResource target = datasource.ref("?resource=resource");
		byte[] turtle = target.get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				if (st.getSubject().stringValue().equals(resource.toString())) {
					super.handleStatement(st);
					assertEquals(RDF.TYPE, st.getPredicate());
					assertEquals(RDFS.RESOURCE, st.getObject());
				} else {
					assertEquals(target.toString(), st.getSubject().stringValue());
				}
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(turtle), target.toString());
		assertEquals(1, sc.getStatements().size());
	}

	@Test
	public void testDeleteIndirectResource() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { <resource> a rdfs:Resource }".getBytes());
		WebResource resource = datasource.ref("?resource=resource");
		resource.delete();
		assertEquals(404, resource.headCode());
	}

	@Test
	public void testPutNoMatchIndirectResource() throws Exception {
		datasource.post(UPDATE,
				"INSERT DATA { <resource> a rdfs:Resource }".getBytes());
		final WebResource resource = datasource.ref("resource")
				.getRedirectTarget();
		try {
			String turtle = "<resource> a rdfs:Resource; rdfs:label 'resource'.";
			resource.putIf(null, "text/turtle",
					(PREFIX + turtle).getBytes("UTF-8"));
		} catch (junit.framework.AssertionFailedError e) {
			// If-Match request header is required
		}
	}

	@Test
	public void testPutNotMatchIndirectResource() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { <resource> a rdfs:Resource }".getBytes());
		final WebResource resource = datasource.ref("resource").getRedirectTarget();
		String turtle = "<resource> a rdfs:Resource; rdfs:label 'resource'.";
		try {
			resource.putIf("invalid", "text/turtle",
					(PREFIX + turtle).getBytes("UTF-8"));
		} catch (junit.framework.AssertionFailedError e) {
			// If-Match does not match
		}
	}

	@Test
	public void testPutAnyIndirectResource() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { <resource> a rdfs:Resource }".getBytes());
		final WebResource resource = datasource.ref("resource");
		final WebResource target = resource.getRedirectTarget();
		String turtle = new String(target.get("text/turtle"), "UTF-8");
		target.putIf("*", "text/turtle",
				(PREFIX + turtle + "<resource> rdfs:label 'resource'.")
						.getBytes("UTF-8"));
		byte[] result = target.get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				if (st.getSubject().stringValue().equals(resource.toString())) {
					super.handleStatement(st);
				} else {
					assertEquals(target.toString(), st.getSubject().stringValue());
				}
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(result), target.toString());
		assertEquals(2, sc.getStatements().size());
	}

	@Test
	public void testPutMatchIndirectResource() throws Exception {
		datasource.post(UPDATE, "INSERT DATA { <resource> a rdfs:Resource }".getBytes());
		final WebResource resource = datasource.ref("resource");
		final WebResource target = resource.getRedirectTarget();
		target.putIf(target.headETag(), "text/turtle",
				(PREFIX + "<resource> a rdfs:Resource; rdfs:label 'resource'.")
						.getBytes("UTF-8"));
		byte[] turtle = target.get("text/turtle");
		StatementCollector sc = new StatementCollector() {
			public void handleStatement(Statement st) {
				if (st.getSubject().stringValue().equals(resource.toString())) {
					super.handleStatement(st);
				} else {
					assertEquals(target.toString(), st.getSubject().stringValue());
				}
			}
		};
		TurtleParser parser = new TurtleParser();
		parser.setRDFHandler(sc);
		parser.parse(new ByteArrayInputStream(turtle), target.toString());
		assertEquals(2, sc.getStatements().size());
	}

}
