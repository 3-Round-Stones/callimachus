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

import static org.callimachusproject.util.PercentCodec.encode;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;

import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.Template;
import org.callimachusproject.engine.TemplateEngine;
import org.callimachusproject.engine.TemplateException;
import org.callimachusproject.engine.events.Ask;
import org.callimachusproject.engine.events.Group;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.events.Union;
import org.callimachusproject.engine.events.Where;
import org.callimachusproject.engine.helpers.SPARQLWriter;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.engine.model.Var;
import org.callimachusproject.engine.model.VarOrTerm;
import org.callimachusproject.form.helpers.EntityUpdater;
import org.callimachusproject.form.helpers.TripleInserter;
import org.callimachusproject.form.helpers.TripleVerifier;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.Conflict;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.helpers.RDFHandlerWrapper;
import org.openrdf.rio.helpers.StatementCollector;

/**
 * Removes and saves the provided RDF/XML triples from and into the RDF store
 * provided they match the patterns present in this template.
 * 
 * @author James Leigh
 * 
 */
public abstract class PageSupport implements CalliObject {
	private static final String CHANGE_NOTE = "http://www.w3.org/2004/02/skos/core#changeNote";
	private static final String HAS_COMPONENT = "http://callimachusproject.org/rdf/2009/framework#" + "hasComponent";

	/**
	 * Called from page.ttl
	 */
	public Template getTemplate() throws IOException, TemplateException, OpenRDFException {
		return getEngine().getTemplate(this.getResource().stringValue());
	}

	/**
	 * Called from page.ttl and query.ttl
	 */
	public Template getTemplateFor(String uri) throws IOException, TemplateException, OpenRDFException {
		assert uri != null;
		String self = this.getResource().stringValue();
		String target = TermFactory.newInstance(self).resolve(uri);
		DetachedRealm realm = getCalliRepository().getRealm(target);
		if (realm == null) {
			realm = this.getRealm();
		}
		String url = self + "?layout&realm=" + encode(realm.toString());
		return getEngine().getTemplate(url);
	}

	public RDFObject calliCreateResource(InputStream in, String type,
			String base, final RDFObject target) throws Exception {
		try {
			ObjectConnection con = target.getObjectConnection();
			if (isResourceAlreadyPresent(con, target.toString()))
				throw new Conflict("Resource already exists: " + target);
			StatementCollector statements = new StatementCollector();
			RDFHandler handler = new RDFHandlerWrapper(new RDFInserter(con), statements);
			TripleInserter tracker = new TripleInserter(handler, con);
			tracker.accept(openPatternReader(target.toString()));
			tracker.accept(created((URI) target.getResource()));
			RDFFormat format = RDFFormat.forMIMEType(type);
			RDFParserRegistry registry = RDFParserRegistry.getInstance();
			RDFParser parser = registry.get(format).getParser();
			parser.setValueFactory(con.getValueFactory());
			parser.setRDFHandler(tracker);
			parser.parse(in, base);
			if (tracker.isEmpty())
				throw new BadRequest("Missing Information");
			if (!tracker.isSingleton())
				throw new BadRequest("Wrong Subject");
			if (tracker.isDisconnectedNodePresent())
				throw new BadRequest("Blank nodes must be connected");
			if (tracker.isContainmentTriplePresent())
				throw new Conflict("ldp:contains is prohibited");
			URI created = tracker.getSubject();
			verifyCreatedStatements(created, statements.getStatements(), con);

			ObjectFactory of = con.getObjectFactory();
			for (URI partner : tracker.getPartners()) {
				if (!partner.toString().equals(base)) {
					of.createObject(partner, CalliObject.class).touchRevision();
				}
			}
			Set<URI> types = tracker.getTypes(created);
			return of.createObject(created, types);
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
	
			// delete clause uses existing triples
			update.acceptDelete(loadEditTriples(resource, con));
			update.acceptInsert(openPatternReader(resource.stringValue()));
			update.acceptInsert(changeNoteOf(resource));
			update.acceptInsert(modified(resource));
			String sparqlUpdate = update.parseUpdate(in);
	
			update.executeUpdate(sparqlUpdate, con);
	
			// insert clause uses triples that can be edited
			verifyInsertClause(sparqlUpdate, resource, con);
	
			ObjectFactory of = con.getObjectFactory();
			for (URI partner : update.getPartners()) {
				of.createObject(partner, CalliObject.class).touchRevision();
			}
			if (target instanceof CalliObject) {
				((CalliObject) target).touchRevision();
			}
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		}
	}

	private void verifyCreatedStatements(URI created,
			Collection<Statement> statements, ObjectConnection con)
			throws IOException, TemplateException, RDFParseException,
			OpenRDFException {
		Template template = getEngine()
				.getTemplate(this.getResource().stringValue());
		String variable = getFirstVariable(template);
		assert variable != null;
		MapBindingSet bindings = new MapBindingSet();
		bindings.addBinding(variable, created);
		GraphQueryResult construct = template.evaluateGraph(bindings, con);
		TripleVerifier verifier = new TripleVerifier();
		verifier.accept(construct);
		verifier.accept(created(created));
		for (Statement st : statements) {
			verifier.verify(st.getSubject(), st.getPredicate(), st.getObject());
		}
	}

	private String getFirstVariable(Template template) throws TemplateException,
			RDFParseException {
		RDFEventReader query = template.openQuery();
		try {
			while (query.hasNext()) {
				RDFEvent event = query.next();
				if (event.isTriplePattern()) {
					VarOrTerm subj = event.asTriplePattern().getSubject();
					if (subj.isVar())
						return subj.stringValue();
				}
			}
			return null;
		} finally {
			query.close();
		}
	}

	private void verifyInsertClause(String sparqlUpdate, URI resource,
			ObjectConnection con) throws RDFParseException, IOException,
			TemplateException, OpenRDFException {
		EntityUpdater postUpdate = new EntityUpdater(resource);
		postUpdate.acceptInsert(loadEditTriples(resource, con));
		postUpdate.acceptInsert(changeNoteOf(resource));
		postUpdate.acceptInsert(modified(resource));
		postUpdate.analyzeUpdate(sparqlUpdate);
	}

	private GraphQueryResult loadEditTriples(URI resource, ObjectConnection con)
			throws IOException, TemplateException, OpenRDFException,
			RDFParseException {
		Template template = getEngine().getTemplate(toString());
		MapBindingSet bindings = new MapBindingSet();
		bindings.addBinding("this", resource);
		return template.evaluateGraph(bindings, con);
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
			throws IOException, TemplateException, OpenRDFException {
		String base = toString();
		return getEngine().getTemplate(base).openQuery();
	}

	private TemplateEngine getEngine() throws OpenRDFException {
		return TemplateEngine.newInstance(this.getHttpClient());
	}

	private TriplePattern changeNoteOf(URI resource) {
		AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
		IRI subj = tf.iri(resource.stringValue());
		return new TriplePattern(subj, tf.iri(CHANGE_NOTE), tf.node());
	}

	private TriplePattern modified(URI resource) {
		AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
		IRI subj = tf.iri(resource.stringValue());
		return new TriplePattern(subj, tf.iri(DCTERMS.MODIFIED.stringValue()), tf.node());
	}

	private TriplePattern created(URI resource) {
		AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
		IRI subj = tf.iri(resource.stringValue());
		return new TriplePattern(subj, tf.iri(DCTERMS.CREATED.stringValue()), tf.node());
	}
}
