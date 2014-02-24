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
package org.callimachusproject.engine.helpers;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import junit.framework.TestCase;

public class XMLElementReaderTest extends TestCase {
	private XMLInputFactory xif = XMLInputFactory.newInstance();
	private XMLOutputFactory xof = XMLOutputFactory.newInstance();

	public String extract(String xptr, String xml) throws Exception {
		XMLEventReader input = xif.createXMLEventReader(new StringReader(xml));
		XMLEventReader output = new XMLElementReader(input, xptr);
		StringWriter out = new StringWriter();
		XMLEventWriter writer = xof.createXMLEventWriter(out);
		writer.add(output);
		writer.close();
		return out.toString();
	}

	public void testRootPath() throws Exception {
		assertEquals(
				"<?xml version=\"1.0\"?><root><child id=\"child\"><leaf></leaf></child></root>",
				extract("/1",
						"<?xml version=\"1.0\"?><root><child id=\"child\"><leaf></leaf></child></root>"));
	}

	public void testPath() throws Exception {
		assertEquals(
				"<?xml version=\"1.0\"?><child id=\"child\"><leaf></leaf></child>",
				extract("/1/1",
						"<?xml version=\"1.0\"?><root><child id=\"child\"><leaf></leaf></child></root>"));
	}

	public void testId() throws Exception {
		assertEquals(
				"<?xml version=\"1.0\"?><child id=\"child\"><leaf></leaf></child>",
				extract("child",
						"<?xml version=\"1.0\"?><root><child id=\"child\"><leaf></leaf></child></root>"));
	}

	public void testIdPath() throws Exception {
		assertEquals(
				"<?xml version=\"1.0\"?><leaf></leaf>",
				extract("child/1",
						"<?xml version=\"1.0\"?><root><child id=\"child\"><leaf></leaf></child></root>"));
	}
}
