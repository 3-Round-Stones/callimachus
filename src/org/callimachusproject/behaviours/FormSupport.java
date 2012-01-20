/*
 * Portions Copyright (c) 2009-10 Zepheira LLC and James Leigh, Some
  Rights Reserved
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

import static org.callimachusproject.engine.helpers.SPARQLWriter.toSPARQL;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.annotations.header;
import org.callimachusproject.annotations.method;
import org.callimachusproject.annotations.query;
import org.callimachusproject.annotations.type;
import org.callimachusproject.concepts.Page;
import org.callimachusproject.engine.RDFaReader;
import org.callimachusproject.engine.Template;
import org.callimachusproject.engine.TemplateEngine;
import org.callimachusproject.engine.TemplateEngineFactory;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.helpers.BufferedXMLEventReader;
import org.callimachusproject.engine.helpers.DeDupedResultSet;
import org.callimachusproject.engine.helpers.OrderedSparqlReader;
import org.callimachusproject.engine.helpers.RDFaProducer;
import org.callimachusproject.engine.helpers.SPARQLPosteditor;
import org.callimachusproject.engine.helpers.SPARQLProducer;
import org.callimachusproject.engine.helpers.TemplateReader;
import org.callimachusproject.engine.model.VarOrTerm;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.ResponseException;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.xslt.XMLEventReaderFactory;
import org.openrdf.repository.object.xslt.XSLTransformer;

/**
 * Implements the construct search method to lookup resources by label prefix
 * and options method to list all possible values.
 * 
 * @author James Leigh 
 * @author Steve Battle
 * 
 */
public abstract class FormSupport implements Page, RDFObject {
	private static final TemplateEngineFactory tef = TemplateEngineFactory.newInstance();
	private static ValueFactory vf = new ValueFactoryImpl();
	
	
	static final XSLTransformer HTML_XSLT;
	static {
		String path = "org/callimachusproject/xsl/xhtml-to-html.xsl";
		ClassLoader cl = ViewSupport.class.getClassLoader();
		String url = cl.getResource(path).toExternalForm();
		InputStream input = cl.getResourceAsStream(path);
		InputStreamReader reader = new InputStreamReader(input);
		HTML_XSLT = new XSLTransformer(reader, url);
	}

	/**
	 * Extracts an element from the template (without variables).
	 */
	@method("GET")
	@query("template")
	@type("text/html")
	public String template(@query("query") String query,
			@query("element") String element) throws Exception {
		return asHtmlString(removeNestedResources(xslt(query, element)));
	}

	@Override
	public String calliConstructHTML(Object target) throws Exception {
		return calliConstructHTML(target, null);
	}
	
	@Override
	public String calliConstructHTML(Object target, String query)
			throws Exception {
		return asHtmlString(calliConstruct(target, query));
	}

	/**
	 * Extracts an element from the template (without variables) and populates
	 * the element with the properties of the about resource.
	 */
	@method("GET")
	@query("construct")
	@type("text/html")
	@header("cache-control:no-store")
	public InputStream calliConstruct(@query("about") URI about,
			@query("query") String query, @query("element") String element)
			throws Exception {
		if (about != null && (element == null || element.equals("/1")))
			throw new BadRequest("Missing element parameter");
		if (about!=null && element!=null)
			return dataConstruct(about, query, element);
		if (about == null && query == null && element == null) {
			ValueFactory vf = getObjectConnection().getValueFactory();
			about = vf.createURI(this.toString());
		}
		XMLEventReader xhtml = calliConstructXhtml(about, query, element);
		return HTML_XSLT.transform(xhtml, this.toString()).asInputStream();
	}

	/**
	 * See data-options, defining an HTML select/option fragment
	 */

	@method("GET")
	@query("options")
	@type("text/html")
	@header("cache-control:no-store")
	
	public InputStream options
	(@query("query") String query, @query("element") String element) throws Exception {
		String base = getResource().stringValue();
		BufferedXMLEventReader template = new BufferedXMLEventReader(xslt(query, element));
		template.mark();
		SPARQLProducer rq = new SPARQLProducer(new RDFaReader(base, template, toString()));
		SPARQLPosteditor ed = new SPARQLPosteditor(rq);
		
		// only pass object vars (excluding prop-exps and content) beyond a certain depth: 
		// ^(/\d+){3,}$|^(/\d+)*\s.*$
		ed.addEditor(ed.new TriplePatternCutter(null,"^(/\\d+){3,}$|^(/\\d+)*\\s.*$"));
		
		RepositoryConnection con = getObjectConnection();
		String sparql = toSPARQL(new OrderedSparqlReader(ed)) + "\nLIMIT 1000";
		TupleQuery qry = con.prepareTupleQuery(SPARQL, sparql, base);
		URI about = vf.createURI(base);
		template.reset(0);
		MapBindingSet bindings = new MapBindingSet();
		bindings.addBinding("this", about);
		RDFaProducer xhtml = new RDFaProducer(template, qry.evaluate(), rq.getOrigins(), bindings, con);
		return HTML_XSLT.transform(xhtml, this.toString()).asInputStream();
	}

	/**
	 * Returns an HTML page listing suggested resources for the given element.
	 * See data-search
	 */
	@method("GET")
	@query("search")
	@type("text/html")
	@header("cache-control:no-validate,max-age=60")
	
	public InputStream constructSearch
	(@query("query") String query, @query("element") String element, @query("q") String q)
	throws Exception {
		String base = getResource().stringValue();
		BufferedXMLEventReader template = new BufferedXMLEventReader(xslt(query, element));
		template.mark();
		SPARQLProducer rq = new SPARQLProducer(new RDFaReader(base, template, toString()));
		SPARQLPosteditor ed = new SPARQLPosteditor(rq);
		
		// filter out the outer rel (the button may add additional bnodes that need to be cut out)
		ed.addEditor(ed.new TriplePatternCutter(null,"^(/\\d+){3,}$|^(/\\d+)*\\s.*$"));
		
		// add soundex conditions to @about siblings on the next level only
		// The regex should not match properties and property-expressions with info following the xptr
		ed.addEditor(ed.new PhoneMatchInsert("^(/\\d+){2}$", q));
		
		// add filters to soundex labels at the next level (only direct properties of the top-level subject)
		//ed.addEditor(ed.new FilterInsert("^(/\\d+){2}$",LABELS,regexStartsWith(q)));
		ed.addEditor(ed.new FilterKeywordExists("^(/\\d+){2}$", q));
	
		RepositoryConnection con = getObjectConnection();
		String sparql = toSPARQL(new OrderedSparqlReader(ed));
		TupleQuery qry = con.prepareTupleQuery(SPARQL, sparql, base);
		// The edited query may return multiple and/or empty solutions
		TupleQueryResult results = new DeDupedResultSet(qry.evaluate(),true);
		URI about = vf.createURI(base);
		template.reset(0);
		MapBindingSet bindings = new MapBindingSet();
		bindings.addBinding("this", about);
		RDFaProducer xhtml = new RDFaProducer(template, results, rq.getOrigins(), bindings, con);
		return HTML_XSLT.transform(xhtml, this.toString()).asInputStream();
	}

	private XMLEventReader removeNestedResources(final XMLEventReader xslt) {
		return new TemplateReader(xslt);
	}

	private String asHtmlString(XMLEventReader xhtml) throws Exception {
		return HTML_XSLT.transform(xhtml, this.toString()).asString();
	}
	
	private XMLEventReader calliConstructXhtml(URI about, String query, String element) 
	throws Exception {
		TemplateEngine engine = tef.createTemplateEngine(getObjectConnection());
		InputStream in = openRequest("xslt", query, element);
		try {
			Template temp = engine.getTemplate(in, toString());
			MapBindingSet bindings = new MapBindingSet();
			bindings.addBinding("this", about);
			return temp.openResultReader(temp.getQuery(), bindings);
		} finally {
			in.close();
		}
	}
	
	private InputStream dataConstruct(URI about, String query, String element) throws Exception {
		String base = getResource().stringValue();
		BufferedXMLEventReader template = new BufferedXMLEventReader(xslt(query, element));
		template.mark();
		SPARQLProducer rq = new SPARQLProducer(new RDFaReader(base, template, toString()));
		SPARQLPosteditor ed = new SPARQLPosteditor(rq);
		
		// only pass object vars (excluding prop-exps and content) beyond a certain depth: 
		// ^(/\d+){3,}$|^(/\d+)*\s.*$
		ed.addEditor(ed.new TriplePatternCutter(null,"^(/\\d+){3,}$|^(/\\d+)*\\s.*$"));
		
		// find top-level new subjects to bind
		SPARQLPosteditor.TriplePatternRecorder rec;
		ed.addEditor(rec = ed.new TriplePatternRecorder("^(/\\d+){2}$",null,null));
		
		RepositoryConnection con = getObjectConnection();
		TupleQuery qry = con.prepareTupleQuery(SPARQL, toSPARQL(new OrderedSparqlReader(ed)), base);
		for (TriplePattern t: rec.getTriplePatterns()) {
			VarOrTerm vt = t.getSubject();
			if (vt.isVar())
				qry.setBinding(vt.asVar().stringValue(), about);
		}
		template.reset(0);
		MapBindingSet bindings = new MapBindingSet();
		bindings.addBinding("this", about);
		RDFaProducer xhtml = new RDFaProducer(template, qry.evaluate(), rq.getOrigins(), bindings, con);
		return HTML_XSLT.transform(xhtml, this.toString()).asInputStream();		
	}

	protected XMLEventReader xslt(String query, String element)
			throws IOException, XMLStreamException {
		XMLEventReaderFactory factory = XMLEventReaderFactory.newInstance();
		InputStream in = openRequest("xslt", query, element);
		return factory.createXMLEventReader(in);
	}

	private InputStream openRequest(String operation, String query, String element)
			throws IOException {
		String uri = getResource().stringValue();
		StringBuilder sb = new StringBuilder();
		sb.append(uri);
		sb.append("?");
		sb.append(URLEncoder.encode(operation, "UTF-8"));
		if (query != null) {
			sb.append("&query=");
			sb.append(URLEncoder.encode(query, "UTF-8"));
		}
		if (element != null) {
			sb.append("&element=");
			sb.append(URLEncoder.encode(element, "UTF-8"));
		}
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpRequest request = new BasicHttpRequest("GET", sb.toString());
		HttpResponse response = client.service(request);
		if (response.getStatusLine().getStatusCode() >= 300)
			throw ResponseException.create(response);
		InputStream in = response.getEntity().getContent();
		return in;
	}

}
