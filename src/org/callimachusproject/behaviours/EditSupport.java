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
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import org.callimachusproject.concepts.Page;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.form.helpers.GraphPatternBuilder;
import org.callimachusproject.form.helpers.StatementExtractor;
import org.callimachusproject.form.helpers.SubjectTracker;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.traits.VersionedObject;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.algebra.Modify;
import org.openrdf.query.algebra.TupleExpr;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.util.RDFInserter;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerBase;

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
		try {
			String input = readString(in);
			SPARQLParser parser = new SPARQLParser();
			ParsedUpdate parsed = parser.parseUpdate(input, target.toString());
			if (parsed.getUpdateExprs().isEmpty())
				throw new BadRequest("No input");
			if (parsed.getUpdateExprs().size() > 1)
				throw new BadRequest("Multiple update statements");
			UpdateExpr updateExpr = parsed.getUpdateExprs().get(0);
			if (!(updateExpr instanceof Modify))
				throw new BadRequest("Not a DELETE/INSERT statement");
			Modify modify = (Modify) updateExpr;
			ObjectConnection con = target.getObjectConnection();
			ValueFactory vf = con.getValueFactory();
			modify.getWhereExpr().visit(
					new StatementExtractor(new RDFHandlerBase() {
						public void handleStatement(Statement st)
								throws RDFHandlerException {
							throw new RDFHandlerException(
									"Where clause must be empty");
						}
					}, vf));
			remove((URI) target.getResource(), modify.getDeleteExpr(), con);
			add((URI) target.getResource(), modify.getInsertExpr(), con);
			if (target instanceof VersionedObject) {
				((VersionedObject) target).touchRevision();
			}
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		}
	}

	private String readString(InputStream in) throws IOException {
		try {
			Reader reader = new InputStreamReader(in, "UTF-8");
			StringWriter writer = new StringWriter(8192);
			int read;
			char[] cbuf = new char[1024];
			while ((read = reader.read(cbuf)) >= 0) {
				writer.write(cbuf, 0, read);
				if (writer.getBuffer().length() > 1048576)
					throw new IOException("Input Stream is too big");
			}
			reader.close();
			return writer.toString();
		} finally {
			in.close();
		}
	}

	private void remove(URI resource, TupleExpr delete, ObjectConnection con)
			throws Exception {
		ValueFactory vf = con.getValueFactory();
		SubjectTracker remover = createSubjectTracker(resource,
				new Remover(con), con);
		remover.addSubject(resource);
		GraphPatternBuilder pattern = new GraphPatternBuilder();
		pattern.startRDF();
		delete.visit(new StatementExtractor(pattern, vf));
		pattern.endRDF();
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

	private void add(URI resource, TupleExpr insert, ObjectConnection con)
			throws Exception {
		ValueFactory vf = con.getValueFactory();
		SubjectTracker inserter = createSubjectTracker(resource,
				new RDFInserter(con), con);
		inserter.addSubject(resource);
		inserter.accept(changeNoteOf(resource));
		inserter.startRDF();
		insert.visit(new StatementExtractor(inserter, vf));
		inserter.endRDF();
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
		AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
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
