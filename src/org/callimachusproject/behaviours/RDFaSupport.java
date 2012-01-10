/*
 * Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some Rights Reserved
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
package org.callimachusproject.behaviours;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.tools.FileObject;
import javax.xml.stream.XMLEventReader;

import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.type;
import org.callimachusproject.concepts.Page;
import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFaReader;
import org.callimachusproject.engine.Template;
import org.callimachusproject.engine.TemplateEngine;
import org.callimachusproject.engine.TemplateEngineFactory;
import org.callimachusproject.engine.TemplateException;
import org.callimachusproject.engine.helpers.RDFXMLEventReader;
import org.openrdf.repository.object.RDFObject;

/**
 * Implements a few {@link Page} methods to read an RDFa document. This class is
 * responsible for applying XSL transformations, extracting x-pointer elements,
 * and parsing RDFa into sparql.
 * 
 * @author James Leigh
 * @author Steve Battle
 * 
 */
public abstract class RDFaSupport implements Page, RDFObject,
		FileObject {
	private static final TemplateEngineFactory tef = TemplateEngineFactory.newInstance();

	@method("GET")
	@query("xslt")
	@type("application/xml")
	public XMLEventReader xslt(@query("query") String query,
			@query("element") String element) throws IOException, TemplateException {
		String base = toUri().toASCIIString();
		TemplateEngine engine = tef.createTemplateEngine(getObjectConnection());
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("query", query);
		param.put("template", true);
		Template temp = engine.getTemplate(base, param).getElement(element);
		return temp.openSourceReader();
	}

	@method("GET")
	@query("triples")
	@type("application/rdf+xml")
	public XMLEventReader triples(@query("query") String query,
			@query("element") String element) throws Exception {
		String base = toUri().toASCIIString();
		TemplateEngine engine = tef.createTemplateEngine(getObjectConnection());
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("query", query);
		param.put("template", false);
		Template temp = engine.getTemplate(base, param).getElement(element);
		XMLEventReader doc = temp.openSourceReader();
		return new RDFXMLEventReader(new RDFaReader(base, doc, toString()));
	}

	@method("GET")
	@query("sparql")
	@type("application/sparql-query")
	public byte[] sparql(@query("about") String about,
			@query("query") String query, @query("element") String element)
			throws Exception {
		String base = toUri().toASCIIString();
		TemplateEngine engine = tef.createTemplateEngine(getObjectConnection());
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("query", query);
		param.put("template", false);
		Template temp = engine.getTemplate(base, param).getElement(element);
		return temp.getQuery().getBytes(Charset.forName("UTF-8"));
	}

	public RDFEventReader openPatternReader(String about, String query,
			String element) throws IOException, TemplateException {
		String base = toUri().toASCIIString();
		TemplateEngine engine = tef.createTemplateEngine(getObjectConnection());
		Map<String, Object> param = new HashMap<String, Object>();
		param.put("query", query);
		param.put("template", true);
		Template temp = engine.getTemplate(base, param).getElement(element);
		return temp.openQueryReader();
	}

}
