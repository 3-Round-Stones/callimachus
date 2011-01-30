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

package org.callimachusproject.stream;

import static org.callimachusproject.stream.SPARQLWriter.toSPARQL;
import static org.openrdf.query.QueryLanguage.SPARQL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.callimachusproject.rdfa.RDFEventReader;
import org.callimachusproject.rdfa.RDFParseException;
import org.callimachusproject.rdfa.events.Document;
import org.callimachusproject.rdfa.events.RDFEvent;
import org.callimachusproject.rdfa.events.Triple;
import org.callimachusproject.rdfa.events.TriplePattern;
import org.callimachusproject.rdfa.model.IRI;
import org.callimachusproject.rdfa.model.Term;
import org.callimachusproject.rdfa.model.TermFactory;
import org.callimachusproject.rdfa.model.VarOrIRI;
import org.callimachusproject.rdfa.model.VarOrTerm;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 * Evaluates queries from the RDF store and recursively reads rdf:Lists
 * returning a single result document.
 * 
 * @author James Leigh
 * 
 */
public class RDFStoreReader extends RDFEventReader {
	private static final String RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private TermFactory tf = TermFactory.newInstance();
	private RepositoryConnection con;
	private TriplePatternStore patterns;
	private Iterator<? extends RDFEventReader> results;
	private RDFEventReader result;
	private Map<VarOrIRI, VarOrTerm> rdfLists = new HashMap<VarOrIRI, VarOrTerm>();
	private Set<Triple> listQueue = new LinkedHashSet<Triple>();
	private boolean started;
	private boolean ended;

	public RDFStoreReader(String sparql, TriplePatternStore store, RepositoryConnection con)
			throws RepositoryException, MalformedQueryException,
			QueryEvaluationException, RDFParseException, IOException {
		this.patterns = store;
		this.con = con;
		IRI rdfRest = tf.iri(RDF + "rest");
		for (TriplePattern rest : patterns.getPatternsByPredicate(rdfRest)) {
			VarOrIRI pred = patterns.getProjectedPattern(rest).getPredicate();
			if (!rest.getSubject().isVar() || !pred.isIRI())
				continue;
			rdfLists.put(pred, rest.getSubject());
		}
		if (sparql != null) {
			GraphQuery q = con.prepareGraphQuery(SPARQL, sparql);
			results = Collections.singleton(new GraphResultReader(q.evaluate())).iterator();
		}
	
	}

	public RDFStoreReader(TriplePatternStore store,
			RepositoryConnection con, String uri) throws RepositoryException,
			MalformedQueryException, QueryEvaluationException,
			RDFParseException, IOException {
		this.patterns = store;
		this.con = con;
		IRI rdfRest = tf.iri(RDF + "rest");
		for (TriplePattern rest : patterns.getPatternsByPredicate(rdfRest)) {
			VarOrIRI pred = patterns.getProjectedPattern(rest).getPredicate();
			if (!rest.getSubject().isVar() || !pred.isIRI())
				continue;
			rdfLists.put(pred, rest.getSubject());
		}
		List<RDFEventReader> list = new ArrayList<RDFEventReader>();
		for (RDFEventReader query : store.openWellJoinedQueries()) {
			GraphQuery q = con.prepareGraphQuery(SPARQL, toSPARQL(query));
			if (uri != null) {
				ValueFactory vf = con.getValueFactory();
				URI self = vf.createURI(uri);
				q.setBinding("this", self);
				TriplePattern tp = patterns.getFirstTriplePattern();
				if (tp.isInverse()) {
					VarOrTerm obj = tp.getObject();
					if (obj.isVar()) {
						q.setBinding(obj.stringValue(), self);
					}
				} else {
					VarOrTerm subj = tp.getSubject();
					if (subj.isVar()) {
						q.setBinding(subj.stringValue(), self);
					}
				}
			}
			list.add(new ReducedTripleReader(new GraphResultReader(q.evaluate())));
		}
		results = list.iterator();
	}

	public String toString() {
		return patterns.toString();
	}

	@Override
	public void close() throws RDFParseException {
		if (result != null) {
			result.close();
		}
		while (results.hasNext()) {
			results.next().close();
		}
	}

	@Override
	protected RDFEvent take() throws RDFParseException {
		try {
			if (!started) {
				started = true;
				return new Document(true);
			}
			if (result == null && results.hasNext()) {
				result = results.next();
			} else if (result != null && !result.hasNext() && results.hasNext()) {
				result.close();
				result = results.next();
			}
			if (result != null && result.hasNext()) {
				RDFEvent next = result.next();
				if (next.isStartDocument())
					return take();
				if (next.isEndDocument())
					return take();
				if (next.isTriple()) {
					Triple triple = next.asTriple();
					if (rdfLists.containsKey(triple.getPredicate())) {
						listQueue.add(triple);
					}
				}
				return next;
			}
			if (result != null) {
				result.close();
				result = null;
			}
			if (listQueue.isEmpty()) {
				if (ended)
					return null;
				ended = true;
				return new Document(false);
			}
			Iterator<Triple> iter = listQueue.iterator();
			Triple rest = iter.next();
			iter.remove();
			VarOrTerm list = rdfLists.get(rest.getPredicate());
			String qry = toSPARQL(patterns.openQueryBySubject(list));
			GraphQuery query = con.prepareGraphQuery(SPARQL, qry);
			query.setBinding(list.stringValue(), toValue(rest.getObject()));
			result = new GraphResultReader(query.evaluate());
			return take();
		} catch (IOException e) {
			throw new RDFParseException(e);
		} catch (RepositoryException e) {
			throw new RDFParseException(e);
		} catch (MalformedQueryException e) {
			throw new RDFParseException(e);
		} catch (QueryEvaluationException e) {
			throw new RDFParseException(e);
		}
	}

	private Value toValue(Term obj) {
		ValueFactory vf = con.getValueFactory();
		if (obj.isIRI())
			return vf.createURI(obj.stringValue());
		if (obj.isPlainLiteral())
			return vf.createLiteral(obj.stringValue(), obj.asPlainLiteral()
					.getLang());
		if (obj.isTypedLiteral()) {
			URI dt = (URI) toValue(obj.asTypedLiteral().getDatatype());
			return vf.createLiteral(obj.stringValue(), dt);
		}
		return vf.createBNode(obj.stringValue());
	}

}
