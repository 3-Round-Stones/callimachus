package org.callimachusproject.xproc;

import java.io.IOException;
import java.io.StringReader;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

public class PipelineTest extends TestCase {

	private static final String IDENTITY = "<p:pipeline version='1.0' xmlns:p='http://www.w3.org/ns/xproc'>\n" +
			"<p:identity/></p:pipeline>";
	private static final String RENDER = "<p:pipeline version='1.0' xmlns:p='http://www.w3.org/ns/xproc' xmlns:calli='http://callimachusproject.org/rdf/2009/framework#'>\n" +
			"<p:declare-step type='calli:render'>\n" +
			"<p:input port='source' sequence='true' primary='true'/>\n" +
			"<p:input port='template'/>\n" +
			"<p:input port='parameters' kind='parameter' />" +
			"<p:option name='output-base-uri'/>\n" +
			"<p:option name='parameter-base-uri'/>\n" +
			"<p:output port='result' sequence='true' primary='true'/>\n" +
			"</p:declare-step>\n"+
			"<calli:render>\n" +
			"<p:input port='template'><p:inline>\n" +
			"<hello resource='?hpage'/>"+
			"</p:inline></p:input>\n" +
			"</calli:render>\n" +
			"</p:pipeline>";

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testIdentity() throws Exception {
		assertEquals("<hello/>", pipe("<hello/>", IDENTITY));
	}

	@Test
	public void testRender() throws Exception {
		String results = "<sparql xmlns='http://www.w3.org/2005/sparql-results#'>\n" +
				"<head> <variable name='hpage'/> <variable name='name'/> <variable name='age'/> <variable name='mbox'/> <variable name='friend'/> </head>\n" +
				"<results>\n" +
				"<result> <binding name='hpage'>	<uri>http://work.example.org/bob/</uri> </binding> <binding name='name'>	<literal xml:lang='en'>Bob</literal> </binding> <binding name='age'>	<literal datatype='http://www.w3.org/2001/XMLSchema#integer'>30</literal> </binding> <binding name='mbox'>	<uri>mailto:bob@work.example.org</uri> </binding> </result>\n" +
				"</results>\n" +
				"</sparql>";
		assertEquals("<hello xmlns:calli=\"http://callimachusproject.org/rdf/2009/framework#\" resource=\"http://work.example.org/bob/\"/>", pipe(results, RENDER));
	}

	private String pipe(String source, String pipeline) throws IOException,
			SAXException {
		PipelineFactory pf = PipelineFactory.getInstance();
		Pipeline pipe = pf.createPipeline(new StringReader(pipeline), "http://example.com/");
		if (source == null)
			return pipe.pipe().asString();
		return pipe.pipeReader(new StringReader(source), "http://example.com/").asString();
	}

}
