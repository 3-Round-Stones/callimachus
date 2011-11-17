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

import java.io.InputStream;

import org.callimachusproject.concepts.Page;
import org.callimachusproject.helpers.GraphPatternBuilder;
import org.callimachusproject.helpers.SubjectTracker;
import org.callimachusproject.io.MultipartParser;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.TermFactory;
import org.openrdf.http.object.exceptions.BadRequest;
import org.openrdf.http.object.traits.VersionedObject;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.rdfxml.RDFXMLParser;

/**
 * Removes and saves the provided RDF/XML triples from and into the RDF store
 * provided they match the patterns present in this template.
 * 
 * @author James Leigh
 * 
 */
public abstract class EditSupport implements Page {

	private static class Remover extends RDFHandlerBase {
		private final ObjectConnection con;

		private Remover(ObjectConnection con) {
			this.con = con;
		}

		public void handleStatement(Statement st) throws RDFHandlerException {
			try {
				con.remove(st.getSubject(), st.getPredicate(), st.getObject());
			} catch (RepositoryException e) {
				throw new RDFHandlerException(e);
			}
		}
	}

	private static final String CHANGE_NOTE = "http://www.w3.org/2004/02/skos/core#changeNote";

	public void calliEditResource(RDFObject target, InputStream in)
			throws Exception {
		MultipartParser multipart = new MultipartParser(in);
		try {
			RDFXMLParser parser = new RDFXMLParser();
			ObjectConnection con = target.getObjectConnection();
			remove((URI) target.getResource(), multipart.next(), parser, con);
			add((URI) target.getResource(), multipart.next(), parser, con);
			if (target instanceof VersionedObject) {
				((VersionedObject) target).touchRevision();
			}
			assert multipart.next() == null;
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		} finally {
			multipart.close();
		}
	}

	private void remove(URI resource, InputStream in, RDFXMLParser parser,
			ObjectConnection con) throws Exception {
		SubjectTracker remover = createSubjectTracker(resource,
				new Remover(con), con);
		remover.addSubject(resource);
		GraphPatternBuilder pattern = new GraphPatternBuilder();
		parser.setRDFHandler(pattern);
		parser.parse(in, resource.stringValue());
		if (!pattern.isEmpty()) {
			String sparql = pattern.toSPARQLQuery();
			con.prepareGraphQuery(SPARQL, sparql).evaluate(remover);
			if (remover.isEmpty())
				throw new BadRequest("Removed Content Not Found");
			if (!remover.isAbout(resource))
				throw new BadRequest("Wrong Subject");
			if (!remover.getTypes(resource).isEmpty())
				throw new BadRequest("Cannot change resource type");
		}
		ObjectFactory of = con.getObjectFactory();
		for (URI partner : remover.getResources()) {
			of.createObject(partner, VersionedObject.class).touchRevision();
		}
	}

	private void add(URI resource, InputStream in, RDFXMLParser parser,
			ObjectConnection con) throws Exception {
		SubjectTracker inserter = createSubjectTracker(resource,
				new RDFInserter(con), con);
		inserter.addSubject(resource);
		inserter.accept(changeNoteOf(resource));
		parser.setValueFactory(con.getValueFactory());
		parser.setRDFHandler(inserter);
		parser.parse(in, resource.stringValue());
		if (!inserter.isEmpty() && !inserter.isAbout(resource))
			throw new BadRequest("Wrong Subject");
		if (!inserter.getTypes(resource).isEmpty())
			throw new BadRequest("Cannot change resource type");
		ObjectFactory of = con.getObjectFactory();
		for (URI partner : inserter.getResources()) {
			of.createObject(partner, VersionedObject.class).touchRevision();
		}
	}

	private TriplePattern changeNoteOf(URI resource) {
		TermFactory tf = TermFactory.newInstance();
		IRI subj = tf.iri(resource.stringValue());
		return new TriplePattern(subj, tf.iri(CHANGE_NOTE), tf.node());
	}

	private SubjectTracker createSubjectTracker(URI resource,
			RDFHandler delegate, ObjectConnection con) throws Exception {
		ValueFactory vf = con.getValueFactory();
		SubjectTracker tracker = new SubjectTracker(delegate, vf);
		tracker.setReverseAllowed(false);
		tracker.setWildPropertiesAllowed(false);
		String about = resource.stringValue();
		tracker.accept(openPatternReader(about, "edit", null));
		return tracker;
	}
}
