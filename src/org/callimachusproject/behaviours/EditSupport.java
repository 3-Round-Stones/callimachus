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

import org.callimachusproject.concepts.Page;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.TemplateException;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.model.AbsoluteTermFactory;
import org.callimachusproject.engine.model.IRI;
import org.callimachusproject.form.helpers.GraphPatternBuilder;
import org.callimachusproject.form.helpers.StatementExtractor;
import org.callimachusproject.form.helpers.TripleAnalyzer;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.traits.VersionedObject;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.algebra.Modify;
import org.openrdf.query.algebra.UpdateExpr;
import org.openrdf.query.parser.ParsedUpdate;
import org.openrdf.query.parser.sparql.SPARQLParser;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectFactory;
import org.openrdf.repository.object.RDFObject;
import org.openrdf.repository.util.RDFInserter;
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
			ObjectConnection con = target.getObjectConnection();
			TripleAnalyzer analyzer = new TripleAnalyzer();
			String input = parseUpdate(in, target, analyzer);

			executeUpdate(input, target.toString(), con);

			ObjectFactory of = con.getObjectFactory();
			for (URI partner : analyzer.getResources()) {
				of.createObject(partner, VersionedObject.class).touchRevision();
			}
			if (target instanceof VersionedObject) {
				((VersionedObject) target).touchRevision();
			}
		} catch (RDFHandlerException e) {
			throw new BadRequest(e);
		}
	}

	private void executeUpdate(String input, String base, ObjectConnection con)
			throws OpenRDFException {
		SPARQLParser parser = new SPARQLParser();
		ParsedUpdate parsed = parser.parseUpdate(input, base);
		if (parsed.getUpdateExprs().isEmpty())
			throw new BadRequest("No input");
		if (parsed.getUpdateExprs().size() > 1)
			throw new BadRequest("Multiple update statements");
		UpdateExpr updateExpr = parsed.getUpdateExprs().get(0);
		if (!(updateExpr instanceof Modify))
			throw new BadRequest("Not a DELETE/INSERT statement");
		Modify modify = (Modify) updateExpr;
		ValueFactory vf = con.getValueFactory();
		modify.getWhereExpr().visit(
				new StatementExtractor(new RDFHandlerBase() {
					public void handleStatement(Statement st)
							throws RDFHandlerException {
						throw new RDFHandlerException(
								"Where clause must be empty");
					}
				}, vf));
		Remover remover = new Remover(con);
		GraphPatternBuilder pattern = new GraphPatternBuilder();
		pattern.startRDF();
		modify.getDeleteExpr().visit(new StatementExtractor(pattern, vf));
		pattern.endRDF();
		if (!pattern.isEmpty()) {
			String sparql = pattern.toSPARQLQuery();
			con.prepareGraphQuery(SPARQL, sparql).evaluate(remover);
		}
		RDFInserter inserter = new RDFInserter(con);
		inserter.startRDF();
		modify.getInsertExpr().visit(new StatementExtractor(inserter, vf));
		inserter.endRDF();
	}

	private String parseUpdate(InputStream in, RDFObject target,
			TripleAnalyzer analyzer) throws RDFParseException, IOException,
			TemplateException, RDFHandlerException, MalformedQueryException {
		URI resource = (URI) target.getResource();
		analyzer.accept(openPatternReader(resource.stringValue(), null));
		analyzer.addSubject(resource);
		analyzer.accept(changeNoteOf(resource));
		String input = analyzer.parseUpdate(in, target.toString());
		if (!analyzer.isAbout(resource))
			throw new BadRequest("Wrong Subject");
		if (!analyzer.getTypes(resource).isEmpty())
			throw new BadRequest("Cannot change resource type");
		if (analyzer.isDisconnectedNodePresent())
			throw new BadRequest("Blank nodes must be connected");
		return input;
	}

	private TriplePattern changeNoteOf(URI resource) {
		AbsoluteTermFactory tf = AbsoluteTermFactory.newInstance();
		IRI subj = tf.iri(resource.stringValue());
		return new TriplePattern(subj, tf.iri(CHANGE_NOTE), tf.node());
	}
}
