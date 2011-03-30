package org.callimachusproject.rdfa.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.RDFaReader;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Triple;
import org.callimachusproject.rdfa.model.Node;
import org.callimachusproject.rdfa.model.PlainLiteral;
import org.callimachusproject.rdfa.model.Term;
import org.callimachusproject.rdfa.model.TypedLiteral;
import org.callimachusproject.stream.GraphPatternReader;
import org.callimachusproject.stream.RDFStoreReader;
import org.callimachusproject.stream.RDFXMLEventReader;
import org.callimachusproject.stream.ReducedTripleReader;
import org.callimachusproject.stream.SPARQLProducer;
import org.callimachusproject.stream.SPARQLResultReader;
import org.callimachusproject.stream.TriplePatternStore;
import org.callimachusproject.stream.TriplePatternVariableStore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openrdf.model.Graph;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.GraphImpl;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.sail.memory.MemoryStore;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

/**
 * Dymnamic Test Suite for the generation of RDFa pages from RDFa templates.
 * 
 * @author Steve Battle
 * 
 */

// This is a parameterized test that runs over the test directory
@RunWith(Parameterized.class)
public class RDFaGenerationTest {
	static final String DTD_FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";
	static final String PROPERTIES = "RDFa/RDFaGenerationTest.props";
	
	// The signifier should appear within the filename
	// The target filename is the substring before the signifier
	// Different suffixes following the signifier allow reuse of the target
	static final String TEST_FILE_SIGNIFIER = "-test";
	
	// static properties loaded by static initializer
	static String test_dir;
	static String transform;
	static { // load default properties
		loadProperties(PROPERTIES);
	}
	
	// static properties defined in @BeforeClass setUp()
	static XMLInputFactory xmlInputFactory;
	static TransformerFactory transformerFactory;
	static DocumentBuilderFactory documentBuilderFactory;
	static XPathFactory xPathFactory;
	static Repository repository;
	
	// static flag set in main()
	static boolean verbose = false;
	
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

	abstract class AbstractNamespaceContext implements NamespaceContext {
		@Override
		public abstract String getNamespaceURI(String prefix) ;
		@Override
		public String getPrefix(String uri) {
			return null;
		}
		@Override
		public Iterator<?> getPrefixes(String uri) {
			return null;
		}
	}
		
	class XPathIterator implements Iterator<Object> {	
		Queue<XPathExpression> queue = (Queue<XPathExpression>) new LinkedList<XPathExpression>();
		final String[] DISTINGUISHING_ATTRIBUTES = { "id", "name", "class" };
		List<String> distinguishingAttributes = Arrays.asList(DISTINGUISHING_ATTRIBUTES);
		
		// enumerate XPaths to all leaf nodes in the document
		XPathIterator(Document doc) throws Exception {
			enumerate(doc.getDocumentElement(),"");
		}
		private void enumerate(final Element element, String path) throws Exception {
			String prefix = element.getPrefix();
			// if this element has no prefix and the default namespace is defined then the prefix is empty
			if (prefix==null && element.lookupNamespaceURI(null)!=null) prefix = "";
			path += "/"+(prefix==null?"":prefix+":")+element.getLocalName();
			String content=null, text=null;
			boolean leaf = true;
			if (element.hasChildNodes()) {
				// add distinguishing attributes
				NamedNodeMap attributes = element.getAttributes();
				for (int i=0; i<attributes.getLength(); i++) {
					org.w3c.dom.Node node = attributes.item(i);
					Attr a = (Attr) node;
					String name = a.getName();
					if (name.startsWith("xmlns") || !distinguishingAttributes.contains(name)) 
						continue;
					if (name.equals("content")) 
						content = a.getValue();
					else path += "[@"+a.getName()+"='"+a.getValue()+"']";
				}
				// iterate over any children
				NodeList children = element.getChildNodes();
				for (int j=0; j<children.getLength(); j++) {
					org.w3c.dom.Node node = children.item(j);
					switch (node.getNodeType()) {
					case org.w3c.dom.Node.ELEMENT_NODE:
						enumerate((Element)node,path);
						leaf = false;
						break;
					case org.w3c.dom.Node.TEXT_NODE:
						Text t = (Text)node;
						String value = t.getTextContent();
						if (!value.trim().equals(""))
							text = value.trim();
					}
				}
				if (leaf) {
					// allow text and content to differ if both are set
					if (content==null) content = text;
					if (text==null) text = content;
					if (content!=null) {
						XPath xpath = xPathFactory.newXPath();
						// add namespace prefix resolver to the xpath based on the current element
						xpath.setNamespaceContext(new AbstractNamespaceContext(){
							public String getNamespaceURI(String prefix) {
								// for the empty prefix lookup the default namespace
								if (prefix.isEmpty()) return element.lookupNamespaceURI(null);
								return element.lookupNamespaceURI(prefix);
							}
						});
						final String exp = path + "[@content='"+content+"' or text()='"+text+"']";
						final XPathExpression compiled = xpath.compile(exp);
						// implement XPathExpression toString() returning the string expression
						queue.add(new XPathExpression() {
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
						});
						if (verbose) System.out.println(exp);
					}
				}
			}
		}
		@Override
		public boolean hasNext() {
			return !queue.isEmpty();
		}
		@Override
		public XPathExpression next() {
			return queue.remove();
		}
		@Override
		public void remove() {}

	}
	
	private static void loadProperties(String properties) {
		try {
			Properties props = new Properties();
			props.load(new FileReader(properties));
			test_dir = props.getProperty("test_dir");
			transform = props.getProperty("transform");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/* define dynamically generated parameters {{ template, target } ... } passed to the constructor
	 * list test-cases by enumerating test files in the test directory 
	 * A test file has the TEST_FILE_SIGNIFIER in the filename 
	 * A test file serves as an RDFa template
	 * A target filename has the TEST_FILE_SIGNIFIER (and anything after it) removed 
	 * If the target doesn't exist we simply test the generated SPARQL query for grammatical errors 
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
				return (filename.contains(TEST_FILE_SIGNIFIER)
						|| (new File(file,filename).isDirectory() && !filename.startsWith(".")) ) ;
		}});
		// enumerate test files (RDFa templates)
		for (File f: testFiles) {
			if (f.isDirectory())
				cases.addAll(listCases(f));
			else {
				// find the corresponding target (if it exists)
				String target = f.getName();
				int n = target.lastIndexOf(TEST_FILE_SIGNIFIER);
				target = target.substring(0, n)+".xhtml";
				cases.add(new Object[] { f, new File(dir,target) });
			}
		}
		return cases;
	}
	
	Statement createStatement(Triple t, ValueFactory f) throws Exception {
		Resource s = null;
		Node node = t.getSubject();
		if (node.isIRI()) {
			s = f.createURI(node.stringValue());
		}
		else {
			s = f.createBNode(node.stringValue());
		}
		URI p = f.createURI(t.getPredicate().stringValue());
		Term term = t.getObject();
		Value o = null;
		if (term.isPlainLiteral()) {
			PlainLiteral lit = term.asPlainLiteral();
			o = f.createLiteral(lit.stringValue(), lit.getLang());
		}
		else if (term.isIRI()) {
			o = f.createURI(term.stringValue());
		}
		else if (term.isNode()) {
			o = f.createBNode(term.stringValue());
		}
		else if (term.isTypedLiteral()) {
			TypedLiteral typed = term.asTypedLiteral();
			URI uri = f.createURI(typed.getDatatype().stringValue());
			o = f.createLiteral(typed.stringValue(), uri);
		}
		else throw new Exception("unimplemented term: "+term);
		return f.createStatement(s, p, o);
	}
	
	String load(File file) throws IOException {
		final FileReader reader = new FileReader(file);
		char[] block = new char[4096];
		final StringBuffer buffer = new StringBuffer();
		try {
			int len;
			while ((len = reader.read(block)) >0) {
				buffer.append(block, 0, len);
			}
		} finally {
			reader.close();
		}
		return buffer.toString();
	}
	
	RDFaReader parseRDFa(File rdfa, String base) throws Exception {
		XMLEventReader xml = xmlInputFactory.createXMLEventReader(new FileReader(rdfa));   
		RDFaReader rdf = new RDFaReader(base, xml, null);
		return rdf;
	}

	void loadRepository(RepositoryConnection con, RDFEventReader rdf) throws Exception {
	    while (rdf.hasNext()) {
	    	RDFEvent e = rdf.next() ;
			if (e.isTriple())
				con.add(createStatement(e.asTriple(),con.getValueFactory()));
	    }
	    rdf.close();
	}
	
	Graph exportGraph(RepositoryConnection con) throws Exception {
		final Graph graph = new GraphImpl();
		con.export(new RDFHandlerBase() {
			public void handleStatement(Statement statement) throws RDFHandlerException {
				graph.add(statement);
			}			
		});
		return graph;
	}

	private Document transform(RDFXMLEventReader rdfxml)
			throws TransformerConfigurationException, MalformedURLException,
			ParserConfigurationException, TransformerException,
			XMLStreamException {
		Transformer transformer = transformerFactory.newTransformer(new StreamSource(new File(transform)));
		transformer.setParameter("this", template.toURI().toURL().toString());
		Document doc = documentBuilderFactory.newDocumentBuilder().newDocument();
		transformer.transform(new StAXSource(rdfxml), new DOMResult(doc));
		return doc;
	}
	
	public static void write(Document doc, OutputStream out) throws Exception {
		Transformer transformer = transformerFactory.newTransformer();
		transformer.transform (new DOMSource(doc), new StreamResult(out));
	}
	
	public static void write(Graph graph, OutputStream out) {
		for (Iterator<Statement> i = graph.iterator(); i.hasNext(); ) {
			System.out.println(i.next());
		}
	}
	
	public static Document read(File file) throws Exception {
		if (!file.exists()) return null;
		Transformer transformer = transformerFactory.newTransformer();
		DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
		Document source = builder.parse(file);
		Document doc = builder.newDocument();
		transformer.transform (new DOMSource(source), new DOMResult(doc));
		return doc;
	}

	/* return only failing XPaths */
	
	XPathExpression evaluateXPaths(XPathIterator iterator, Document doc) throws Exception {
		while (iterator.hasNext()) {
			XPathExpression exp = iterator.next();
			Object result = exp.evaluate(doc,XPathConstants.NODE);
			if (result==null) return exp;
		}
		return null;
	}
	
	/* a document is only viewable if it defines a fragment that is about '?this' */
	
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
	
	boolean equivalent(Document outputDoc, Document targetDoc) throws Exception {
		if (targetDoc==null) return true;
		// Match output to target
		if (verbose) System.out.println("\nTEST: "+template);
		XPathExpression evalOutput = evaluateXPaths(new XPathIterator(outputDoc), targetDoc) ;

		if (verbose) System.out.println();
		// Match target to output
		XPathExpression evalTarget = evaluateXPaths(new XPathIterator(targetDoc), outputDoc) ;

		if (evalOutput!=null || evalTarget!=null || verbose) {
			if (!verbose) System.out.println("\nTEST: "+template);
			if (evalOutput!=null) System.out.println("NO MATCH IN TARGET: "+evalOutput);
			write(outputDoc,System.out);
			System.out.println();
			
			System.out.println("\nTARGET: "+target);
			if (evalTarget!=null) System.out.println("NO MATCH IN TEST OUTPUT: "+evalTarget);
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
		base = target.toURI().toURL().toString();
		
		// if the target is supplied parse it for RDF and load the repository
		if (target.exists())
			loadRepository(con, parseRDFa(target, base));
	}
	
	@After
	public void closure() throws Exception {
		if (con!=null) con.close();	
	}
	
	@Test
	public void legacyTest() throws Exception {
		// produce SPARQL from the RDFa template
		XMLEventReader xml = xmlInputFactory.createXMLEventReader(new FileReader(template)); 
		RDFEventReader rdfa = new RDFaReader(base, xml, null);
		RDFEventReader sparql = new GraphPatternReader(rdfa);
		TriplePatternStore query = constructQuery(base, sparql, true);

		// should last parameter be base?
		RDFEventReader results = new RDFStoreReader(query, con, isViewable(read(template))?base:null);
		RDFXMLEventReader rdfxml = new RDFXMLEventReader(results);			
		Document outputDoc = transform(rdfxml);

		boolean eq = equivalent(outputDoc,read(target));
		if (!eq || verbose) {
			System.out.println("\nRDF (from target):");
			write(exportGraph(con), System.out);
			System.out.println("\nSPARQL (from test):\n"+query);
			results = new RDFStoreReader(query, con, null);
			while (results.hasNext()) System.out.println(results.next());
		}
		assertTrue(eq);
	}
	
	@Test
	public void unionTest() throws Exception {
		// produce SPARQL from the RDFa template
		XMLEventReader xml = xmlInputFactory.createXMLEventReader(new FileReader(template));   
		RDFEventReader sparql = new SPARQLProducer(new RDFaReader(base, xml, null));
		TriplePatternStore query = constructQuery(base, sparql, true);
		
		// evaluate the SPARQL query
		// some tests fail 'about-typeof-test' if we pass the base instead of null
		RDFEventReader results = new SPARQLResultReader(query, con, isViewable(read(template))?base:null);
		
		results = new ReducedTripleReader(results);
		RDFXMLEventReader rdfxml = new RDFXMLEventReader(results);			
		Document outputDoc = transform(rdfxml);

		boolean eq = equivalent(outputDoc,read(target));
		if (!eq || verbose) {
			System.out.println("\nRDF (from target):");
			write(exportGraph(con), System.out);
			System.out.println("\nSPARQL (from test):\n"+query);
			results = new SPARQLResultReader(query, con, null);
			while (results.hasNext()) System.out.println(results.next());
		}
		assertTrue(eq);
	}

	// switch between different test modes from main()
	enum MODE { LEGACY, UNION };
		
	public static void main(String[] args) {
		try {
			RDFaGenerationTest test;
			
			MODE mode = MODE.UNION;
			for (int i=0; i<args.length; i++) {
				String arg = args[i];
				if (arg.equals("-verbose")) verbose = true;
				else if (arg.equals("-legacy")) mode = MODE.LEGACY;
				else if (!arg.startsWith("-")) loadProperties(arg);
			}
			
			setUp();

			// run the dynamically generated test-cases
	        Collection<Object[]> cases = listCases();
	        Iterator<Object[]> parameters = cases.iterator();
	        while (parameters.hasNext()) {
	        	Object[] param = parameters.next();
	        	test = new RDFaGenerationTest((File)param[0], (File)param[1]);
				test.initialize();
				switch (mode) {
				case LEGACY:
					test.legacyTest();
					break;
				case UNION:
					test.unionTest();					
				}
				test.closure();
	        }
			tearDown();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
