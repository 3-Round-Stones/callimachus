package org.callimachusproject.engine;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;

import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.memory.MemoryStore;

public class TestParameterizedQueries extends TestCase {
	private static final String EXAMPLE_COM = "http://example.com/";
	private static final String PREFIX = "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n";
	private SailRepository repo;
	private SailRepositoryConnection con;
	private ValueFactory vf;
	private ParameterizedQueryParser parser;

	public void setUp() throws Exception {
		repo = new SailRepository(new MemoryStore());
		repo.initialize();
		con = repo.getConnection();
		vf = con.getValueFactory();
		parser = ParameterizedQueryParser.newInstance();
	}

	public void tearDown() throws Exception {
		con.close();
		repo.shutDown();
	}

	public void testNoParameters() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label ?label }";
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(null);

		con.add(vf.createURI("urn:test:thing"), RDFS.LABEL, vf.createLiteral("Thing"));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("urn:test:thing", result.next().getValue("thing").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testMissingParameters() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label \"?label\" }";
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(null);

		con.add(vf.createURI("urn:test:thing"), RDFS.LABEL, vf.createLiteral("Thing"));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testLiteralParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label \"?label\" }";
		Map<String, String[]> parameters = Collections.singletonMap("label", new String[]{"Thing"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing"), RDFS.LABEL, vf.createLiteral("Thing"));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("urn:test:thing", result.next().getValue("thing").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testUriParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { <?thing> rdfs:label ?label }";
		Map<String, String[]> parameters = Collections.singletonMap("thing", new String[]{"urn:test:thing"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing"), RDFS.LABEL, vf.createLiteral("Thing"));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("Thing", result.next().getValue("label").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testSelectParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT ?thing ?label { ?thing rdfs:label \"?label\" }";
		Map<String, String[]> parameters = Collections.singletonMap("label", new String[]{"Thing"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing"), RDFS.LABEL, vf.createLiteral("Thing"));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		BindingSet next = result.next();
		assertEquals("urn:test:thing", next.getValue("thing").stringValue());
		assertEquals("Thing", next.getValue("label").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

}
