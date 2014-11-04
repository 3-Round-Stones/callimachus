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

import java.io.IOException;
import java.io.InputStream;

import org.callimachusproject.auth.DetachedRealm;
import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.Template;
import org.callimachusproject.engine.TemplateEngine;
import org.callimachusproject.engine.TemplateException;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.TermFactory;
import org.callimachusproject.form.helpers.EntityUpdater;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.rio.RDFHandlerException;

/**
 * Removes and saves the provided RDF/XML triples from and into the RDF store
 * provided they match the patterns present in this template.
 * 
 * @author James Leigh
 * 
 */
public abstract class PageSupport implements CalliObject {
	private static final String CHANGE_NOTE = "http://www.w3.org/2004/02/skos/core#changeNote";

	/**
	 * Called from page.ttl query.ttl and composite.ttl
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

	public void calliEditResource(RDFObject target, InputStream in)
			throws Exception {
		try {
			ObjectConnection con = target.getObjectConnection();
			URI resource = (URI) target.getResource();
			EntityUpdater update = new EntityUpdater(resource, resource.stringValue());
	
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

	private void verifyInsertClause(String sparqlUpdate, URI resource,
			ObjectConnection con) throws RDFParseException, IOException,
			TemplateException, OpenRDFException {
		EntityUpdater postUpdate = new EntityUpdater(resource, resource.stringValue());
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
}
