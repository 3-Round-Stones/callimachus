package org.callimachusproject.form;

import org.callimachusproject.form.helpers.TripleAnalyzer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

public class TestTripleAnalyzer {
	private static final String PREFIX = "PREFIX foaf:<http://xmlns.com/foaf/0.1/>\n";
	private final ValueFactory vf = ValueFactoryImpl.getInstance();

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDeleteInsertWhere() throws Exception {
		String input = PREFIX + "DELETE { <#you> a foaf:Person } INSERT { <#me> a foaf:Person } WHERE {}";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeUpdate(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertTrue(analyzer.isAbout(vf.createURI("http://example.com/")));
	}

	@Test
	public void testSingleInsertDATA() throws Exception {
		String input = PREFIX + "INSERT DATA { <#me> a foaf:Person }";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeInsertData(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertTrue(analyzer.isAbout(vf.createURI("http://example.com/")));
	}

	@Test
	public void testInsertDATA() throws Exception {
		String input = PREFIX + "INSERT DATA { <#me> a foaf:Person; foaf:name \"me\" }";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeInsertData(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertTrue(analyzer.isAbout(vf.createURI("http://example.com/")));
	}

	@Test
	public void testBlankInsertDATA() throws Exception {
		String input = PREFIX + "INSERT DATA { <#me> a foaf:Person; foaf:knows [foaf:name \"you\"]}";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeInsertData(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertTrue(analyzer.isAbout(vf.createURI("http://example.com/")));
	}

}
