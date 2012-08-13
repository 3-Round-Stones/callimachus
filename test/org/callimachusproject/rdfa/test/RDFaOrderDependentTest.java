/*
 * Portions Copyright (c) 2010-2011 Talis Inc, Some Rights Reserved
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

import static org.callimachusproject.engine.helpers.SPARQLWriter.toSPARQL;
import static org.callimachusproject.rdfa.test.Utility.asDocument;
import static org.callimachusproject.rdfa.test.Utility.exportGraph;
import static org.callimachusproject.rdfa.test.Utility.loadRepository;
import static org.callimachusproject.rdfa.test.Utility.parseRDFa;
import static org.callimachusproject.rdfa.test.Utility.readDocument;
import static org.callimachusproject.rdfa.test.Utility.write;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.TransformerFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFaReader;
import org.callimachusproject.engine.helpers.OrderedSparqlReader;
import org.callimachusproject.engine.helpers.RDFaProducer;
import org.callimachusproject.engine.helpers.SPARQLProducer;
import org.callimachusproject.engine.helpers.XMLEventList;
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
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * Dynamic Test Suite for the generation of XHTML+RDFa pages from RDFa templates.
 * Where RDFaGenerationTest is order independent, this tests for order dependent features
 * 
 * @author Steve Battle
 * 
 */

// This is a parameterized test that runs over the test directory (test_dir)
@RunWith(Parameterized.class)
public class RDFaOrderDependentTest {
	static final String DTD_FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
	static final String INDENT_AMOUNT = "{http://xml.apache.org/xslt}indent-amount";
	static final String PROPERTIES = "RDFaGeneration/RDFaGenerationTest.props";
	static final Pattern pathPattern = Pattern.compile("^.*element=([/\\d+]+).*\\z");
	// location of the XSLT transform relative to the test directory
	static final String TRANSFORM = "../webapps/callimachus/operations/construct.xsl";
	static final String TEST_FILE_SUFFIX = "-test";
	static final String DATA_ATTRIBUTE_TEST_BASE = "http://example.org/test";
	static final String MENU = "examples/menu.xml";
			
	// static properties defined in @BeforeClass setUp()
	static XMLInputFactory xmlInputFactory;
	static TransformerFactory transformerFactory;
	static XPathFactory xPathFactory;
	static Repository repository;
	
	// static flags set in main()
	static boolean verbose = false;
	static boolean show_rdf = false;
	static boolean show_sparql = false;
	static boolean show_xml = false;
	static boolean show_results = false;

	// this is the default test directory
	static String test_dir = "RDFaGeneration/order-dependent-tests/";
	static {
		// override default test-dir with VM arg
		if (System.getProperty("dir")!=null)
			test_dir = System.getProperty("dir");
	}
	
	// the default test set to run on the default test_dir (other tests are assumed disabled)
	//static String test_set = "legacy construct fragment select data";
	static String test_set = "select data";
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

	public RDFaOrderDependentTest(File template, File target) {
		this.template = template;
		this.target = target;
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
		String path = cl.getResource(test_dir).getPath();
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
		
	/* a document is only viewable if it defines a fragment that is about '?this'*/
	
	boolean isViewable(Document doc) throws Exception {
		XPath xpath = xPathFactory.newXPath();
		// there are no namespaces in this xpath so a prefix resolver is not required
		String exp = "//*[@about='?this']";
		XPathExpression compiled = xpath.compile(exp);
		Object result = compiled.evaluate(doc,XPathConstants.NODE);
		return result!=null;
	}
	
	/* order dependent equivalence
	 * check that all elements and text (including whitespace) appear in the same order
	 * Doesn't check attributes (the ordering is variable)
	 */
	
	boolean equivalent(XMLEventReader output, XMLEventReader target, String base) throws Exception {	
		if (output==null || target==null) return false;
		boolean ok = true;
		
		while (ok && target.hasNext() && output.hasNext()) {
			XMLEvent e1 = target.nextEvent() ;
			while (isNegativeTest(e1)) 
				e1 = skipElement(target);

			XMLEvent e2 = output.nextEvent();
			if (e1.isStartDocument() && e2.isStartDocument()) continue;
			if (e1.isStartElement() && e2.isStartElement())
				ok = e1.asStartElement().getName().equals(e2.asStartElement().getName());
			else if (e1.isCharacters() && e2.isCharacters())
				ok = e1.asCharacters().getData().equals(e2.asCharacters().getData()) ;
			else if (e1.isEndElement() && e2.isEndElement())
				ok = e1.asEndElement().getName().equals(e2.asEndElement().getName()) ;
			else return e1.isEndDocument() && e2.isEndDocument() ;
		}
		return ok ;
	}
			
	private XMLEvent skipElement(XMLEventReader target) throws XMLStreamException {
		int depth = 0;
		XMLEvent e=null;
		while (depth>=0 && target.hasNext()) {
			e = target.nextEvent() ;	
			if (e.isStartElement()) depth++ ;
			else if (e.isEndElement()) depth--;
		}
		return target.hasNext()?target.nextEvent():null;
	}

	private boolean isNegativeTest(XMLEvent e1) {
		if (e1.isStartElement()) {
			StartElement s = e1.asStartElement();
			Attribute a = s.getAttributeByName(new QName("class"));
			return a!=null && a.getValue().equals("negative-test");
		}
		return false;
	}

	static String getFragmentIdentifiers(Element element) {
		NamedNodeMap attributes = element.getAttributes();
		for (int i=0; i<attributes.getLength(); i++) {
			Attr a = (Attr) attributes.item(i);
			if (a.getName().equals("data-search")) {
				Matcher pathMatcher = pathPattern.matcher(a.getValue());
				if (pathMatcher.matches()) return pathMatcher.group(1);
			}
		}
		return null;
	}

	@BeforeClass
	public static void setUp() throws Exception {		
		xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		xmlInputFactory.setProperty("http://java.sun.com/xml/stream/properties/report-cdata-event", true);
		
		// XSL
		transformerFactory = TransformerFactory.newInstance();
		
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
		
		base = DATA_ATTRIBUTE_TEST_BASE;
		
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
	public void orderDependentTest() throws Exception {
		assumeTrue(test_set.contains("select"));
		try {
			if (verbose || show_rdf || show_sparql || show_xml || show_results) {
				System.out.println("\nORDER DEPENDENT TEST: "+template);
				write(readDocument(template),System.out);
			}
			// produce SPARQL from the RDFa template
			XMLEventReader src = xmlInputFactory.createXMLEventReader(new FileReader(template));
			XMLEventList xml = new XMLEventList(src);

			RDFEventReader rdfa = new RDFaReader(base, xml.iterator(), base);			
			SPARQLProducer sparql = new SPARQLProducer(rdfa);
			String query = toSPARQL(new OrderedSparqlReader(sparql));			
			ValueFactory vf = con.getValueFactory();
			TupleQuery q = con.prepareTupleQuery(SPARQL, query, base);
			URI self = vf.createURI(base);
			q.setBinding("this", self);
			TupleQueryResult results = q.evaluate();
			XMLEventReader xrdfa = new RDFaProducer(xml.iterator(), results, sparql.getOrigins());
		
			XMLEventReader targetXML = xmlInputFactory.createXMLEventReader(new FileReader(target));
			boolean ok = equivalent(xrdfa,targetXML,base);
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
				System.out.println(query+"\n");
			}
			if (!ok || verbose || show_xml) {
				System.out.println("\nOUTPUT:");
				results = q.evaluate();
				Document doc = asDocument(new RDFaProducer(xml.iterator(), results, sparql.getOrigins()));
				write(doc,System.out);
			}
			if (!ok || verbose || show_results) {
				System.out.println("\nRESULTS:");
				results = q.evaluate();
				while (results.hasNext()) System.out.println(results.next());
			}
			if (!show_rdf && !show_sparql && !show_xml && !show_results) 
				assertTrue(ok);
		}
		catch (Exception e) {
			System.out.println("ORDER DEPENDENT TEST: "+template);
			e.printStackTrace();
			fail();
		}
	}
		
	public static void main(String[] args) {
		try {
			RDFaOrderDependentTest test;
			
			for (int i=0; i<args.length; i++) {
				String arg = args[i];
				if (arg.equals("-verbose")) verbose = true;
				// just show the generated queries (don't run the test)
				else if (arg.equals("-rdf")) show_rdf = true;
				else if (arg.equals("-sparql")) show_sparql = true;
				else if (arg.equals("-xml")) show_xml = true;
				else if (arg.equals("-results")) show_results = true;
				else if (!arg.startsWith("-")) test_dir = arg;
			}
			if (!new File(test_dir).isDirectory()) {
				System.out.println("usage: [-verbose] [-rdf] [-sparql] [-xml] [-results] [test_dir]");
				return;
			}
			
			setUp();

			// run the dynamically generated test-cases
	        Collection<Object[]> cases = listCases();
	        Iterator<Object[]> parameters = cases.iterator();
	        while (parameters.hasNext()) {
	        	Object[] param = parameters.next();
	        	test = new RDFaOrderDependentTest((File)param[0], (File)param[1]);
				test.initialize();
				test.orderDependentTest();
				test.closure();
	        }
			tearDown();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
