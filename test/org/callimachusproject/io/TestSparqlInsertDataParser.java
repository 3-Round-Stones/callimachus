package org.callimachusproject.io;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;
import org.openrdf.rio.helpers.StatementCollector;

public class TestSparqlInsertDataParser extends TestCase {
	private static final Map<String, Integer> files = new HashMap<String, Integer>() {
		{
			put("sparql-insert/drop-all.ru", 0);
			put("sparql-insert/drop-insert-data-named.ru", 1);
			put("sparql-insert/drop-insert-data-default.ru", 1);
			put("sparql-insert/drop-named.ru", 0);
			put("sparql-insert/insert-data-named1.ru", 1);
			put("sparql-insert/insert-data-named2.ru", 1);
			put("sparql-insert/insert-data-spo1.ru", 1);
			put("sparql-insert/large-request-01.ru", 868);
			put("sparql-insert/syntax-update-23.ru", 3);
			put("sparql-insert/syntax-update-24.ru", 3);
			put("sparql-insert/syntax-update-25.ru", 6);
			put("sparql-insert/syntax-update-53.ru", 2);
			put("sparql-insert/syntax-update-54.ru", 2);
			put("sparql-insert/syntax-update-bad-04.ru", -1);
		}
	};

	public static Test suite() {
		TestSuite suite = new TestSuite();
		for (String file : files.keySet()) {
			suite.addTest(new TestSparqlInsertDataParser(file));
		}
		return suite;
	}

	public TestSparqlInsertDataParser(String name) {
		super(name);
	}

	public void setUp() throws Exception {
		super.setUp();
	}

	public void tearDown() throws Exception {
		super.tearDown();
	}

	@Override
	protected void runTest() throws Throwable {
		int expected = files.get(getName());
		ClassLoader cl = this.getClass().getClassLoader();
		InputStream in = cl.getResource(getName()).openStream();
		try {
			SparqlInsertDataParser parser = new SparqlInsertDataParser() {
				@Override
				protected void reportDropGraph(URI graph)
						throws RDFHandlerException {
					assertEquals("sparql-insert/drop-insert-data-named.ru", getName());
				}

				@Override
				protected void reportDropDefault() throws RDFHandlerException {
					assertEquals("sparql-insert/drop-insert-data-default.ru", getName());
				}

				@Override
				protected void reportDropNamed() throws RDFHandlerException {
					assertEquals("sparql-insert/drop-named.ru", getName());
				}

				@Override
				protected void reportDropAll() throws RDFHandlerException {
					assertEquals("sparql-insert/drop-all.ru", getName());
				}
			};
			StatementCollector sc = new StatementCollector();
			parser.setRDFHandler(sc);
			parser.parse(in, "http://example.org/");
			assertEquals(expected, sc.getStatements().size());
		} catch (RDFParseException e) {
			if (expected >= 0)
				throw e;
		} finally {
			in.close();
		}
	}

}
