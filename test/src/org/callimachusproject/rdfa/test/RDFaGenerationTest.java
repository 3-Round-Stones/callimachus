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
import static org.callimachusproject.rdfa.test.TestUtility.asXMLEventReader;
import static org.callimachusproject.rdfa.test.TestUtility.exportGraph;
import static org.callimachusproject.rdfa.test.TestUtility.loadRepository;
import static org.callimachusproject.rdfa.test.TestUtility.parseRDFa;
import static org.callimachusproject.rdfa.test.TestUtility.readDocument;
import static org.callimachusproject.rdfa.test.TestUtility.transform;
import static org.callimachusproject.rdfa.test.TestUtility.write;
import static org.callimachusproject.stream.SPARQLWriter.toSPARQL;
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

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.RDFaReader;
import org.callimachusproject.stream.BufferedXMLEventReader;
import org.callimachusproject.stream.GraphPatternReader;
import org.callimachusproject.stream.RDFStoreReader;
import org.callimachusproject.stream.RDFXMLEventReader;
import org.callimachusproject.stream.RDFaProducer;
import org.callimachusproject.stream.ReducedTripleReader;
import org.callimachusproject.stream.SPARQLProducer;
import org.callimachusproject.stream.SPARQLResultReader;
import org.callimachusproject.stream.TriplePatternStore;
import org.callimachusproject.stream.TriplePatternVariableStore;
import org.callimachusproject.stream.XMLElementReader;
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
	static final String DTD_FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
	static final String INDENT_AMOUNT = "{http://xml.apache.org/xslt}indent-amount";
	static final String PROPERTIES = "RDFaGeneration/RDFaGenerationTest.props";
	static final Pattern pathPattern = Pattern.compile("^.*element=([/\\d+]+).*\\z");
	// location of the XSLT transform relative to the test directory
	static final String TRANSFORM = "../webapps/callimachus/operations/construct.xsl";
	static final String TEST_FILE_SUFFIX = "-test";
	static final String DATA_ATTRIBUTE_TEST_BASE = "http://example.org/test";
	static final String DATA_ATTRIBUTE_TEST_XSLT = "../webapps/callimachus/operations/data-attributes.xsl";
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
	static String test_dir = "RDFaGeneration/test-suite/test-cases/";
	static {
		// override default test-dir with VM arg
		if (System.getProperty("dir")!=null)
			test_dir = System.getProperty("dir");
	}
	
	// the default test set to run on the default test_dir (other tests are assumed disabled)
	static String test_set = "legacy construct select fragment";
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
		File testDir = new File(test_dir);
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
	
	static XMLEventReader applyDataAttributeXSLT(XMLEventReader xml, String _this) throws Exception {
		Transformer transformer = transformerFactory.newTransformer(new StreamSource(DATA_ATTRIBUTE_TEST_XSLT));
		transformer.setParameter("this", _this);
		transformer.setURIResolver(new URIResolver() {
			@Override
			public Source resolve(String href, String base)
					throws TransformerException {
				try {
					// we should only ever be loading the menu result set
					XMLEventReader xml = xmlInputFactory
							.createXMLEventReader(new FileReader(MENU));
					return new StAXSource(xml);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
		});
		return asXMLEventReader(transform(xml,transformer));
	}
	
	private Document applyConstructXSLT(RDFXMLEventReader rdfxml, File self, String query, String element)
		throws Exception {
		Transformer transformer = transformerFactory.newTransformer(new StreamSource(TRANSFORM));
		transformer.setParameter("this", self.toURI().toURL().toString());	
		if (query!=null) transformer.setParameter("query", query);
		if (element!=null) transformer.setParameter("element", element);
		
		transformer.setURIResolver(new URIResolver() {
			@Override
			public Source resolve(String href, String base) throws TransformerException {
				try {
					Matcher pathMatcher = pathPattern.matcher(href);
					String path=null;
					if (pathMatcher.matches()) path = pathMatcher.group(1);
					else return null;
					// we should only ever be loading the template
					XMLEventReader xml = xmlInputFactory.createXMLEventReader(new FileReader(template));
					// extract the fragment
					XMLElementReader fragment = new XMLElementReader(xml,path);
					return new StAXSource(fragment);
				}
				catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}	
		});
		return transform(rdfxml,transformer);
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
	
	private static boolean verifyXPath(XPathExpression xpath, Document doc) throws Exception {
		if (xpath==null) return false;
		NodeList result = (NodeList) xpath.evaluate(doc,XPathConstants.NODESET);
		return result.getLength()==1;
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
	
	private TriplePatternStore constructQuery(String base,
			RDFEventReader sparql, boolean useVariant) throws RDFParseException {
		TriplePatternStore query ;	
		// TriplePatternVariableStore is a variation on TriplePatternStore 
		// inserts extra relationships to identify variables
		if (useVariant) query = new TriplePatternVariableStore(base) ;
		else query = new TriplePatternStore(base) ;
		query.consume(sparql);
		sparql.close();
		return query;
	}
	
	static NodeList findFragmentIdentifiers(Document doc) throws Exception {
		XPath xpath = xPathFactory.newXPath();
		// there are no namespaces in this xpath so a prefix resolver is not required
		// do not use data-add, data-more?
		String exp = "//*[@data-search]";
		XPathExpression compiled = xpath.compile(exp);
		NodeList results = (NodeList) compiled.evaluate(doc,XPathConstants.NODESET);
		return results;
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
		
		// used in tests of undefined namespaces
		con.setNamespace("xsd", "http://www.w3.org/2001/XMLSchema");
		
		// use the target filename as the base URL if not 'legacy', 'construct' or 'fragment' (all construct queries) ;
		if (!test_set.contains("data") && !test_set.contains("construct") && !test_set.contains("fragment")) 
			base = DATA_ATTRIBUTE_TEST_BASE;
		else base = target.toURI().toURL().toString();
		
		// if the target is supplied parse it for RDF and load the repository
		if (target.exists()) {
			testPI(xmlInputFactory.createXMLEventReader(new FileReader(target)));
			loadRepository(con, parseRDFa(target, base));
		}
	}
	
	@After
	public void closure() throws Exception {
		if (con!=null) con.close();	
	}
	
	/* Produce SPARQL from the RDFa template using GraphPatternReader.
	 * Generate SPARQL result-set using RDFStoreReader.
	 * Test equivalence of the target and transform output.
	 */
	
	@Test
	public void legacyTest() throws Exception {
		assumeTrue(test_set.contains("legacy"));
		try {
			if (verbose || show_rdf || show_sparql || show_results || show_xml) {
				System.out.println("\nLEGACY TEST: "+template);
				write(readDocument(template),System.out);
			}
			XMLEventReader xml = xmlInputFactory.createXMLEventReader(new FileReader(template)); 
			RDFEventReader rdfa = new RDFaReader(base, xml, null);
			RDFEventReader sparql = new GraphPatternReader(rdfa);
			TriplePatternStore query = constructQuery(base, sparql, true);
			String uri = isViewable(readDocument(template))?base:null;
			RDFEventReader results = new RDFStoreReader(query, con, uri);
			results = new ReducedTripleReader(results);
			RDFXMLEventReader rdfxml = new RDFXMLEventReader(results);			
			Document outputDoc = applyConstructXSLT(rdfxml,template,null,null);
	
			boolean ok = equivalent(outputDoc,readDocument(target),base);
			if (!ok || verbose || show_rdf) {
				System.out.println("RDF (from target):");
				write(exportGraph(con), System.out);
			}
			if (!ok || verbose || show_sparql) {
				System.out.println("SPARQL (from test):\n"+query);
			}
			if (!ok || verbose || show_results) {
				System.out.println("\nRESULTS:");
				results = new RDFStoreReader(query, con, null);				
				while (results.hasNext()) System.out.println(results.next());
			}
			if (!ok || verbose || show_xml) {
				System.out.println("\nOUTPUT:");
				write(outputDoc,System.out);
			}
			if (!show_rdf && !show_sparql && !show_results) 
				assertTrue(ok);
		}
		catch (Exception e) {
			System.out.println("LEGACY TEST: "+template);
			fail();
		}
	}
	
	/* Produce SPARQL from the RDFa template using SPARQLProducer.
	 * Generate SPARQL result-set using SPARQLResultReader.
	 * Test equivalence of the target and transform output.
	 */
	
	@Test
	public void unionConstructTest() throws Exception {
		assumeTrue(test_set.contains("construct"));
		try {
			if (verbose || show_rdf || show_sparql || show_results || show_xml) {
				System.out.println("\nUNION CONSTRUCT TEST: "+template);
				write(readDocument(template),System.out);
			}
			// produce SPARQL from the RDFa template
			XMLEventReader xml = xmlInputFactory.createXMLEventReader(new FileReader(template)); 
			RDFEventReader rdfa = new RDFaReader(base, xml, null);
			RDFEventReader sparql = new SPARQLProducer(rdfa);

			TriplePatternStore query = constructQuery(base, sparql, true);
			// the URI is supplied for a ?view template, but not for ?construct
			String uri = isViewable(readDocument(template))?base:null;
			RDFEventReader results = new SPARQLResultReader(query, con, uri);	
			results = new ReducedTripleReader(results);
			RDFXMLEventReader rdfxml = new RDFXMLEventReader(results);			
			Document outputDoc = applyConstructXSLT(rdfxml,template,null,null);
	
			boolean ok = equivalent(outputDoc,readDocument(target),base);
			if (!ok || verbose || show_rdf) {
				System.out.println("RDF (from target):");
				write(exportGraph(con), System.out);
			}
			if (!ok || verbose || show_sparql) {
				System.out.println("\nSPARQL:");
				System.out.println(query+"\n");
			}
			if (!ok || verbose || show_results) {
				System.out.println("\nRESULTS:");
				results = new SPARQLResultReader(query, con, null);
				while (results.hasNext()) System.out.println(results.next());				
			}
			if (!ok || verbose || show_xml) {
				System.out.println("\nOUTPUT:");
				write(outputDoc,System.out);
			}
			if (!show_sparql && !show_sparql && !show_results && !show_xml) 
				assertTrue(ok);
		}
		catch (Exception e) {
			System.out.println("UNION TEST: "+template);
			fail();
		}
	}
	
	/* Look for template fragment identifiers in the output
	 * Generate SPARQL query and transformed output from the fragment
	 * Test that the fragment output is a sub-document of the prior output
	 */
	
	@Test
	public void fragmentTest() throws Exception {
		assumeTrue(test_set.contains("fragment"));
		try {
			XMLEventReader xml = xmlInputFactory.createXMLEventReader(new FileReader(template));
			RDFEventReader rdfa = new RDFaReader(base, xml, null);
			RDFEventReader sparql = new SPARQLProducer(rdfa);
			TriplePatternStore query = constructQuery(base, sparql, true);
			// the URI is supplied for a ?view template, but not for ?construct
			String uri = isViewable(readDocument(template))?base:null;
			RDFEventReader results = new SPARQLResultReader(query, con, uri);	
			results = new ReducedTripleReader(results);
			RDFXMLEventReader rdfxml = new RDFXMLEventReader(results);			
			Document outputDoc = applyConstructXSLT(rdfxml,template,null,null);
			
			// look for fragment identifiers
			NodeList fragments = findFragmentIdentifiers(outputDoc);
			for (int i=0; i<fragments.getLength(); i++) {
				org.w3c.dom.Node frag = fragments.item(i);
				String path = getFragmentIdentifiers((Element)frag);

				// Generate the query from the fragment
				xml = xmlInputFactory.createXMLEventReader(new FileReader(template));
				XMLElementReader xmlFragment = new XMLElementReader(xml,path);
				rdfa = new RDFaReader(base, xmlFragment, null);
				sparql = new SPARQLProducer(rdfa);
				query = constructQuery(base, sparql, true);
				
				results = new SPARQLResultReader(query, con, uri);	
				results = new ReducedTripleReader(results);
				rdfxml = new RDFXMLEventReader(results);
								
				Document fragment = applyConstructXSLT(rdfxml,template,"edit",path);
				XPathExpression xpath = conjoinXPaths(fragment,base) ;
				
				// the output fragment should be a sub-document of the target
				boolean ok = verifyXPath(xpath,outputDoc);
				if (!ok || verbose) {
					System.out.println("\nFRAGMENT TEST: "+template+"\n");
		
					System.out.println("TEMPLATE:");
					write(readDocument(template),System.out);
		
					System.out.println("\nOUTPUT:");
					write(outputDoc,System.out);
					
					System.out.println("\nPATH: "+path);
					// show the fragment addressed by the path
					xml = xmlInputFactory.createXMLEventReader(new FileReader(template));
					write(new XMLElementReader(xml,path), System.out);
					
					System.out.println("\nXPATH:");
					System.out.println(xpath);
					
					System.out.println("\nSPARQL: ");
					System.out.println(query);

					System.out.println("RESULT SET: ");
					results = new SPARQLResultReader(query, con, uri);	
					while (results.hasNext()) System.out.println(results.next());
		
					System.out.println("\nTRANSFORMED FRAGMENT:");
					write(fragment,System.out);
				}
				if (!show_sparql) assertTrue(ok);		
			}
		}
		catch (Exception e) {
			System.out.println("FRAGMENT TEST: "+template);
			fail();
		}
	}
	
	/* test Processing Instructions */	
	void testPI(XMLEventReader xml) throws Exception {
		while (xml.hasNext()) {
			XMLEvent e = xml.nextEvent();
			if (e.isProcessingInstruction()) {
				if (e.toString().contains("repository clear")) {
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
	public void unionSelectTest() throws Exception {
		assumeTrue(test_set.contains("select"));
		try {
			if (verbose || show_rdf || show_sparql || show_xml || show_results) {
				System.out.println("\nUNION SELECT TEST: "+template);
				write(readDocument(template),System.out);
			}
			// produce SPARQL from the RDFa template
			XMLEventReader src = xmlInputFactory.createXMLEventReader(new FileReader(template));
			BufferedXMLEventReader xml = new BufferedXMLEventReader(src);
			int start = xml.mark();

			//XMLEventReader xml = xmlInputFactory.createXMLEventReader(src); 
			RDFEventReader rdfa = new RDFaReader(base, xml, null);			
			SPARQLProducer sparql = new SPARQLProducer(rdfa,SPARQLProducer.QUERY.SELECT);
			String query = toSPARQL(sparql);			
			ValueFactory vf = con.getValueFactory();
			TupleQuery q = con.prepareTupleQuery(SPARQL, query, base);
			URI self = vf.createURI(base);
			q.setBinding("this", self);
			TupleQueryResult results = q.evaluate();
			xml.reset(start);
			//xml = xmlInputFactory.createXMLEventReader(new FileReader(template));
			XMLEventReader xrdfa = new RDFaProducer(xml, results, sparql.getOrigins(),self,con);

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
				System.out.println(query+"\n");
			}
			if (!ok || verbose || show_xml) {
				System.out.println("\nOUTPUT:");
				write(outputDoc,System.out);
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
			System.out.println("UNION SELECT TEST: "+template);
			fail();
		}
	}
	
	@Test
	public void dataAttributeTest() throws Exception {
		assumeTrue(test_set.contains("data"));

		if (verbose) {
			System.out.println("\nTEST: "+template);
		}
		// produce SPARQL from the RDFa template
		XMLEventReader xml = xmlInputFactory.createXMLEventReader(new FileReader(template));
		xml = applyDataAttributeXSLT(xml,base);
		BufferedXMLEventReader buffer = new BufferedXMLEventReader(xml);
		int start = buffer.mark();

		RDFEventReader rdfa = new RDFaReader(base, buffer, null);			
		SPARQLProducer sparql = new SPARQLProducer(rdfa,SPARQLProducer.QUERY.SELECT);
		String query = toSPARQL(sparql);

		ValueFactory vf = con.getValueFactory();
		TupleQuery q = con.prepareTupleQuery(SPARQL, query, base);
		URI self = vf.createURI(base);
		q.setBinding("this", self);
		TupleQueryResult results = q.evaluate();

		buffer.reset(start);
		XMLEventReader xrdfa = new RDFaProducer(buffer, results, sparql.getOrigins(),self, con);
		Document outputDoc = asDocument(xrdfa);
		boolean ok = equivalent(outputDoc,readDocument(target),base);
		if (!ok || verbose) {
			System.out.println("RDF (from target):");
			write(exportGraph(con), System.out);
			
			System.out.println("\nSPARQL:");
			System.out.println(query+"\n");
			
			System.out.println("\nRESULTS:");
			results = q.evaluate();			
			while (results.hasNext()) System.out.println(results.next());

			if (verbose && ok) {
				System.out.println("\nOUTPUT:");
				write(outputDoc,System.out);
			}
		}
		assertTrue(ok);
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
				else if (arg.equals("-legacy")) test_set = "legacy";
				else if (arg.equals("-construct")) test_set = "construct";
				else if (arg.equals("-select")) test_set = "select";
				else if (arg.equals("-fragment")) test_set = "fragment";
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
				test.legacyTest();
				test.unionConstructTest();
				test.unionSelectTest();
				test.fragmentTest();
				test.closure();
	        }
			tearDown();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
