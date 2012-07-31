/*
   Portions Copyright (c) 2009-10 Zepheira LLC, Some Rights Reserved
   Portions Copyright (c) 2010-11 Talis Inc, Some Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */
package org.callimachusproject.behaviours;

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Set;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;
import org.callimachusproject.concepts.Page;
import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.events.Ask;
import org.callimachusproject.engine.events.Group;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.events.Union;
import org.callimachusproject.engine.events.Where;
import org.callimachusproject.engine.helpers.SPARQLWriter;
import org.callimachusproject.engine.helpers.TemplateReader;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.Var;
import org.callimachusproject.engine.model.VarOrTerm;
import org.callimachusproject.form.helpers.TripleInserter;
import org.callimachusproject.server.client.HTTPObjectClient;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.Conflict;
import org.callimachusproject.server.exceptions.ResponseException;
import org.callimachusproject.traits.VersionedObject;
import org.callimachusproject.xml.XMLEventReaderFactory;
import org.openrdf.annotations.Bind;
import org.openrdf.annotations.Sparql;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BooleanQuery;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.rdfxml.RDFXMLParser;

/**
 * Save the provided RDF/XML triples into the RDF store provided they match the
 * patterns present in this template.
 * 
 * @author James Leigh
 * 
 */
public abstract class CreateSupport implements Page, RDFObject {
	private static final String PREFIX = "PREFIX calli:<http://callimachusproject.org/rdf/2009/framework#>\n";
	private static final String HAS_COMPONENT = "http://callimachusproject.org/rdf/2009/framework#" + "hasComponent";

	/**
	 * calliConstructTemplate() is used e.g. by the create operation. It returns
	 * the complete XHTML page.
	 */
	@Override
	public XMLEventReader calliConstructTemplate(Object target)
			throws Exception {
		assert target instanceof RDFObject;
		Resource about = ((RDFObject) target).getResource();
		return new TemplateReader(xslt(findRealm(about)));
	}

	public RDFObject calliCreateResource(InputStream in, String base,
			final RDFObject target) throws Exception {
		try {
			ObjectConnection con = target.getObjectConnection();
			if (target.toString().equals(base))
				throw new RDFHandlerException("Target resource URI not provided");
			if (isResourceAlreadyPresent(con, target.toString()))
				throw new Conflict("Resource already exists: " + target);
			TripleInserter tracker = new TripleInserter(con);
			tracker.accept(openPatternReader(target.toString(), null));
			RDFXMLParser parser = new RDFXMLParser();
			parser.setValueFactory(con.getValueFactory());
			parser.setRDFHandler(tracker);
			parser.parse(in, base);
			if (tracker.isEmpty())
				throw new BadRequest("Missing Information");
			if (!tracker.isSingleton())
				throw new BadRequest("Wrong Subject");
			if (tracker.isDisconnectedNodePresent())
				throw new BadRequest("Blank nodes must be connected");
			ObjectFactory of = con.getObjectFactory();
			for (URI partner : tracker.getResources()) {
				if (!partner.toString().equals(base)) {
					of.createObject(partner, VersionedObject.class).touchRevision();
				}
			}
			Set<URI> types = tracker.getTypes(tracker.getSubject());
			return of.createObject(tracker.getSubject(), types);
		} catch (URISyntaxException  e) {
			throw new BadRequest(e);
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		} finally {
			in.close();
		}
	}

	@Sparql(PREFIX
			+ "SELECT ?realm { ?realm calli:hasComponent* $target FILTER EXISTS {?realm a calli:Realm} }\n"
			+ "ORDER BY desc(?realm) LIMIT 1")
	protected abstract RDFObject findRealm(@Bind("target") Resource about);

	private XMLEventReader xslt(RDFObject realm) throws IOException,
			XMLStreamException {
		XMLEventReaderFactory factory = XMLEventReaderFactory.newInstance();
		String url = url("xslt", realm);
		InputStream in = openRequest(url);
		return factory.createXMLEventReader(url, in);
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

	private boolean isResourceAlreadyPresent(ObjectConnection con, String about)
			throws Exception {
		AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
		RDFEventReader reader = openPatternReader(about, null);
		try {
			boolean first = true;
			StringWriter str = new StringWriter();
			SPARQLWriter writer = new SPARQLWriter(str);
			while (reader.hasNext()) {
				RDFEvent next = reader.next();
				if (next.isStartDocument() || next.isBase()
						|| next.isNamespace()) {
					writer.write(next);
				} else if (first) {
					first = false;
					writer.write(new Ask(next.getLocation()));
					writer.write(new Where(true, next.getLocation()));
					writer.write(new Group(true, next.getLocation()));
					IRI has = tf.iri(HAS_COMPONENT);
					Var var = tf.var("calliHasComponent");
					writer.write(new TriplePattern(var, has, tf.var("this"), next.getLocation()));
					writer.write(new Group(false, next.getLocation()));
				}
				if (next.isTriplePattern()) {
					VarOrTerm subj = next.asTriplePattern().getSubject();
					if (subj.isIRI() && subj.stringValue().equals(about)
							|| subj.isVar()
							&& subj.stringValue().equals("this")) {
						writer.write(new Union(next.getLocation()));
						writer.write(new Group(true, next.getLocation()));
						writer.write(next);
						writer.write(new Group(false, next.getLocation()));
					}
				} else if (next.isEndDocument()) {
					writer.write(new Where(false, next.getLocation()));
					writer.write(next);
				}
			}
			writer.close();
			String qry = str.toString();
			ValueFactory vf = con.getValueFactory();
			BooleanQuery query = con.prepareBooleanQuery(SPARQL, qry, this.toString());
			query.setBinding("this", vf.createURI(about));
			return query.evaluate();
		} finally {
			reader.close();
		}
	}
}
