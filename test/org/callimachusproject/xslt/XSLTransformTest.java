package org.callimachusproject.xslt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.callimachusproject.annotations.xslt;
import org.callimachusproject.xml.XMLEventReaderFactory;
import org.openrdf.annotations.Bind;
import org.openrdf.annotations.Iri;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryConfig;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XSLTransformTest extends TestCase {
	private static final String XML_STRING = "<AliBaba/>";
	private static final byte[] XML_BYTES = XML_STRING.getBytes(Charset
			.forName("UTF-8"));
	public static final String XSLT_ECHO = "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
			+ "<xsl:template match='/'>"
			+ "<xsl:copy-of select='.'/>"
			+ "</xsl:template></xsl:stylesheet>";
	public static final String XSLT_HELLO_WORLD = "<message xsl:version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>hello world!</message>";

	private Concept concept;

	public static Test suite() throws Exception {
		return suite(XSLTransformTest.class);
	}
	protected static final String DEFAULT = "memory";

	protected static final String DELIM = "#";

	private interface RepositoryFactory {
		Repository createRepository() throws Exception;
	}

	public static LinkedHashMap<String, RepositoryFactory> factories = new LinkedHashMap<String, RepositoryFactory>();
	static {
		factories.put(DEFAULT, new RepositoryFactory() {
			public Repository createRepository() {
				return new SailRepository(new MemoryStore());
			}
		});
	}

	public static Test suite(Class<? extends TestCase> subclass)
			throws Exception {
		String sname = subclass.getName();
		TestSuite suite = new TestSuite(sname);
		for (Method method : subclass.getMethods()) {
			String name = method.getName();
			if (name.startsWith("test")) {
				for (String code : factories.keySet()) {
					TestCase test = subclass.newInstance();
					test.setName(name + DELIM + code);
					suite.addTest(test);
				}
			}
		}
		return suite;
	}

	private String factory;

	protected Repository repository;

	protected ObjectRepositoryConfig config = new ObjectRepositoryConfig();

	protected ObjectConnection con;

	protected ObjectFactory of;

	public XSLTransformTest() {
		factory = DEFAULT;
		setFactory(DEFAULT);
	}

	public XSLTransformTest(String name) {
		setName(name);
	}

	@Override
	public String getName() {
		String name = super.getName();
		if (DEFAULT.equals(factory))
			return name;
		return name + DELIM + factory;
	}

	@Override
	public void setName(String name) {
		if (name.contains(DELIM)) {
			super.setName(name.substring(0, name.indexOf(DELIM)));
			this.factory = name.substring(name.lastIndexOf(DELIM) + 1);
		} else {
			super.setName(name);
			factory = DEFAULT;
		}
	}

	public String getFactory() {
		return factory;
	}

	public void setFactory(String factory) {
		this.factory = factory;
	}

	protected Repository createRepository() throws Exception {
		return factories.get(factory).createRepository();
	}

	protected ObjectRepository getRepository() throws Exception {
		Repository repository = createRepository();
		repository.initialize();
		RepositoryConnection con = repository.getConnection();
		try {
			con.setAutoCommit(false);
			con.clear();
			con.clearNamespaces();
			con.setNamespace("test", "urn:test:");
			con.setAutoCommit(true);
		} finally {
			con.close();
		}
		ObjectRepositoryFactory factory = new ObjectRepositoryFactory();
		return factory.createRepository(config, repository);
	}

	@Override
	public void setUp() throws Exception {
		config.addConcept(Concept.class);
		config.addConcept(XMLFile.class);
		repository = getRepository();
		con = (ObjectConnection) repository.getConnection();
		con.setAutoCommit(false);
		con.setNamespace("rdf", RDF.NAMESPACE);
		con.setNamespace("rdfs", RDFS.NAMESPACE);
		con.setAutoCommit(true);
		of = con.getObjectFactory();
		concept = con.addDesignation(con.getObject("urn:test:concept"),
				Concept.class);
	}

	@Override
	protected void tearDown() throws Exception {
		try {
			if (con.isOpen()) {
				con.close();
			}
			repository.shutDown();
		} catch (Exception e) {
		}
	}

	@Iri("urn:mimetype:application/xml")
	public interface XMLFile {

	}

	@Iri("urn:test:Concept")
	public interface Concept {

		@xslt(XSLT_ECHO)
		String echo(String input);

		@xslt(XSLT_ECHO)
		Node echo(Node input);

		@xslt(XSLT_ECHO)
		Document echo(Document input);

		@xslt(XSLT_ECHO)
		DocumentFragment echo(DocumentFragment input);

		@xslt(XSLT_ECHO)
		Element echo(Element input);

		@xslt(XSLT_ECHO)
		XMLEventReader echo(XMLEventReader input);

		@xslt(XSLT_ECHO)
		Readable echo(Readable input);

		@xslt(XSLT_ECHO)
		Reader echo(Reader input);

		@xslt(XSLT_ECHO)
		ReadableByteChannel echo(ReadableByteChannel input);

		@xslt(XSLT_ECHO)
		InputStream echo(InputStream input);

		@xslt(XSLT_ECHO)
		ByteArrayOutputStream echo(ByteArrayOutputStream input);

		@xslt(XSLT_ECHO)
		byte[] echo(byte[] input);

		@xslt(XSLT_ECHO)
		String toString(String input);

		@xslt(XSLT_ECHO)
		String toString(Node input);

		@xslt(XSLT_ECHO)
		String toString(Document input);

		@xslt(XSLT_ECHO)
		String toString(DocumentFragment input);

		@xslt(XSLT_ECHO)
		String toString(Element input);

		@xslt(XSLT_ECHO)
		String toString(XMLEventReader input);

		@xslt(XSLT_ECHO)
		String toString(Readable input);

		@xslt(XSLT_ECHO)
		String toString(Reader input);

		@xslt(XSLT_ECHO)
		String toString(ReadableByteChannel input);

		@xslt(XSLT_ECHO)
		String toString(InputStream input);

		@xslt(XSLT_ECHO)
		String toString(ByteArrayOutputStream input);

		@xslt(XSLT_ECHO)
		String toString(byte[] input);

		@xslt(XSLT_HELLO_WORLD)
		Element hello();

		@xslt("<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
				+ "<xsl:param name='arg' />"
				+ "<xsl:output method='text' />"
				+ "<xsl:template match='/'>"
				+ "<xsl:text>hello </xsl:text><xsl:value-of select='$arg'/><xsl:text>!</xsl:text>"
				+ "</xsl:template></xsl:stylesheet>")
		String hello(@Bind("arg") String arg);

		@xslt("<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
				+ "<xsl:template match='/'>"
				+ "</xsl:template></xsl:stylesheet>")
		InputStream nothing();

		@xslt("<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
				+ "<xsl:param name='arg' />"
				+ "<xsl:output method='text' />"
				+ "<xsl:template match='/'>"
				+ "<xsl:value-of select='$arg'/>"
				+ "</xsl:template></xsl:stylesheet>")
		String csv(@Bind("arg") Object arg);

		@xslt("<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
				+ "<xsl:param name='arg' />"
				+ "<xsl:output method='text' />"
				+ "<xsl:template match='/'>"
				+ "<xsl:for-each select='$arg'><xsl:value-of select='.'/><xsl:text>,</xsl:text></xsl:for-each>"
				+ "</xsl:template></xsl:stylesheet>")
		String csv(@Bind("arg") Set<?> arg);
	}

	public void testObjectStringParameter() throws Exception {
		assertEquals("world", concept.csv("world"));
	}

	public void testObjectIntegerParameter() throws Exception {
		assertEquals("1", concept.csv(1));
	}

	public void testString() throws Exception {
		assertEquals(XML_STRING, concept.echo("<AliBaba/>"));
	}

	public void testNode() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		doc.appendChild(doc.createElement("AliBaba"));
		assertEquals(XML_STRING, concept.toString(concept.echo((Node) doc)));
	}

	public void testDocument() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		doc.appendChild(doc.createElement("AliBaba"));
		assertEquals(XML_STRING, concept.toString(concept.echo(doc)));
	}

	public void testDocumentFragment() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		DocumentFragment frag = doc.createDocumentFragment();
		frag.appendChild(doc.createElement("AliBaba"));
		assertEquals(XML_STRING, concept.toString(concept.echo(frag)));
	}

	public void testElement() throws Exception {
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().newDocument();
		Element element = doc.createElement("AliBaba");
		assertEquals(XML_STRING, concept.toString(concept.echo(element)));
	}

	public void testXMLEventReader() throws Exception {
		Reader reader = new StringReader(XML_STRING);
		XMLEventReader events = XMLEventReaderFactory.newInstance()
				.createXMLEventReader("urn:test:location", reader);
		XMLEventReader result = concept.echo(events);
		assertEquals(XML_STRING, concept.toString(result));
	}

	public void testReadable() throws Exception {
		Readable reader = new StringReader(XML_STRING);
		assertEquals(XML_STRING, concept.toString(concept.echo(reader)));
	}

	public void testReader() throws Exception {
		Reader reader = new StringReader(XML_STRING);
		assertEquals(XML_STRING, concept.toString(concept.echo(reader)));
	}

	public void testReadableByteChannel() throws Exception {
		ReadableByteChannel reader = Channels
				.newChannel(new ByteArrayInputStream(XML_BYTES));
		assertEquals(XML_STRING, concept.toString(concept.echo(reader)));
	}

	public void testInputStream() throws Exception {
		InputStream stream = new ByteArrayInputStream(XML_BYTES);
		assertEquals(XML_STRING, concept.toString(concept.echo(stream)));
	}

	public void testByteArrayOutputStream() throws Exception {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		stream.write(XML_BYTES);
		assertEquals(XML_STRING, concept.toString(concept.echo(stream)));
	}

	public void testByteArray() throws Exception {
		assertEquals(XML_STRING, concept.toString(concept.echo(XML_BYTES)));
	}

	public void testNoInput() throws Exception {
		assertEquals("hello world!", concept.hello().getTextContent());
	}

	public void testParameter() throws Exception {
		assertEquals("hello world!", concept.hello("world"));
	}

	public void testNothing() throws Exception {
		InputStream in = concept.nothing();
		if (in != null) {
			System.out.print("Open>");
			int read;
			byte[] buf = new byte[1024];
			while ((read = in.read(buf)) >= 0) {
				System.out.write(buf, 0, read);
			}
			System.out.println("<Close");
		}
		assertNull(in);
	}

}
