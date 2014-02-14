/*
 * Copyright (c) 2013 3 Round Stones Inc., Some Rights Reserved
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
package org.callimachusproject.xproc;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.ParserConfigurationException;

import junit.framework.TestCase;

import org.apache.http.client.HttpClient;
import org.callimachusproject.client.HttpClientFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.xmlcalabash.core.XProcException;

public class SerializeCssTest extends TestCase {

	private final HttpClient client = HttpClientFactory.getInstance()
			.createHttpClient("http://example.com/");

	private static final String IDENTITY = "<p:pipeline version='1.0'\n"
			+ "xmlns:p='http://www.w3.org/ns/xproc'\n"
			+ "xmlns:c='http://www.w3.org/ns/xproc-step'\n"
			+ "xmlns:calli='http://callimachusproject.org/rdf/2009/framework#'>\n"
			+ "<p:serialization port='result' media-type='text/css' method='text' />\n"
			+ "\n"
			+ "    <p:declare-step type='calli:deserialize-css'>\n"
			+ "        <p:input port='source' sequence='true' primary='true' />\n"
			+ "        <p:option name='content-type'/>\n"
			+ "        <p:option name='encoding'/>\n"
			+ "        <p:option name='charset'/>\n"
			+ "        <p:option name='flavor'/>\n"
			+ "        <p:output port='result' sequence='true' />\n"
			+ "    </p:declare-step>\n"
			+ "\n"
			+ "    <p:declare-step type='calli:serialize-css'>\n"
			+ "        <p:input port='source' sequence='true' primary='true' />\n"
			+ "        <p:option name='content-type'/>\n"
			+ "        <p:output port='result' sequence='true' />\n"
			+ "    </p:declare-step>\n" + "\n" + "<calli:deserialize-css/>\n"
			+ "<calli:serialize-css/>\n" + "</p:pipeline>\n";

	private static final String PROPERTIES = "<p:pipeline version='1.0'\n"
			+ "xmlns:p='http://www.w3.org/ns/xproc'\n"
			+ "xmlns:c='http://www.w3.org/ns/xproc-step'\n"
			+ "xmlns:calli='http://callimachusproject.org/rdf/2009/framework#'\n"
			+ "xmlns:css='http://callimachusproject.org/xmlns/2013/cssx#'>\n"
			+ "<p:serialization port='result' media-type='text/css' method='text' />\n"
			+ "\n"
			+ "    <p:declare-step type='calli:deserialize-css'>\n"
			+ "        <p:input port='source' sequence='true' primary='true' />\n"
			+ "        <p:option name='content-type'/>\n"
			+ "        <p:option name='encoding'/>\n"
			+ "        <p:option name='charset'/>\n"
			+ "        <p:option name='flavor'/>\n"
			+ "        <p:output port='result' sequence='true' />\n"
			+ "    </p:declare-step>\n"
			+ "\n"
			+ "    <p:declare-step type='calli:serialize-css'>\n"
			+ "        <p:input port='source' sequence='true' primary='true' />\n"
			+ "        <p:option name='content-type'/>\n"
			+ "        <p:output port='result' sequence='true' />\n"
			+ "    </p:declare-step>\n" + "\n" + "<calli:deserialize-css/>\n"
			+ "<calli:serialize-css>\n"
			+ "<p:input port='source' select='//css:property' />\n"
			+ "</calli:serialize-css>\n"
			+ "<p:wrap-sequence wrapper='c:data' />\n" + "</p:pipeline>\n";

	private static final String CSS = ".step {\n"
			+ "	background: white;\n"
			+ "}\n"
			+ "div#step-2 {\n"
			+ "	background: #D1B377;\n"
			+ "}\n"
			+ "div#step-4 {\n"
			+ "	background: url(http://callimachusproject.org/themes/2013/earth/images/screen-w3c.png);\n"
			+ "}\n";

	private static final String CSSX = "<css:style-sheet xmlns:css='http://callimachusproject.org/xmlns/2013/cssx#'>\n"
			+ "<css:style selector='.step'><css:property name='background'>white</css:property></css:style>\n"
			+ "<css:style selector='div#step-2'><css:property name='background'>#D1B377</css:property></css:style>\n"
			+ "<css:style selector='div#step-4'><css:property name='background'>url(http://callimachusproject.org/themes/2013/earth/images/screen-w3c.png)</css:property></css:style>\n"
			+ "</css:style-sheet>";

	private static final String SERIALIZE = "<p:pipeline version='1.0'\n"
			+ "xmlns:p='http://www.w3.org/ns/xproc'\n"
			+ "xmlns:c='http://www.w3.org/ns/xproc-step'\n"
			+ "xmlns:calli='http://callimachusproject.org/rdf/2009/framework#'\n"
			+ "xmlns:css='http://callimachusproject.org/xmlns/2013/cssx#'>\n"
			+ "<p:serialization port='result' media-type='text/css' method='text' />\n"
			+ "\n"
			+ "    <p:declare-step type='calli:serialize-css'>\n"
			+ "        <p:input port='source' sequence='true' primary='true' />\n"
			+ "        <p:option name='content-type'/>\n"
			+ "        <p:output port='result' sequence='true' />\n"
			+ "    </p:declare-step>\n" + "\n" + "<calli:serialize-css>\n"
			+ "<p:input port='source'>\n" + "<p:inline>\n" + CSSX
			+ "</p:inline>" + "</p:input>" + "</calli:serialize-css>\n"
			+ "</p:pipeline>\n";

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testEmptySheet() throws Exception {
		assertRoundTrip("");
	}

	@Test
	public void testCharset() throws Exception {
		assertRoundTrip("@charset \"UTF-8\";");
	}

	@Test
	public void testImport() throws Exception {
		assertRoundTrip("@import url(landscape.css) screen;");
	}

	@Test
	public void testNamespace() throws Exception {
		assertRoundTrip("@namespace svg http://www.w3.org/2000/svg;");
	}

	@Test
	public void testMedia() throws Exception {
		assertRoundTrip("@media screen, print { body { line-height: 1.2; } }");
	}

	@Test
	public void testPage() throws Exception {
		assertRoundTrip("@page :first { margin: 2in; }");
	}

	@Test
	public void testStyle() throws Exception {
		assertRoundTrip("div#id { margin: 20px; }");
	}

	@Test
	public void testFontFace() throws Exception {
		assertRoundTrip("@font-face { font-family: Bitstream Vera Serif Bold; src: http://developer.mozilla.org/@api/deki/files/2934/=VeraSeBd.ttf; }");
	}

	@Test
	public void testDocument() throws Exception {
		assertRoundTrip("@document http://www.w3.org/, url-prefix(http://www.w3.org/Style/), domain(mozilla.org), regexp(https:.*) { body { color: purple; background: yellow; } }");
	}

	@Test
	public void testMediaProperties() throws Exception {
		assertEquals(
				"line-height: 1.2em;font-size: 16px;",
				pipe("@media screen, print { body { line-height: 1.2em; font-size: 16px } }",
						PROPERTIES));
	}

	@Test
	public void testSerialize() throws Exception {
		assertEquals(CSS, pipe(null, SERIALIZE));
	}

	@Test
	public void testGradient() throws Exception {
		assertRoundTrip("@page { background: radial-gradient(rgb(240,240,240),rgb(190,190,190)); }");
	}

	private void assertRoundTrip(String expected) throws IOException,
			SAXException, ParserConfigurationException {
		String actual = pipe(expected, IDENTITY);
		assertEquals(expected.replaceAll("\\s+", " ").trim(), actual
				.replaceAll("\\s+", " ").trim());
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
				"text/css;charset=UTF-8").asString();
	}

}
