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
import java.util.Map;

import javax.tools.FileObject;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.concepts.Page;
import org.callimachusproject.stream.RDFaProducer;
import org.callimachusproject.stream.SPARQLProducer;
import org.openrdf.http.object.client.HTTPObjectClient;
import org.openrdf.http.object.exceptions.ResponseException;
import org.openrdf.http.object.traits.VersionedObject;
import org.openrdf.http.object.util.ChannelUtil;
import org.openrdf.model.URI;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.object.xslt.XMLEventReaderFactory;

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

	/**
	 * calliConstruct() is used e.g. by the view tab (not exclusively) and
	 * returns 'application/xhtml+xml'. It returns the complete XHTML page.
	 */
	@Override
	public XMLEventReader calliConstruct(Object target, String query) throws Exception {
		URI about = null;
		assert(target instanceof RDFObject);
		about = (URI) ((RDFObject) target).getResource();
		return calliConstructXhtml(about, query);
	}

	private XMLEventReader calliConstructXhtml(URI about, String query) 
	throws Exception {
		ObjectConnection con = getObjectConnection();
		TupleQueryResult results;
		Map<String,String> origins;
		assert(about!=null);
		// evaluate SPARQL derived from the template
		String base = about.stringValue();
		String sparql = sparql(query);
		TupleQuery q = con.prepareTupleQuery(SPARQL, sparql, base);
		q.setBinding("this", about);
		results = q.evaluate();
		origins = SPARQLProducer.getOrigins(sparql);
		
		return new RDFaProducer(xslt(query), results, origins, about, con);
	}

	private String sparql(String query) throws IOException {
		InputStream in = request("sparql", query);
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ChannelUtil.transfer(in, out);
			return new String(out.toByteArray());
		} finally {
			in.close();
		}
	}

	private XMLEventReader xslt(String query)
			throws IOException, XMLStreamException {
		XMLEventReaderFactory factory = XMLEventReaderFactory.newInstance();
		InputStream in = request("xslt", query);
		return factory.createXMLEventReader(in);
	}

	private InputStream request(String operation, String query)
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
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpRequest request = new BasicHttpRequest("GET", sb.toString());
		HttpResponse response = client.service(request);
		if (response.getStatusLine().getStatusCode() >= 300)
			throw ResponseException.create(response);
		InputStream in = response.getEntity().getContent();
		return in;
	}

}
