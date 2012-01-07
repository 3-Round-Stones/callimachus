/*
 * Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved
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

package org.callimachusproject.rdfa.test;

import static org.callimachusproject.rdfa.test.TestUtility.asDocument;
import static org.callimachusproject.rdfa.test.TestUtility.exportGraph;
import static org.callimachusproject.rdfa.test.TestUtility.loadRepository;
import static org.callimachusproject.rdfa.test.TestUtility.parseRDFa;
import static org.callimachusproject.rdfa.test.TestUtility.readDocument;
import static org.callimachusproject.rdfa.test.TestUtility.write;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.callimachusproject.engine.Template;
import org.callimachusproject.engine.TemplateEngine;
import org.callimachusproject.engine.TemplateEngineFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Dynamic Test Suite for the generation of XHTML+RDFa pages from RDFa templates.
 * 
 * @author Steve Battle
 * 
 */

// This is a parameterized test that runs over the test directory (test_dir)
@RunWith(Parameterized.class)
public class RDFaGenerationTest {
	// location of the XSLT transform relative to the test directory
	private static final String TEST_FILE_SUFFIX = "-test";
	private static final String DATA_ATTRIBUTE_TEST_BASE = "http://example.org/test";
			
	// private static properties defined in @BeforeClass setUp()
	private static XMLInputFactory xmlInputFactory;
	private static XPathFactory xPathFactory;
	private static Repository repository;
	
	// private static flags set in main()
	private static boolean verbose = false;
	private static boolean show_rdf = false;
	private static boolean show_sparql = false;
	private static boolean show_xml = false;
	private static boolean show_results = false;

	// this is the default test directory
	private static String test_dir = "RDFaGeneration/test-suite/";
	static {
		// override default test-dir with VM arg
		if (System.getProperty("dir")!=null)
			test_dir = System.getProperty("dir");
	}
	
	// the default test set to run on the default test_dir (other tests are assumed disabled)
	//private static String test_set = "legacy construct fragment select data";
	private static String test_set = "select data";
	static {
		// override default test-dir with VM arg
		if (System.getProperty("test")!=null)
			test_set = System.getProperty("test");
	}
	
	// object properties defined in @Before initialize() 
	RepositoryConnection con;
	String base;
	
	// properties defined by constructor
	// XHTML RDFa template used to derive SPARQL query 
	File template;
	// target XHTML with embedded RDFa (also used as RDF data source)
	File target;

	public RDFaGenerationTest(File template, File target) {
		this.template = template;
		this.target = target;
	}
	
	XPathExpression conjoinXPaths(Document fragment, String base) throws Exception {
		String path = "//*";
		final Element e = fragment.getDocumentElement();
		if (e==null) return null;
		final XPathIterator conjunction = new XPathIterator(e, base);
		if (conjunction.hasNext()) {
			path += "[";
			boolean first = true;
			while (conjunction.hasNext()) {
				if (!first) path += " and ";
				XPathExpression x = conjunction.next();
				String exp = x.toString();
				boolean positive = true;
				if (exp.startsWith("-")) {
					positive = false;
					exp = exp.substring(1);
				}
				// remove the initial '/'
				exp = exp.substring(1);
				if (positive) path += exp;
				else path += "not("+exp+")";
				first = false;
			}
			path += "]";
		}
		XPath xpath = xPathFactory.newXPath();
		// add namespace prefix resolver to the xpath based on the current element
		xpath.setNamespaceContext(new AbstractNamespaceContext(){
			public String getNamespaceURI(String prefix) {
				// for the empty prefix lookup the default namespace
				if (prefix.isEmpty()) return e.lookupNamespaceURI(null);
				for (int i=0; i<conjunction.contexts.size(); i++) {
					NamespaceContext c = conjunction.contexts.get(i);
					String ns = c.getNamespaceURI(prefix);
					if (ns!=null) return ns;
				}
				return null;
			}
		});
		final String exp = path;
		final XPathExpression compiled = xpath.compile(path);
		if (verbose) 
			System.out.println(exp);

		return new XPathExpression() {
			public String evaluate(Object source) throws XPathExpressionException {
				return compiled.evaluate(source);
			}
			public String evaluate(InputSource source) throws XPathExpressionException {
				return compiled.evaluate(source);
			}
			public Object evaluate(Object source, QName returnType) throws XPathExpressionException {
				return compiled.evaluate(source, returnType);
			}
			public Object evaluate(InputSource source, QName returnType) throws XPathExpressionException {
				return compiled.evaluate(source, returnType);
			}
			public String toString() {
				return exp;
			}
		};
	}
		
	/* define dynamically generated parameters {{ template, target } ... } passed to the constructor
	 * list test-cases by enumerating test files in the test directory 
	 * A test file has the TEST_FILE_SIGNIFIER in the filename 
	 * A test file serves as an RDFa template
	 * A target filename has the TEST_FILE_SIGNIFIER removed 
	 */

	@Parameters
	public static Collection<Object[]> listCases() {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		System.out.println("list-cases"+cl.toString());
		String path = cl.getResource(test_dir).getPath();
		System.out.println(path);
		File testDir = new File(path);
		if (testDir.exists() && testDir.isDirectory())
			return listCases(testDir);
		return null;
	}

	private static Collection<Object[]> listCases(File dir) {
		Collection<Object[]> cases = new ArrayList<Object[]>();

		File[] testFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File file, String filename) {
				return (filename.endsWith(TEST_FILE_SUFFIX+".xhtml")
				|| (new File(file,filename).isDirectory() && !filename.startsWith(".")) ) ;
		}});
		// enumerate test files (RDFa templates)
		for (File f: testFiles) {
			if (f.isDirectory())
				cases.addAll(listCases(f));
			else {
				// find the corresponding target (if it exists)
				String target = f.getName();
				int n = target.lastIndexOf(TEST_FILE_SUFFIX);
				target = target.substring(0, n)+".xhtml";
				cases.add(new Object[] { f, new File(dir,target) });
			}
		}
		return cases;
	}

	/* return only failing XPaths */
	
	XPathExpression evaluateXPaths(XPathIterator iterator, Document doc) throws Exception {
		while (iterator.hasNext()) {
			XPathExpression exp = iterator.next();
			NodeList result = (NodeList) exp.evaluate(doc,XPathConstants.NODESET);
			boolean negativeTest = exp.toString().startsWith("-");
			int solutions = result.getLength();
			// a positive test should return exactly one solution
			boolean failure = ((solutions!=1 && !negativeTest)
			// a negative test should return no solutions
			 || (solutions>0 && negativeTest)) ;
			if (failure) return exp; // fail
		}
		return null;
	}
	
	/* a document is only viewable if it defines a fragment that is about '?this'*/
	
	boolean isViewable(Document doc) throws Exception {
		XPath xpath = xPathFactory.newXPath();
		// there are no namespaces in this xpath so a prefix resolver is not required
		String exp = "//*[@about='?this']";
		XPathExpression compiled = xpath.compile(exp);
		Object result = compiled.evaluate(doc,XPathConstants.NODE);
		return result!=null;
	}
	
	/* order independent equivalence */
	/* check that all elements in the target appear in the output, and vice versa */
	
	boolean equivalent(Document outputDoc, Document targetDoc, String base) throws Exception {
		XPathExpression evalOutput=null, evalTarget=null;
		
		if (outputDoc==null || targetDoc==null) return false;
		
		// Match output to target
		evalOutput = evaluateXPaths(new XPathIterator(outputDoc,base), targetDoc) ;

		if (verbose) System.out.println();
		// Match target to output
		evalTarget = evaluateXPaths(new XPathIterator(targetDoc,base), outputDoc) ;

		if ((evalOutput!=null || evalTarget!=null || verbose) && !show_sparql) {
			if (!verbose) System.out.println("\nTEST: "+template);
			
			System.out.println("\nOUTPUT");
			if (evalOutput!=null) System.out.println("FAILS: "+evalOutput);
			write(outputDoc,System.out);
			System.out.println();
			
			System.out.println("\nTARGET: "+target);
			if (evalTarget!=null) System.out.println("FAILS: "+evalTarget);
			write(targetDoc,System.out);
			System.out.println();
		}
		return evalOutput==null && evalTarget==null;
	}

	@BeforeClass
	public static void setUp() throws Exception {		
		xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
		xmlInputFactory.setProperty("http://java.sun.com/xml/stream/properties/ignore-external-dtd", true);
		xmlInputFactory.setProperty("http://java.sun.com/xml/stream/properties/report-cdata-event", true);
		
		// XPath
		xPathFactory = XPathFactory.newInstance();

		// initialize an in-memory store
		repository = new SailRepository(new MemoryStore());
		repository.initialize();
	}

	@AfterClass
	public static void tearDown() throws Exception {		
		repository.shutDown();
	}
	
	@Before
	public void initialize() throws Exception {
		con = repository.getConnection();

		// clear the repository of earlier contexts and namespaces
		con.clear();
		con.clearNamespaces();
		
		// used in tests of undefined namespaces
		con.setNamespace("xsd", "http://www.w3.org/2001/XMLSchema");
		
		// use the target filename as the base URL if not 'legacy', 'construct' or 'fragment' (all construct queries) ;
		if (!test_set.contains("legacy") && !test_set.contains("construct") && !test_set.contains("fragment")) 
			base = DATA_ATTRIBUTE_TEST_BASE;
		else base = target.toURI().toURL().toString();
		
		// if the target is supplied parse it for RDF and load the repository
		if (target.exists()) {
			loadRepository(con, parseRDFa(target, base));
			testPI(xmlInputFactory.createXMLEventReader(new FileReader(target)));
		}
	}
	
	@After
	public void closure() throws Exception {
		if (con!=null) con.close();	
	}
	
	/* test Processing Instructions */	
	void testPI(XMLEventReader xml) throws Exception {
		while (xml.hasNext()) {
			XMLEvent e = xml.nextEvent();
			if (e.isProcessingInstruction()) {
				if (e.toString().contains("repository clear") 
					|| e.toString().contains("clear repository")) {
					con.clear();
				}
			}
		}
	}

	/* Produce SPARQL from the RDFa template using SPARQLProducer.
	 * Generate XHTML+RDFa using RDFaProducer.
	 * Test equivalence of the target and generated output.
	 */
	
	@Test
	public void test() throws Exception {
		assumeTrue(test_set.contains("select"));
		try {
			if (verbose || show_rdf || show_sparql || show_xml || show_results) {
				System.out.println("\nUNION SELECT TEST: "+template);
				write(readDocument(template),System.out);
			}
			// produce SPARQL from the RDFa template
			TemplateEngineFactory tef = TemplateEngineFactory.newInstance();
			TemplateEngine engine = tef.createTemplateEngine(con);
			Template temp = engine.getTemplate(new FileInputStream(template), base);

			//XMLEventReader xml = xmlInputFactory.createXMLEventReader(src); 
			ValueFactory vf = con.getValueFactory();
			URI self = vf.createURI(base);
			//xml = xmlInputFactory.createXMLEventReader(new FileReader(template));
			MapBindingSet bindings = new MapBindingSet();
			bindings.addBinding("this", self);
			XMLEventReader xrdfa = temp.openResultReader(temp.getQuery(), bindings);

			Document outputDoc = asDocument(xrdfa);
		
			boolean ok = equivalent(outputDoc,readDocument(target),base);
			if (!ok || verbose || show_rdf) {
				System.out.println("RDF (from target):");
				write(exportGraph(con), System.out);
			}
			if (!ok) {
				System.out.println("\nTEMPLATE:");
				write(readDocument(template),System.out);
			}
			if (!ok || verbose || show_sparql) {
				System.out.println("\nSPARQL (from template):");
				System.out.println(temp.getQuery()+"\n");
			}
			if (!ok || verbose || show_xml) {
				System.out.println("\nOUTPUT:");
				write(outputDoc,System.out);
			}
			if (!ok || verbose || show_results) {
				System.out.println("\nRESULTS:");
				TupleQuery q = con.prepareTupleQuery(SPARQL, temp.getQuery(), base);
				q.setBinding("this", self);
				TupleQueryResult results = q.evaluate();
				results = q.evaluate();
				while (results.hasNext()) System.out.println(results.next());
			}
			if (!show_rdf && !show_sparql && !show_xml && !show_results) 
				assertTrue(ok);
		}
		catch (Exception e) {
			System.out.println("UNION SELECT TEST: "+template);
			throw e;
		}
	}
		
	public static void main(String[] args) {
		try {
			RDFaGenerationTest test;
			
			for (int i=0; i<args.length; i++) {
				String arg = args[i];
				if (arg.equals("-verbose")) verbose = true;
				// just show the generated queries (don't run the test)
				else if (arg.equals("-rdf")) show_rdf = true;
				else if (arg.equals("-sparql")) show_sparql = true;
				else if (arg.equals("-xml")) show_xml = true;
				else if (arg.equals("-results")) show_results = true;
				//else if (arg.equals("-legacy")) test_set = "legacy";
				//else if (arg.equals("-construct")) test_set = "construct";
				else if (arg.equals("-select")) test_set = "select";
				//else if (arg.equals("-fragment")) test_set = "fragment";
				else if (arg.equals("-data")) test_set = "data";
				else if (!arg.startsWith("-")) test_dir = arg;
			}
			if (!new File(test_dir).isDirectory()) {
				System.out.println("usage: [-verbose] [-show] [-legacy] [-union] [-fragment] [-data] [test_dir]");
				return;
			}
			
			setUp();

			// run the dynamically generated test-cases
	        Collection<Object[]> cases = listCases();
	        Iterator<Object[]> parameters = cases.iterator();
	        while (parameters.hasNext()) {
	        	Object[] param = parameters.next();
	        	test = new RDFaGenerationTest((File)param[0], (File)param[1]);
				test.initialize();
				//test.legacyTest();
				//test.unionConstructTest();
				test.test();
				//test.fragmentTest();
				test.closure();
	        }
			tearDown();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
