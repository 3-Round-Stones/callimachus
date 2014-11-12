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
package org.callimachusproject.behaviours;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

import org.callimachusproject.auth.AuthorizationManager;
import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.Template;
import org.callimachusproject.engine.TemplateException;
import org.callimachusproject.engine.events.RDFEvent;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.VarOrTerm;
import org.callimachusproject.form.helpers.TripleInserter;
import org.callimachusproject.form.helpers.TripleVerifier;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.Conflict;
import org.callimachusproject.server.exceptions.InternalServerError;
import org.callimachusproject.traits.CalliObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.vocabulary.DCTERMS;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.impl.MapBindingSet;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerWrapper;

public abstract class CompositeSupport implements CalliObject {

	public RDFObject insertComponentGraph(InputStream in, String type, String uri,
			RDFHandler collector) throws OpenRDFException, IOException {
		try {
			ObjectConnection con = this.getObjectConnection();
			RDFHandler handler = new RDFInserter(con);
			if (collector != null) {
				handler = new RDFHandlerWrapper(handler, collector);
			}
			TripleInserter tracker = new TripleInserter(handler, con);
			tracker.parseAndInsert(in, type, uri);
			if (tracker.isEmpty())
				throw new BadRequest("Missing Information");
			if (!tracker.isSingleton())
				throw new BadRequest("Wrong Subject");
			if (tracker.isDisconnectedNodePresent())
				throw new BadRequest("Blank nodes must be connected");
			if (tracker.isContainmentTriplePresent())
				throw new Conflict("ldp:contains is prohibited");
			URI created = tracker.getPrimaryTopic();

			ObjectFactory of = con.getObjectFactory();
			for (URI partner : tracker.getPartners()) {
				if (!partner.equals(created)) {
					of.createObject(partner, CalliObject.class).touchRevision();
				}
			}
			Set<URI> types = tracker.getTypes(created);
			return of.createObject(created, types);
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		} finally {
			in.close();
		}
	}

	public boolean isAuthorized(String user, RDFObject target, String[] roles)
			throws RepositoryException, OpenRDFException {
		AuthorizationManager manager = getCalliRepository().getAuthorizationManager();
		return manager.isAuthorized(user, target, roles);
	}

	public void calliVerifyCreatedResource(RDFObject target,
			Collection<Template> templates, Collection<Statement> statements,
			String documentUri) throws RDFParseException, TemplateException,
			OpenRDFException, IOException {
		try {
			URI created = (URI) target.getResource();
			TripleInserter tracker = new TripleInserter();
			tracker.setBaseURI(documentUri);
			for (Template template : templates) {
				tracker.accept(template.openQuery());
			}
			tracker.accept(created(created));
			tracker.startRDF();
			for (Statement st : statements) {
				tracker.handleStatement(st);
			}
			tracker.endRDF();
			if (tracker.isEmpty())
				throw new BadRequest("Missing Information");
			if (!tracker.isSingleton() || !created.equals(tracker.getPrimaryTopic()))
				throw new BadRequest("Wrong Resource");
			if (tracker.isDisconnectedNodePresent())
				throw new BadRequest("Blank nodes must be connected");
			if (tracker.isContainmentTriplePresent())
				throw new Conflict("ldp:contains is prohibited");
			ObjectConnection con = target.getObjectConnection();
			TripleVerifier verifier = new TripleVerifier();
			for (Template template : templates) {
				String variable = getFirstVariable(template);
				if (variable != null) {
					MapBindingSet bindings = new MapBindingSet();
					bindings.addBinding(variable, created);
					GraphQueryResult construct = template.evaluateGraph(bindings, con);
					verifier.accept(construct);
				}
			}
			verifier.accept(created(created));
			for (Statement st : statements) {
				verifier.verify(st.getSubject(), st.getPredicate(), st.getObject());
			}
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
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

	private TriplePattern created(URI resource) {
		AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
		IRI subj = tf.iri(resource.stringValue());
		return new TriplePattern(subj, tf.iri(DCTERMS.CREATED.stringValue()), tf.node());
	}

}
