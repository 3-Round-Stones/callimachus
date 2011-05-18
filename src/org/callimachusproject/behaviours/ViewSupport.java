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

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;

import javax.tools.FileObject;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.concepts.Page;
import org.callimachusproject.stream.RDFaProducer;
import org.openrdf.http.object.annotations.header;
import org.openrdf.http.object.annotations.query;
import org.openrdf.http.object.annotations.type;
import org.openrdf.http.object.client.HTTPObjectClient;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.traits.VersionedObject;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.TupleQueryResultImpl;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.xslt.XMLEventReaderFactory;
import org.openrdf.repository.object.xslt.XSLTransformer;

/**
 * Extracts parts of this template and constructs the RDF needed for this
 * template. This class is responsible for extracting data from the RDF store
 * and merging it with the RDFa template document.
 * 
 * @author James Leigh
 * @author Steve Battle
 * 
 */
public abstract class ViewSupport implements Page, RDFObject, VersionedObject, FileObject {

	private static final XSLTransformer HTML_XSLT;
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
	 * TODO strip out RDFa variables and expressions
	 */
	@query("template")
	@type("text/html")
	public String template(@query("query") String query,
			@query("element") String element) throws Exception {
		String html = asHtmlString(xslt(query, element));
		html = html.replaceAll("\\{[^\\}<>]*\\}", "");
		html = html.replaceAll("\\b(content|resource|about)(=[\"'])\\?\\w+([\"'])", "$1$2$3");
		return html;
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
	 * calliConstruct() is used e.g. by the view tab (not exclusively) and
	 * returns 'application/xhtml+xml'. It returns the complete XHTML page.
	 */
	@Override
	public XMLEventReader calliConstruct(Object target, String query) throws Exception {
		URI about = null;
		if (target instanceof RDFObject) {
			about = (URI) ((RDFObject) target).getResource();
		}
		return calliConstructXhtml(about, query, null);
	
	}

	/**
	 * Extracts an element from the template (without variables) and populates
	 * the element with the properties of the about resource.
	 */
	@query("construct")
	@type("text/html")
	@header("cache-control:no-store")
	public InputStream calliConstruct(@query("about") URI about,
			@query("query") String query, @query("element") String element)
			throws Exception {
		if (about != null && (element == null || element.equals("/1")))
			throw new BadRequest("Missing element parameter");
		if (about == null && query == null && element == null) {
			ValueFactory vf = getObjectConnection().getValueFactory();
			about = vf.createURI(this.toString());
		}
		XMLEventReader xhtml = calliConstructXhtml(about, query, element);
		return HTML_XSLT.transform(xhtml, this.toString()).asInputStream();
	}

	private String asHtmlString(XMLEventReader xhtml) throws Exception {
		return HTML_XSLT.transform(xhtml, this.toString()).asString();
	}

	private XMLEventReader calliConstructXhtml(URI about, String query,
			String element) throws Exception {
		ObjectConnection con = getObjectConnection();
		// Generate SPARQL from the template and evaluate
		TupleQueryResult results = evaluate(about, query, element);
		return new RDFaProducer(xslt(query, element), results, about, con);
	}

	private TupleQueryResult evaluate(URI about, String query, String element)
			throws Exception {
		if (about == null) {
			List<String> names = Collections.emptyList();
			List<BindingSet> results = Collections.emptyList();
			return new TupleQueryResultImpl(names, results.iterator());
		}
		String base = about.stringValue();
		String qry = sparql(query, element);
		ObjectConnection con = getObjectConnection();
		TupleQuery q = con.prepareTupleQuery(SPARQL, qry, base);
		q.setBinding("this", about);
		return q.evaluate();
	}

	private String sparql(String query, String element) throws IOException {
		InputStream in = request("sparql", query, element);
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChannelUtil.transfer(in, out);
			return new String(out.toByteArray());
		} finally {
			in.close();
		}
	}

	private XMLEventReader xslt(String query, String element)
			throws IOException, XMLStreamException {
		XMLEventReaderFactory factory = XMLEventReaderFactory.newInstance();
		InputStream in = request("xslt", query, element);
		return factory.createXMLEventReader(in);
	}

	private InputStream request(String operation, String query, String element)
			throws IOException {
		String uri = getResource().stringValue();
		StringBuilder sb = new StringBuilder();
		sb.append(uri);
		sb.append("?");
		sb.append(operation);
		if (query != null) {
			sb.append("&query=");
			sb.append(query);
		}
		if (element != null) {
			sb.append("&element=");
			sb.append(element);
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
