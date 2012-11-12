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

import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Set;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.Template;
import org.callimachusproject.engine.TemplateEngine;
import org.callimachusproject.engine.TemplateException;
import org.callimachusproject.engine.events.Ask;
import org.callimachusproject.engine.events.Base;
import org.callimachusproject.engine.events.Group;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.events.Union;
import org.callimachusproject.engine.events.Where;
import org.callimachusproject.engine.helpers.OverrideBaseReader;
import org.callimachusproject.engine.helpers.SPARQLWriter;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.Var;
import org.callimachusproject.engine.model.VarOrTerm;
import org.callimachusproject.form.helpers.EntityUpdater;
import org.callimachusproject.form.helpers.TripleInserter;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.Conflict;
import org.callimachusproject.traits.VersionedObject;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BooleanQuery;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.rdfxml.RDFXMLParser;

/**
 * Removes and saves the provided RDF/XML triples from and into the RDF store
 * provided they match the patterns present in this template.
 * 
 * @author James Leigh
 * 
 */
public abstract class PageSupport {
	private static final String CHANGE_NOTE = "http://www.w3.org/2004/02/skos/core#changeNote";
	private static final String HAS_COMPONENT = "http://callimachusproject.org/rdf/2009/framework#" + "hasComponent";

	private static final TemplateEngine ENGINE = TemplateEngine.newInstance();

	/**
	 * Called from page.ttl and query.ttl
	 */
	public Template getTemplate(String url) throws IOException, TemplateException {
		return ENGINE.getTemplate(url);
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
			tracker.accept(openPatternReader(target.toString()));
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
			for (URI partner : tracker.getPartners()) {
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

	public void calliEditResource(RDFObject target, InputStream in)
			throws Exception {
		try {
			ObjectConnection con = target.getObjectConnection();
			URI resource = (URI) target.getResource();
			EntityUpdater update = new EntityUpdater(resource);
			update.accept(openPatternReader(resource.stringValue()));
			update.accept(changeNoteOf(resource));
			update.executeUpdate(in, con);

			ObjectFactory of = con.getObjectFactory();
			for (URI partner : update.getPartners()) {
				of.createObject(partner, VersionedObject.class).touchRevision();
			}
			if (target instanceof VersionedObject) {
				((VersionedObject) target).touchRevision();
			}
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		}
	}

	private boolean isResourceAlreadyPresent(ObjectConnection con, String about)
			throws Exception {
		AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
		RDFEventReader reader = openPatternReader(about);
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

	private RDFEventReader openPatternReader(String about)
			throws IOException, TemplateException {
		String base = toString();
		Template temp = ENGINE.getTemplate(base);
		RDFEventReader reader = temp.openQuery();
		Base resolver = new Base(base);
		if (about == null) {
			reader = new OverrideBaseReader(resolver, null, reader);
		} else {
			String uri = resolver.resolve(about);
			reader = new OverrideBaseReader(resolver, new Base(uri), reader);
		}
		return reader;
	}

	private TriplePattern changeNoteOf(URI resource) {
		AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
		IRI subj = tf.iri(resource.stringValue());
		return new TriplePattern(subj, tf.iri(CHANGE_NOTE), tf.node());
	}
}
