package org.callimachusproject.engine;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import junit.framework.TestCase;

import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.sail.SailRepositoryConnection;
import org.openrdf.sail.memory.MemoryStore;

public class TestParameterizedQueries extends TestCase {
	private static final String EXAMPLE_COM = "http://example.com/";
	private static final String PREFIX = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
			+ "PREFIX rdfs:<http://www.w3.org/2000/01/rdf-schema#>\n"
			+ "PREFIX test:<urn:test:>\n";
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
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label ?label } ORDER BY ?thing";
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(null);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("urn:test:thing1", result.next().getValue("thing").stringValue());
		assertTrue(result.hasNext());
		assertEquals("urn:test:thing2", result.next().getValue("thing").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testMissingParameters() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label \"$label\" }";
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(null);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testLiteralParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label \"$label\" }";
		Map<String, String[]> parameters = Collections.singletonMap("label", new String[]{"Thing1"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("urn:test:thing1", result.next().getValue("thing").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testUriParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { <$thing> rdfs:label ?label }";
		Map<String, String[]> parameters = Collections.singletonMap("thing", new String[]{"urn:test:thing1"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("Thing1", result.next().getValue("label").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testSelectParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT ?thing $label { ?thing rdfs:label \"$label\" }";
		Map<String, String[]> parameters = Collections.singletonMap("label", new String[]{"Thing1"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		BindingSet next = result.next();
		assertEquals("urn:test:thing1", next.getValue("thing").stringValue());
		assertEquals("Thing1", next.getValue("label").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testMissingOpenParameters() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label $label } ORDER BY ?thing";
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(null);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		BindingSet next = result.next();
		assertEquals("urn:test:thing1", next.getValue("thing").stringValue());
		assertEquals("Thing1", next.getValue("label").stringValue());
		assertTrue(result.hasNext());
		next = result.next();
		assertEquals("urn:test:thing2", next.getValue("thing").stringValue());
		assertEquals("Thing2", next.getValue("label").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testInvalidOpenParameters() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label $label } ORDER BY ?thing";
		Map<String, String[]> parameters = Collections.singletonMap("label", new String[]{"Thing1"});
		try {
			parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);
			fail();
		} catch (IllegalArgumentException e) {
			// success
		}
	}

	public void testLiteralOpenParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label $label }";
		Map<String, String[]> parameters = Collections.singletonMap("label", new String[]{"\"Thing1\""});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral("1", XMLSchema.INTEGER));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral("2", XMLSchema.INTEGER));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("urn:test:thing1", result.next().getValue("thing").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testIntegerOpenParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdf:value $value }";
		Map<String, String[]> parameters = Collections.singletonMap("value", new String[]{"1"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral("1", XMLSchema.INTEGER));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral("2", XMLSchema.INTEGER));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "value"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("urn:test:thing1", result.next().getValue("thing").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testLongLiteralOpenParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label $label }";
		Map<String, String[]> parameters = Collections.singletonMap("label", new String[]{"\"\"\"Thing1\"\"\""});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("urn:test:thing1", result.next().getValue("thing").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testLangStringOpenParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label $label }";
		Map<String, String[]> parameters = Collections.singletonMap("label", new String[]{"\"Thing1\"@en"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1", "en"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("urn:test:thing1", result.next().getValue("thing").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testUriOpenParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { $thing rdfs:label ?label }";
		Map<String, String[]> parameters = Collections.singletonMap("thing", new String[]{"<urn:test:thing1>"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("Thing1", result.next().getValue("label").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testCurieOpenParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { $thing rdfs:label ?label }";
		Map<String, String[]> parameters = Collections.singletonMap("thing", new String[]{"test:thing1"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("Thing1", result.next().getValue("label").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testMissingExpressionParameters() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label ${$label} }";
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(null);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing"), result.getBindingNames());
		assertFalse(result.hasNext());
		result.close();
	
	}

	public void testLiteralExpressionParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label ${$label} }";
		Map<String, String[]> parameters = Collections.singletonMap("label", new String[]{"\"Thing1\""});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("urn:test:thing1", result.next().getValue("thing").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testUriExpressionParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ${<$thing>} rdfs:label ?label }";
		Map<String, String[]> parameters = Collections.singletonMap("thing", new String[]{"urn:test:thing1"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("label"), result.getBindingNames());
		assertTrue(result.hasNext());
		assertEquals("Thing1", result.next().getValue("label").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testSelectExpressionParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT ?thing (${\"$label\"} AS ?string) { ?thing rdfs:label ${\"$label\"} }";
		Map<String, String[]> parameters = Collections.singletonMap("label", new String[]{"Thing1"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "string"), result.getBindingNames());
		assertTrue(result.hasNext());
		BindingSet next = result.next();
		assertEquals("urn:test:thing1", next.getValue("thing").stringValue());
		assertEquals("Thing1", next.getValue("string").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

	public void testOffsetExpressionParameter() throws Exception {
		String labelQuery = PREFIX + "SELECT * { ?thing rdfs:label ?label } ORDER BY ?thing OFFSET ${$position - 1} LIMIT 1";
		Map<String, String[]> parameters = Collections.singletonMap("position", new String[]{"2"});
		String sparql = parser.parseQuery(labelQuery, EXAMPLE_COM).prepare(parameters);

		con.add(vf.createURI("urn:test:thing1"), RDFS.LABEL, vf.createLiteral("Thing1"));
		con.add(vf.createURI("urn:test:thing2"), RDFS.LABEL, vf.createLiteral("Thing2"));
		con.add(vf.createURI("urn:test:thing1"), RDF.VALUE, vf.createLiteral(1));
		con.add(vf.createURI("urn:test:thing2"), RDF.VALUE, vf.createLiteral(2));
		TupleQueryResult result = con.prepareTupleQuery(QueryLanguage.SPARQL, sparql, EXAMPLE_COM).evaluate();
		assertEquals(Arrays.asList("thing", "label"), result.getBindingNames());
		assertTrue(result.hasNext());
		BindingSet next = result.next();
		assertEquals("urn:test:thing2", next.getValue("thing").stringValue());
		assertEquals("Thing2", next.getValue("label").stringValue());
		assertFalse(result.hasNext());
		result.close();
	}

}
