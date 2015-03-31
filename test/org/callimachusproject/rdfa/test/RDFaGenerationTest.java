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

import static org.callimachusproject.rdfa.test.Utility.asDocument;
import static org.callimachusproject.rdfa.test.Utility.exportGraph;
import static org.callimachusproject.rdfa.test.Utility.loadRepository;
import static org.callimachusproject.rdfa.test.Utility.parseRDFa;
import static org.callimachusproject.rdfa.test.Utility.readDocument;
import static org.callimachusproject.rdfa.test.Utility.write;
import static org.junit.Assume.assumeTrue;
import static org.openrdf.query.QueryLanguage.SPARQL;
import info.aduna.text.StringUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

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

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

import org.apache.http.client.HttpClient;
import org.callimachusproject.engine.RDFaReader;
import org.callimachusproject.engine.Template;
import org.callimachusproject.engine.TemplateEngine;
import org.callimachusproject.engine.helpers.XMLEventList;
import org.openrdf.http.object.client.HttpClientFactory;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.repository.util.RepositoryUtil;
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
public class RDFaGenerationTest extends TestCase {
	// location of the XSLT transform relative to the test directory
	private static final String TEMPLATE_FILE_SUFFIX = "-test.xhtml";
	private static final String SOURCE_FILE_SUFFIX = "-source.xhtml";
	private static final String EXPECTED_FILE_SUFFIX = ".xhtml";
	private static final String DATA_ATTRIBUTE_TEST_BASE = "http://example.org/test";

	private final HttpClient client = HttpClientFactory.getInstance().createHttpClient("http://example.com/");
	// private static properties defined in @BeforeClass setUp()
	private XMLInputFactory xmlInputFactory;
	private XPathFactory xPathFactory;
	private Repository sourceRepository;
	private Repository expectedRepository;
	private Repository actualRepository;
	
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
	private RepositoryConnection source;
	private RepositoryConnection expected;
	private RepositoryConnection actual;
	private String base;
	
	// properties defined by constructor
	// XHTML RDFa template used to derive SPARQL query 
	private File template;
	// expectedFile XHTML with embedded RDFa
	private File expectedFile;
	// source XHTML with embedded RDFa used as RDF data source
	private File sourceFile;

	public RDFaGenerationTest(String name) {
		super(name);
		this.template = new File(name.substring(name.indexOf('!') + 1));
		this.expectedFile = new File(template.getPath().replace(TEMPLATE_FILE_SUFFIX,
				EXPECTED_FILE_SUFFIX));
		this.sourceFile = new File(template.getPath().replace(TEMPLATE_FILE_SUFFIX,
				SOURCE_FILE_SUFFIX));
		if (!sourceFile.exists()) {
			sourceFile = expectedFile;
		}
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
		
	/* define dynamically generated parameters {{ template, expectedFile } ... } passed to the constructor
	 * list test-cases by enumerating test files in the test directory 
	 * A test file has the TEST_FILE_SIGNIFIER in the filename 
	 * A test file serves as an RDFa template
	 * A expectedFile filename has the TEST_FILE_SIGNIFIER removed 
	 */
	public static TestSuite suite() throws URISyntaxException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL path = cl.getResource(test_dir);
		File testDir = new File(path.toURI());
		if (testDir.exists() && testDir.isDirectory()) {
			TestSuite suite = listCases(testDir);
			suite.setName(RDFaGenerationTest.class.getName());
			return suite;
		}
		return new TestSuite(RDFaGenerationTest.class.getName());
	}

	private static TestSuite listCases(File dir) {
		TestSuite cases = new TestSuite(dir.getName());

		File[] testFiles = dir.listFiles(new FilenameFilter() {
			public boolean accept(File file, String filename) {
				return (filename.endsWith(TEMPLATE_FILE_SUFFIX)
				|| (new File(file,filename).isDirectory() && !filename.startsWith(".")) ) ;
		}});
		// enumerate test files (RDFa templates)
		for (File f: testFiles) {
			if (f.isDirectory()) {
				cases.addTest(listCases(f));
			} else {
				cases.addTest(new RDFaGenerationTest(f.getName() + "!" + f.getPath()));
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
			boolean failure = ((solutions<1 && !negativeTest)
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
	/* check that all elements in the expectedFile appear in the output, and vice versa */
	
	void assertEqualDocuments(Document expectedDoc, Document actualDoc, String base) throws Exception {
		XPathExpression unexpected=null, missing=null;
		assertNotNull(actualDoc);
		assertNotNull(expectedDoc);
		
		// Match actual to expectedFile
		unexpected = evaluateXPaths(new XPathIterator(actualDoc,base), expectedDoc) ;

		if (verbose) System.out.println();
		// Match expectedFile to actual
		missing = evaluateXPaths(new XPathIterator(expectedDoc,base), actualDoc) ;

		if ((unexpected!=null || missing!=null || verbose) && !show_sparql) {
			if (!verbose) System.out.println("\nTEMPLATE: "+template);
			
			System.out.println("\nOUTPUT");
			if (unexpected!=null) System.out.println("UNEXPECTED: "+unexpected);
			write(actualDoc,System.out);
			System.out.println();
			
			System.out.println("\nEXPECTED: "+expectedFile);
			if (missing!=null) System.out.println("MISSING: "+missing);
			write(expectedDoc,System.out);
			System.out.println();
		}
		assertEquals(null, unexpected);
		assertEquals(null, missing);
	}

	public void setUp() throws Exception {		
		xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		xmlInputFactory.setProperty(XMLInputFactory.IS_VALIDATING, false);
		xmlInputFactory.setProperty("http://java.sun.com/xml/stream/properties/ignore-external-dtd", true);
		xmlInputFactory.setProperty("http://java.sun.com/xml/stream/properties/report-cdata-event", true);
		
		// XPath
		xPathFactory = XPathFactory.newInstance();

		// initialize an in-memory store
		sourceRepository = new SailRepository(new MemoryStore());
		sourceRepository.initialize();
		source = sourceRepository.getConnection();
		expectedRepository = new SailRepository(new MemoryStore());
		expectedRepository.initialize();
		expected = expectedRepository.getConnection();
		actualRepository = new SailRepository(new MemoryStore());
		actualRepository.initialize();
		actual = actualRepository.getConnection();
		initialize();
	}

	public void tearDown() throws Exception {
		source.close();
		expected.close();
		actual.close();
		sourceRepository.shutDown();
		expectedRepository.shutDown();
		actualRepository.shutDown();
	}
	
	private void initialize() throws Exception {
		// use the expectedFile filename as the base URL if not 'legacy', 'construct' or 'fragment' (all construct queries) ;
		if (!test_set.contains("legacy") && !test_set.contains("construct") && !test_set.contains("fragment")) 
			base = DATA_ATTRIBUTE_TEST_BASE;
		else base = expectedFile.toURI().toURL().toString();
		
		// if the sourceFile is supplied parse it for RDF and load the sourceRepository
		if (sourceFile.exists()) {
			loadRepository(source, parseRDFa(sourceFile, base));
			testPI(xmlInputFactory.createXMLEventReader(new FileReader(sourceFile)));
		}
		if (expectedFile.exists()) {
			loadRepository(expected, parseRDFa(expectedFile, base));
		}
	}
	
	/* test Processing Instructions */	
	void testPI(XMLEventReader xml) throws Exception {
		while (xml.hasNext()) {
			XMLEvent e = xml.nextEvent();
			if (e.isProcessingInstruction()) {
				if (e.toString().contains("repository clear") 
					|| e.toString().contains("clear repository")) {
					source.clear();
				}
			}
		}
	}

	/* Produce SPARQL from the RDFa template using SPARQLProducer.
	 * Generate XHTML+RDFa using RDFaProducer.
	 * Test equivalence of the expectedFile and generated output.
	 */
	public void runTest() throws Exception {
		assumeTrue(test_set.contains("select"));
		if (verbose || show_rdf || show_sparql || show_xml || show_results) {
			System.out.println("\nUNION SELECT TEST: "+template);
			write(readDocument(template),System.out);
		}
		// produce SPARQL from the RDFa template
		TemplateEngine engine = TemplateEngine.newInstance(client);
		Template temp = engine.getTemplate(new FileInputStream(template), base);

		//XMLEventReader xml = xmlInputFactory.createXMLEventReader(src); 
		ValueFactory vf = source.getValueFactory();
		URI self = vf.createURI(base);
		//xml = xmlInputFactory.createXMLEventReader(new FileReader(template));
		MapBindingSet bindings = new MapBindingSet();
		bindings.addBinding("this", self);
		XMLEventList xrdfa = new XMLEventList(temp.openResult(bindings, source));
		assert xrdfa.get(0).getLocation().getCharacterOffset() >= 0;

		loadRepository(actual, new RDFaReader(base, xrdfa.iterator(), base));
		Document outputDoc = asDocument(xrdfa.iterator());

		AssertionFailedError failed = null;
		try {
			assertEqualDocuments(readDocument(expectedFile),outputDoc,base);
			assertEqualRepositories(expected, actual);
		}
		catch (AssertionFailedError e) {
			failed = e;
			System.out.println("UNION SELECT TEST: "+template);
			throw e;
		} finally {
			boolean ok = failed == null;
			if (!ok || verbose || show_rdf) {
				System.out.println("Source File: " + sourceFile);
				System.out.println("RDF (from Source File):");
				write(exportGraph(source), System.out);
			}
			if (!ok) {
				System.out.println("\nTEMPLATE:");
				write(readDocument(template),System.out);
			}
			if (!ok || verbose || show_sparql) {
				System.out.println("\nSPARQL (from template):");
				System.out.println(temp.getQueryString()+"\n");
			}
			if (!ok || verbose || show_xml) {
				System.out.println("\nOUTPUT:");
				write(outputDoc,System.out);
			}
			if (!ok || verbose || show_results) {
				System.out.println("\nRESULTS:");
				TupleQuery q = source.prepareTupleQuery(SPARQL, temp.getQueryString(), base);
				q.setBinding("this", self);
				TupleQueryResult results = q.evaluate();
				results = q.evaluate();
				while (results.hasNext()) System.out.println(results.next());
			}
		}
	}

	private void assertEqualRepositories(RepositoryConnection expected,
			RepositoryConnection actual) throws RepositoryException {
		if (!RepositoryUtil.equals(expected.getRepository(), actual.getRepository())) {
			List<Statement> expectedList = expected.getStatements(null, null, null, true).asList();
			List<Statement> actualList = actual.getStatements(null, null, null, true).asList();
			StringBuilder message = new StringBuilder(128);
			message.append("\n============ ");
			message.append(getName());
			message.append(" =======================\n");
			message.append("Expected: \n");
			for (Statement st : expectedList) {
				message.append(st.toString());
				if (!actualList.contains(st)) {
					message.append("*");
				}
				message.append("\n");
			}
			message.append("=============");
			StringUtil.appendN('=', getName().length(), message);
			message.append("========================\n");

			message.append("Actual: \n");
			for (Statement st : actualList) {
				message.append(st.toString());
				if (!expectedList.contains(st)) {
					message.append("*");
				}
				message.append("\n");
			}
			message.append("=============");
			StringUtil.appendN('=', getName().length(), message);
			message.append("========================\n");

			fail(message.toString());
		}
	}
		
	public static void main(String[] args) {
		try {
			
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

			// run the dynamically generated test-cases
			System.exit(TestRunner.run(RDFaGenerationTest.suite())
					.wasSuccessful() ? 0 : 1);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
