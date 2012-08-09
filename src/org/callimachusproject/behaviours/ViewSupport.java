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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.tools.FileObject;
import javax.xml.stream.XMLEventReader;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.concepts.Page;
import org.callimachusproject.engine.Template;
import org.callimachusproject.engine.TemplateEngine;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.traits.VersionedObject;
import org.openrdf.annotations.Bind;
import org.openrdf.annotations.Sparql;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.RDFObject;

/**
 * Extracts parts of this template and constructs the RDF needed for this
 * template. This class is responsible for extracting data from the RDF store
 * and merging it with the RDFa template document.
 * 
 * @author James Leigh
 * @author Steve Battle
 * 
 */
public abstract class ViewSupport implements Page, RDFObject, VersionedObject,
		FileObject {
	private static final String PREFIX = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";
	private static final TemplateEngine ENGINE = TemplateEngine.newInstance();

	/**
	 * calliConstruct() is used e.g. by the view tab (not exclusively) and
	 * returns 'application/xhtml+xml'. It returns the complete XHTML page.
	 */
	@Override
	public XMLEventReader calliConstruct(Object target)
			throws Exception {
		assert target instanceof RDFObject;
		URI about = (URI) ((RDFObject) target).getResource();
		return calliConstructXhtml(about);
	}

	@Sparql(PREFIX
			+ "SELECT ?realm { ?realm calli:hasComponent* $target FILTER EXISTS {?realm a calli:Realm} }\n"
			+ "ORDER BY desc(?realm) LIMIT 1")
	protected abstract RDFObject findRealm(@Bind("target") Resource about);

	private XMLEventReader calliConstructXhtml(URI about)
			throws Exception {
		ObjectConnection con = getObjectConnection();
		String url = url("xslt", findRealm(about));
		InputStream in = openRequest(url);
		try {
			Template temp = ENGINE.getTemplate(in, url);
			MapBindingSet bindings = new MapBindingSet();
			bindings.addBinding("this", about);
			return temp.openResult(bindings, con);
		} finally {
			in.close();
		}
	}

	private String url(String operation, RDFObject realm)
			throws UnsupportedEncodingException {
		String uri = getResource().stringValue();
		StringBuilder sb = new StringBuilder();
		sb.append(uri);
		sb.append("?");
		sb.append(URLEncoder.encode(operation, "UTF-8"));
		if (realm != null) {
			sb.append("&realm=");
			sb.append(URLEncoder.encode(realm.toString(), "UTF-8"));
		}
		return sb.toString();
	}

	private InputStream openRequest(String url) throws IOException {
		HTTPObjectClient client = HTTPObjectClient.getInstance();
		HttpRequest request = new BasicHttpRequest("GET", url);
		HttpResponse response = client.service(request);
		if (response.getStatusLine().getStatusCode() >= 300)
			throw ResponseException.create(response, url);
		return response.getEntity().getContent();
	}

}
