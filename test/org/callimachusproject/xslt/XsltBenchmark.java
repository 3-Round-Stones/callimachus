package org.callimachusproject.xslt;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.callimachusproject.xml.XMLEventReaderFactory;
import org.xml.sax.InputSource;

public class XsltBenchmark extends TestCase {
	public static int runs = 1;

	public static Test suite() {
		List<Test> tests = new ArrayList<Test>();
		for (int i = 0; i < runs; i++) {
			addTestsTo(tests);
		}
		Random random = new Random();
		TestSuite result = new TestSuite(XsltBenchmark.class.getName());
		while (!tests.isEmpty()) {
			int i = random.nextInt(tests.size());
			result.addTest(tests.remove(i));
		}
		return result;
	}

	private static void addTestsTo(List<Test> tests) {
		TestSuite suite = new TestSuite(XsltBenchmark.class);
		Enumeration<?> e = suite.tests();
		while (e.hasMoreElements()) {
			tests.add((Test) e.nextElement());
		}
	}

	private final XMLInputFactory xmlInput = XMLInputFactory.newInstance();
	private final XMLEventReaderFactory inFactory = XMLEventReaderFactory
			.newInstance();
	private int iterations = 1;
	private ClassLoader cl = XsltBenchmark.class.getClassLoader();
	private InputStream xsl;
	private Templates xslt;
	private List<InputStream> inputs;
	private StreamResult result;
	private TransformerFactory tfactory = TransformerFactory.newInstance();
	private final DocumentBuilderFactory builder = DocumentBuilderFactory
			.newInstance();
	private long duration;
	{
		builder.setNamespaceAware(true);
		try {
			builder.setFeature(
					"http://apache.org/xml/features/nonvalidating/load-external-dtd",
					false);
		} catch (ParserConfigurationException e) {
			throw new AssertionError(e);
		}
	}

	public XsltBenchmark(String name) {
		super(name);
	}

	@Override
	public void runBare() throws Throwable {
		super.runBare();
		long total = 0;
		long min = Long.MAX_VALUE;
		long max = 0;
		for (int i = 0; i < iterations; i++) {
			super.runBare();
			total += duration;
			if (duration < min) {
				min = duration;
			}
			if (duration > max) {
				max = duration;
			}
		}
		if (iterations > 2) {
			long avg = (total - min - max) / (iterations - 2);
			double sec = avg / 1000000000.0;
			System.out.println(getName() + ":\t" + sec + "s");
		}
	}

	@Override
	public void setUp() throws Exception {
		xsl = cl.getResourceAsStream("xml/layout.xsl");
		Properties properties = new Properties();
		InputStream index = cl.getResourceAsStream("xml/index.properties");
		if (index != null) {
			properties.load(index);
		}
		inputs = new ArrayList<InputStream>();
		for (Object key : properties.keySet()) {
			inputs.add(cl.getResourceAsStream(key.toString()));
		}
		DocumentBuilder db = builder.newDocumentBuilder();
		InputSource is = new InputSource(xsl);
		URL layout = cl.getResource("xml/layout.xsl");
		if (layout != null) {
			String systemId = layout.toString();
			is.setSystemId(systemId);
			Source source = new DOMSource(db.parse(is), systemId);
			xslt = tfactory.newTemplates(source);
		}
		result = new StreamResult(new ByteArrayOutputStream(262144));
	}

	@Override
	public void tearDown() throws Exception {
		if (xsl != null) {
			xsl.close();
		}
		for (InputStream in : inputs) {
			in.close();
		}
	}

	@Override
	protected void runTest() throws Throwable {
		long before = System.nanoTime();
		super.runTest();
		long after = System.nanoTime();
		duration = after - before;
	}

	public void testDOM() throws Exception {
		for (InputStream in : inputs) {
			DocumentBuilder db = builder.newDocumentBuilder();
			Source source = new DOMSource(db.parse(in));
			xslt.newTransformer().transform(source, result);
		}
	}

	public void testXMLEvent() throws Exception {
		for (InputStream in : inputs) {
			XMLEventReader reader = inFactory.createXMLEventReader(in);
			StAXSource source = new StAXSource(reader);
			xslt.newTransformer().transform(source, result);
		}
	}

	public void testXMLStream() throws Exception {
		for (InputStream in : inputs) {
			XMLStreamReader reader = xmlInput.createXMLStreamReader(in);
			StAXSource source = new StAXSource(reader);
			xslt.newTransformer().transform(source, result);
		}
	}

	public void testStream() throws Exception {
		for (InputStream in : inputs) {
			xslt.newTransformer().transform(new StreamSource(in), result);
		}
	}
}
