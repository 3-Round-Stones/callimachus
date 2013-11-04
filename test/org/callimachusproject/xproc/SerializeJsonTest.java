package org.callimachusproject.xproc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.apache.http.client.HttpClient;
import org.callimachusproject.client.HttpClientFactory;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcException;

public class SerializeJsonTest extends TestCase {
	private static final String JSON = "{\n" + "    \"firstName\": \"John\",\n"
			+ "    \"lastName\": \"Smith\",\n" + "    \"age\": 25,\n"
			+ "    \"address\": {\n"
			+ "        \"streetAddress\": \"21 2nd Street\",\n"
			+ "        \"city\": \"New York\",\n"
			+ "        \"state\": \"NY\",\n"
			+ "        \"postalCode\": 10021\n" + "    },\n"
			+ "    \"phoneNumbers\": [\n" + "        {\n"
			+ "            \"type\": \"home\",\n"
			+ "            \"number\": \"212 555-1234\"\n" + "        },\n"
			+ "        {\n" + "            \"type\": \"fax\",\n"
			+ "            \"number\": \"646 555-4567\"\n" + "        }\n"
			+ "    ]\n" + "}";

	private final HttpClient client = HttpClientFactory.getInstance()
			.createHttpClient("http://example.com/");

	private static final String IDENTITY = "<p:pipeline version='1.0'\n"
			+ "xmlns:p='http://www.w3.org/ns/xproc'\n"
			+ "xmlns:c='http://www.w3.org/ns/xproc-step'\n"
			+ "xmlns:calli='http://callimachusproject.org/rdf/2009/framework#'>\n"
			+ "<p:serialization port='result' media-type='application/json' method='text' />\n"
			+ "\n"
			+ "    <p:declare-step type='calli:deserialize-json'>\n"
			+ "        <p:input port='source' sequence='true' primary='true' />\n"
			+ "        <p:option name='content-type'/>\n"
			+ "        <p:option name='encoding'/>\n"
			+ "        <p:option name='charset'/>\n"
			+ "        <p:option name='flavor'/>\n"
			+ "        <p:output port='result' sequence='true' />\n"
			+ "    </p:declare-step>\n"
			+ "\n"
			+ "    <p:declare-step type='calli:serialize-json'>\n"
			+ "        <p:input port='source' sequence='true' primary='true' />\n"
			+ "        <p:option name='content-type'/>\n"
			+ "        <p:output port='result' sequence='true' />\n"
			+ "    </p:declare-step>\n" + "\n"
			+ "<calli:deserialize-json encoding='base64' charset='UTF-8' flavor='jsonx' />\n"
			+ "<calli:serialize-json/>\n" + "</p:pipeline>\n";

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testJsonRoundTrip() throws Exception {
		String expected = new JSONObject(JSON).toString();
		String actual = pipe(JSON, IDENTITY);
		assertEquals(expected, new JSONObject(actual).toString());
	}

	private String pipe(String source, String pipeline) throws IOException,
			SAXException, XProcException, ParserConfigurationException {
		PipelineFactory pf = PipelineFactory.newInstance();
		Pipeline pipe = pf.createPipeline(new StringReader(pipeline),
				"http://example.com/", client);
		if (source == null)
			return pipe.pipe().asString();
		ByteArrayInputStream in = new ByteArrayInputStream(
				source.getBytes("UTF-8"));
		return pipe.pipeStreamOf(in, "http://example.com/",
				"application/json;charset=UTF-8").asString();
	}

}
