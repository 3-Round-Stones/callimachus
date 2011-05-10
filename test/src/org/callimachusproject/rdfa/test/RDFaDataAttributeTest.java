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
import static org.callimachusproject.rdfa.test.TestUtility.write;
import static org.callimachusproject.stream.SPARQLWriter.toSPARQL;
import static org.junit.Assert.assertTrue;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFaReader;
import org.callimachusproject.stream.BufferedXMLEventReader;
import org.callimachusproject.stream.RDFaProducer;
import org.callimachusproject.stream.SPARQLProducer;
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
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Dymnamic Test Suite for the generation of XHTML+RDFa pages from RDFa templates.
 * 
 * @author Steve Battle
 * 
 */

// This is a parameterized test that runs over the test directory
@RunWith(Parameterized.class)
public class RDFaDataAttributeTest {
	static final String DTD_FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
	static final String INDENT_AMOUNT = "{http://xml.apache.org/xslt}indent-amount";
	static final String PROPERTIES = "RDFaGeneration/RDFaGenerationTest.props";
	static final Pattern pathPattern = Pattern.compile("^.*element=([/\\d+]+).*\\z");
	static final String TEST_FILE_SUFFIX = "-test";
	
	// this is the default test directory
	static String test_dir = "RDFaGeneration/test-suite/data-attributes";

	// static properties defined in @BeforeClass setUp()
	static XMLInputFactory xmlInputFactory;
	static TransformerFactory transformerFactory;
	static DocumentBuilderFactory documentBuilderFactory;
	static XPathFactory xPathFactory;
	static Repository repository;
	
	// static flags set in main()
	static boolean verbose = false;
	
	// properties defined by constructor
	// XHTML RDFa template used to derive SPARQL query 
	File template;
	// target XHTML with embedded RDFa (also used as RDF data source)
	File target;

	// object properties defined in @Before initialize() 
	RepositoryConnection con;
	String base;
	
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

	public RDFaDataAttributeTest(File template, File target) {
		this.template = template;
		this.target = target;	
	}
		
	// the XSLT to apply to the template
	//static final String XSLT = "webapps/layout/template.xsl";
	static final String DATA_XSLT = "../webapps/callimachus/operations/data-attributes.xsl";
	static final String MENU = "examples/menu.xml";
	static final String DATA_ATTRIBUTE_BASE = "http://example.org/test";
	
	private static Document transform(XMLEventReader xml, Transformer transformer) throws Exception {
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
		Document doc = documentBuilderFactory.newDocumentBuilder().newDocument();
		transformer.transform(new StAXSource(xml), new DOMResult(doc));
		return doc;
	}

	static XMLEventReader applyDataXSLT(XMLEventReader xml, String _this) throws Exception {
		Transformer transformer = transformerFactory.newTransformer(new StreamSource(DATA_XSLT));
		transformer.setParameter("this", _this);
		Document doc = transform(xml,transformer);
//		System.out.println("\n\nDATA ATTRIBUTES:\n");
//		write(doc,System.out);
		return asXMLEventReader(doc);
	}
	
	/* order independent equivalence */
	/* check that all elements in the target appear in the output, and vice versa */
		
	boolean equivalent(Document outputDoc, Document targetDoc, String base) throws Exception {
		XPathExpression evalOutput=null, evalTarget=null;
		
		if (outputDoc==null || targetDoc==null) {
			return false;
		}
		
		// Match output to target
		evalOutput = evaluateXPaths(new XPathIterator(outputDoc,base,true), targetDoc) ;

		if (verbose) System.out.println();
		// Match target to output
		evalTarget = evaluateXPaths(new XPathIterator(targetDoc,base,true), outputDoc) ;
		
		if ((evalOutput!=null || evalTarget!=null || verbose)) {
			if (!verbose) System.out.println("\nTEST: "+template);
			
			System.out.println("\nOUTPUT: "+target);
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
	
	/* return only failing XPaths */
	
	XPathExpression evaluateXPaths(XPathIterator iterator, Document doc) throws Exception {
		while (iterator.hasNext()) {
			XPathExpression exp = iterator.next();
			//System.out.println(exp);
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

	@BeforeClass
	public static void setUp() throws Exception {		
		xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
		
		// XSL
		transformerFactory = TransformerFactory.newInstance();
		
		// DOM
		documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		documentBuilderFactory.setFeature(DTD_FEATURE, false);
		
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
		
		// use the target filename as the base URL
		//base = target.toURI().toURL().toString();
		base = DATA_ATTRIBUTE_BASE;
		
		// if the target is supplied parse it for RDF and load the repository
		if (target.exists())
			loadRepository(con, parseRDFa(target, base));
	}
	
	@After
	public void closure() throws Exception {
		if (con!=null) con.close();	
	}
		
	/* If a data-attribute exists in the target, check that it also exists in the output. */
	
	@Test
	public void dataAttributeTest() throws Exception {
		if (verbose) {
			System.out.println("\nTEST: "+template);
			//write(readDocument(template),System.out);
		}
		// only run these tests if the target contains data-attributes
		Document templateDoc = readDocument(template);
		
		String base = DATA_ATTRIBUTE_BASE;
		// produce SPARQL from the RDFa template
		XMLEventReader xml = xmlInputFactory.createXMLEventReader(new FileReader(template));
		xml = applyDataXSLT(xml,base);
		BufferedXMLEventReader buffer = new BufferedXMLEventReader(xml);
		int start = buffer.mark();
		
		//write(buffer,System.out);
		//buffer.reset(start);

		RDFEventReader rdfa = new RDFaReader(base, buffer, null);			
		SPARQLProducer sparql = new SPARQLProducer(rdfa,SPARQLProducer.QUERY.SELECT);
		String query = toSPARQL(sparql);

		ValueFactory vf = con.getValueFactory();
		TupleQuery q = con.prepareTupleQuery(SPARQL, query, base);
		URI self = vf.createURI(base);
		q.setBinding("this", self);
		TupleQueryResult results = q.evaluate();

		//xml = xmlInputFactory.createXMLEventReader(new FileReader(template));
		buffer.reset(start);
		//xml = applyDataXSLT(buffer,base);
		XMLEventReader xrdfa = new RDFaProducer(buffer, results, sparql.getOrigins(),self);
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
			RDFaDataAttributeTest test;
			
			for (int i=0; i<args.length; i++) {
				String arg = args[i];
				if (arg.equals("-verbose")) verbose = true;
				else if (!arg.startsWith("-")) test_dir = arg;
			}
			if (!new File(test_dir).isDirectory()) {
				return;
			}
			
			setUp();

			// run the dynamically generated test-cases
	        Collection<Object[]> cases = listCases();
	        Iterator<Object[]> parameters = cases.iterator();
	        while (parameters.hasNext()) {
	        	Object[] param = parameters.next();
	        	test = new RDFaDataAttributeTest((File)param[0], (File)param[1]);
				test.initialize();
				test.dataAttributeTest();
				test.closure();
	        }
			tearDown();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
