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
package org.callimachusproject.form.helpers;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.callimachusproject.engine.RDFEventReader;
import org.callimachusproject.engine.RDFParseException;
import org.callimachusproject.engine.events.TriplePattern;
import org.callimachusproject.engine.helpers.SparqlUpdateFactory;
import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.Conflict;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.rio.RDFHandlerException;

public class EntityUpdater {
	private final TripleAnalyzer analyzer = new TripleAnalyzer();
	private final URI entity;
	private final boolean components;

	public EntityUpdater(URI entity) {
		this(entity, false);
	}

	public EntityUpdater(URI entity, boolean components) {
		this.entity = entity;
		this.components = components;
		if (!components) {
			analyzer.addSubject(entity);
		}
	}

	public void acceptDelete(RDFEventReader template) throws RDFParseException {
		analyzer.acceptDelete(template);
	}

	public void acceptDelete(GraphQueryResult result) throws RDFParseException,
			QueryEvaluationException {
		analyzer.acceptDelete(result);
	}

	public void acceptInsert(RDFEventReader template) throws RDFParseException {
		analyzer.acceptInsert(template);
	}

	public void acceptInsert(GraphQueryResult result) throws RDFParseException,
			QueryEvaluationException {
		analyzer.acceptInsert(result);
	}

	public void acceptInsert(TriplePattern pattern) {
		analyzer.acceptInsert(pattern);
	}

	public URI getSubject() {
		return analyzer.getSubject();
	}

	public Set<URI> getPartners() {
		return analyzer.getPartners();
	}

	public Set<URI> getTypes(URI subject) {
		return analyzer.getTypes(subject);
	}

	public void executeReplacement(GraphQueryResult deleteData,
			GraphQueryResult insertData, ObjectConnection con)
			throws IOException, OpenRDFException {
		SparqlUpdateFactory factory = new SparqlUpdateFactory();
		String sparql = factory.replacement(deleteData, insertData);
		analyzer.analyzeUpdate(sparql, entity.stringValue());
		verify();
		executeUpdate(sparql, con);
	}

	public void executeUpdate(InputStream in, ObjectConnection con)
			throws OpenRDFException, IOException {
		String sparqlUpdate = parseUpdate(in);
		executeUpdate(sparqlUpdate, con);
	}

	public String parseUpdate(InputStream in) throws OpenRDFException,
			IOException {
		String ret = analyzer.parseUpdate(in, entity.stringValue());
		verify();
		return ret;
	}

	public void analyzeUpdate(String input)
			throws MalformedQueryException, RDFHandlerException {
		analyzer.analyzeUpdate(input, entity.stringValue());
		verify();
	}

	public void executeUpdate(String sparqlUpdate, ObjectConnection con)
			throws BadRequest, OpenRDFException {
		Set<Resource> nodes = selectBlankNodes(analyzer, con);
		executeUpdate(sparqlUpdate, entity.toString(), con);
		ensureConnectionOrRemoved(nodes, con);
	}

	private void verify() throws BadRequest {
		if (!analyzer.isAbout(entity)) {
			URI target = analyzer.getSubject();
			if (components && analyzer.isInsertOnly()) {
				String uri = target.stringValue();
				String ns = entity.stringValue();
				if (uri.indexOf(ns) != 0)
					throw new BadRequest("Resource URI must start with: " + ns);
				String local = uri.substring(ns.length());
				if (local.indexOf('/') == 0 && ns.charAt(ns.length() - 1) != '/') {
					local = local.substring(1);
				}
				if (local.charAt(local.length() - 1) == '/') {
					local = local.substring(0, local.length() - 1);
				}
				if (local.indexOf('/') >= 0)
					throw new BadRequest("Can only created nested components here");
			} else {
				throw new BadRequest("Wrong Subject: " + analyzer.getSubject());
			}
		}
		if (!analyzer.isSingleton())
			throw new BadRequest("Only one entity can be modified per request");
		if (analyzer.isDisconnectedNodePresent())
			throw new BadRequest("Blank nodes must be connected");
		if (analyzer.isComplicated())
			throw new BadRequest("Only basic graph patterns are permitted");
		if (analyzer.isContainmentTriplePresent())
			throw new Conflict("ldp:contains is prohibited");
	}

	private Set<Resource> selectBlankNodes(TripleAnalyzer analyzer,
			ObjectConnection con) throws OpenRDFException {
		Set<Statement> connections = analyzer.getConnections();
		if (connections.isEmpty())
			return Collections.emptySet();
		String qry = createConnectionQuery(connections);
		Set<Resource> set = new HashSet<Resource>();
		TupleQuery query = con.prepareTupleQuery(QueryLanguage.SPARQL, qry);
		TupleQueryResult results = query.evaluate();
		try {
			while (results.hasNext()) {
				BindingSet bs = results.next();
				for (String name : bs.getBindingNames()) {
					Value value = bs.getValue(name);
					if (value instanceof Resource) {
						set.add((Resource) value);
					}
				}
			}
		} finally {
			results.close();
		}
		return set;
	}

	private String createConnectionQuery(Set<Statement> connections) {
		StringBuilder sb = new StringBuilder();
		for (Statement st : connections) {
			if (st.getSubject() instanceof URI) {
				if (sb.length() == 0) {
					sb.append("SELECT REDUCED * { {\n");
				} else {
					sb.append("UNION {\n");
				}
				appendPattern(st, connections, sb);
				sb.append("} ");
			}
		}
		assert sb.length() > 0;
		String qry = sb.append("}").toString();
		return qry;
	}

	private void appendPattern(Statement st, Set<Statement> connections,
			StringBuilder sb) {
		appendTerm(st.getSubject(), sb).append(' ');
		appendTerm(st.getPredicate(), sb).append(' ');
		appendTerm((Resource) st.getObject(), sb).append(" .\n");
		for (Statement next : connections) {
			if (st.getObject().equals(next.getSubject())) {
				sb.append("OPTIONAL {\n");
				appendPattern(next, connections, sb);
				sb.append("}\n");
			}
		}
	}

	private StringBuilder appendTerm(Resource term, StringBuilder sb) {
		if (term instanceof URI) {
			String uri = term.stringValue();
			if (uri.indexOf('>') >= 0)
				throw new IllegalArgumentException("Invalide URI: " + uri);
			sb.append('<').append(uri).append('>');
		} else {
			String id = term.stringValue();
			if (id.indexOf('>') >= 0 || id.indexOf(':') >= 0)
				throw new IllegalArgumentException("Invalide nodeID: " + id);
			sb.append("?var").append(id);
		}
		return sb;
	}

	private void executeUpdate(String input, String base, ObjectConnection con)
			throws OpenRDFException {
		con.prepareUpdate(QueryLanguage.SPARQL, input, base).execute();
	}

	private void ensureConnectionOrRemoved(Set<Resource> nodes,
			ObjectConnection con) throws OpenRDFException {
		for (Resource node : nodes) {
			if (con.hasStatement(node, null, null, false)) {
				if (!con.hasStatement(null, null, node, false))
					throw new BadRequest(
							"Incomplete or indistinguishable blank node");
			}
		}
	}
}
