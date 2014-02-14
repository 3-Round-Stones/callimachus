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
package org.callimachusproject.form;

import org.callimachusproject.form.helpers.TripleAnalyzer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.rio.RDFHandlerException;

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
	public void testOptionalVariables() throws Exception {
		String input = PREFIX + "DELETE { <#you> foaf:knows ?me . ?me foaf:name 'me' } WHERE { ?me foaf:name 'me' OPTIONAL { <#you> foaf:knows ?me } };";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeUpdate(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertTrue(analyzer.isAbout(vf.createURI("http://example.com/")));
		Assert.assertFalse(analyzer.isDisconnectedNodePresent());
		Assert.assertTrue(analyzer.isSingleton());
		Assert.assertTrue(analyzer.isComplicated());
	}

	@Test
	public void testVariables() throws Exception {
		String input = PREFIX + "DELETE { <#you> foaf:knows ?me . ?me foaf:name 'me' } WHERE { <#you> foaf:knows ?me . ?me foaf:name 'me' };";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeUpdate(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertTrue(analyzer.isAbout(vf.createURI("http://example.com/")));
		Assert.assertFalse(analyzer.isDisconnectedNodePresent());
		Assert.assertTrue(analyzer.isSingleton());
		Assert.assertFalse(analyzer.isComplicated());
	}

	@Test
	public void testDeleteWhereInsertWhere() throws Exception {
		String input = PREFIX + "DELETE { <#you> a foaf:Person } WHERE { <#you> a foaf:Person }; INSERT { <#me> a foaf:Person } WHERE {}";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeUpdate(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertTrue(analyzer.isAbout(vf.createURI("http://example.com/")));
		Assert.assertFalse(analyzer.isDisconnectedNodePresent());
		Assert.assertTrue(analyzer.isSingleton());
		Assert.assertFalse(analyzer.isComplicated());
	}

	@Test
	public void testInsertWhereVariable() throws Exception {
		String input = PREFIX + "INSERT { <#you> foaf:knows ?me } WHERE { ?me foaf:name 'me' }";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeUpdate(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertFalse(analyzer.isAbout(vf.createURI("http://example.com/")));
		Assert.assertFalse(analyzer.isSingleton());
		Assert.assertTrue(analyzer.isDisconnectedNodePresent());
		Assert.assertFalse(analyzer.isComplicated());
	}

	@Test
	public void testDisconnectedInsertWhere() throws Exception {
		String input = PREFIX + "INSERT { <#you> foaf:knows ?me } WHERE { <#you> a foaf:Person . ?me foaf:name 'me' }";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeUpdate(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertTrue(analyzer.isAbout(vf.createURI("http://example.com/")));
		Assert.assertTrue(analyzer.isSingleton());
		Assert.assertTrue(analyzer.isDisconnectedNodePresent());
		Assert.assertFalse(analyzer.isComplicated());
	}

	@Test
	public void testDeleteStar() throws Exception {
		String input = PREFIX + "DELETE { ?s ?p ?o } WHERE { ?s ?p ?o}";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		try {
			analyzer.analyzeUpdate(input, "http://example.com/");
			Assert.fail();
		} catch (RDFHandlerException e) {
			
		}
	}

	@Test
	public void testDeleteInsertWhere() throws Exception {
		String input = PREFIX + "DELETE { <#you> a foaf:Person } INSERT { <#me> a foaf:Person } WHERE {}";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeUpdate(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertTrue(analyzer.isAbout(vf.createURI("http://example.com/")));
		Assert.assertFalse(analyzer.isDisconnectedNodePresent());
		Assert.assertTrue(analyzer.isSingleton());
		Assert.assertFalse(analyzer.isComplicated());
	}

	@Test
	public void testSingleInsertDATA() throws Exception {
		String input = PREFIX + "INSERT DATA { <#me> a foaf:Person }";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeUpdate(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertTrue(analyzer.isAbout(vf.createURI("http://example.com/")));
		Assert.assertFalse(analyzer.isDisconnectedNodePresent());
		Assert.assertTrue(analyzer.isSingleton());
		Assert.assertFalse(analyzer.isComplicated());
	}

	@Test
	public void testInsertDATA() throws Exception {
		String input = PREFIX + "INSERT DATA { <#me> a foaf:Person; foaf:name \"me\" }";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeUpdate(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertTrue(analyzer.isAbout(vf.createURI("http://example.com/")));
		Assert.assertFalse(analyzer.isDisconnectedNodePresent());
		Assert.assertTrue(analyzer.isSingleton());
		Assert.assertFalse(analyzer.isComplicated());
	}

	@Test
	public void testBlankInsertDATA() throws Exception {
		String input = PREFIX + "INSERT DATA { <#me> a foaf:Person; foaf:knows [foaf:name \"you\"]}";
		TripleAnalyzer analyzer = new TripleAnalyzer();
		analyzer.analyzeUpdate(input, "http://example.com/");
		Assert.assertFalse(analyzer.isEmpty());
		Assert.assertTrue(analyzer.isAbout(vf.createURI("http://example.com/")));
		Assert.assertFalse(analyzer.isDisconnectedNodePresent());
		Assert.assertTrue(analyzer.isSingleton());
		Assert.assertFalse(analyzer.isComplicated());
	}

}
